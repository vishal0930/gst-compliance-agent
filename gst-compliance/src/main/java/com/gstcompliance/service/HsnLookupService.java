package com.gstcompliance.service;

import com.gstcompliance.model.HsnCode;
import com.gstcompliance.repository.HsnCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class HsnLookupService {

    private final HsnCodeRepository hsnCodeRepository;
    private final EmbeddingService embeddingService;
    private final JdbcTemplate jdbcTemplate;

    // Minimum similarity threshold configuration (e.g., 0.60 means 60% semantic match)
    private static final double SIMILARITY_THRESHOLD = 0.60;

    // @Cacheable removed — GenericJackson2JsonRedisSerializer deserialises BigDecimal
    // fields as Double, causing null rates when the result is read back from cache.
    // The Redis cache for individual item classifications is handled in HsnCacheService
    // with explicit BigDecimal normalisation, which is the correct layer for caching.
    public List<HsnCode> findTopCandidates(String description, int limit) {
        log.info("🔍 Initializing RAG candidate retrieval for: {}", description);

        // 1. Attempt vector search over the embedded database
        List<HsnCode> candidates = performVectorSearch(description, limit);

        // 2. If vector search yields nothing, fallback to an indexed full text query
        if (candidates.isEmpty()) {
            log.warn("⚠️ Semantic search yielded no quality context. Falling back to database text query.");
            candidates = performTextSearch(description, limit);
        }

        return candidates;
    }

    // ✅ REAL SEMANTIC SEARCH USING pgvector
    private List<HsnCode> performVectorSearch(String description, int limit) {
        try {
            float[] embedding = embeddingService.generateEmbedding(description);
            log.info("Embedding dimension: {}", embedding.length);
            log.info("Searching HSN for: {}", description);

            if (embedding.length == 0) {
                log.warn("⚠️ Empty embedding vector generated, skipping semantic pass.");
                return List.of();
            }

            String vectorString = embeddingService.embeddingToString(embedding);

            // Using cosine distance (1 - distance = similarity) matching against the threshold
            String sql = """
SELECT
    hsn_code,
    description,
    gst_rate,
    cgst_rate,
    sgst_rate,
    igst_rate,
    cess_rate,
    chapter,
    heading,
    sub_heading,
    1 - (embedding <=> '%s'::vector) AS similarity
FROM hsn_codes
WHERE embedding IS NOT NULL
ORDER BY embedding <=> '%s'::vector
LIMIT %d
""".formatted(vectorString, vectorString, limit);


            Integer total = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM hsn_codes",
                    Integer.class);

            Integer embedded = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM hsn_codes WHERE embedding IS NOT NULL",
                    Integer.class);

            log.info("========================================");
            log.info("TOTAL HSN RECORDS       : {}", total);
            log.info("HSN WITH EMBEDDINGS     : {}", embedded);
            log.info("VECTOR STRING LENGTH    : {}", vectorString.length());
            log.info("VECTOR PREFIX           : {}",
                    vectorString.substring(0, Math.min(150, vectorString.length())));
            log.info("========================================");

            List<Map<String, Object>> results =
                    jdbcTemplate.queryForList(sql);
            log.info("================ TOP VECTOR RESULTS ================");

            for (Map<String, Object> row : results) {

                log.info(
                        "Distance={} | HSN={} | GST={} | DESC={}",
                        row.get("similarity"),
                        row.get("hsn_code"),
                        row.get("gst_rate"),
                        row.get("description")
                );

            }

            log.info("===================================================");
            List<HsnCode> codes = new ArrayList<>();
            for (Map<String, Object> row : results) {
                codes.add(mapRowToHsnCode(row));
            }

            log.info("✅ Found {} HSN codes via semantic vector search", codes.size());

            for (HsnCode code : codes) {
                log.info(
                        "Candidate -> HSN: {}, GST: {}, Description: {}",
                        code.getHsnCode(),
                        code.getGstRate(),
                        code.getDescription()
                );
            }

            return codes;

        } catch (Exception e) {
            log.error("========================================");
            log.error("VECTOR SEARCH FAILED");
            log.error("Message : {}", e.getMessage());
            log.error("SQL State: ", e);
            log.error("========================================");
            return List.of();
        }
    }

    // ✅ DATABASE RAG TEXT FALLBACK
    private List<HsnCode> performTextSearch(String description, int limit) {
        log.info("📝 Executing text-token matching for: {}", description);

        String cleaned = description
                .toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        String[] words = cleaned.split(" ");
        String keyword = cleaned;

        // Find the longest meaningful word (at least 5 chars)
        for (String word : words) {
            if (word.length() >= 5) {
                keyword = word;
                break;
            }
        }

        String searchToken = "%" + keyword + "%";

        // Try multiple search strategies
        List<Map<String, Object>> results = new ArrayList<>();
        
        // Strategy 1: Search by keyword (most specific)
        String sql1 = """
            SELECT hsn_code, description, gst_rate, cgst_rate, sgst_rate, igst_rate, cess_rate, chapter, heading, sub_heading
            FROM hsn_codes
            WHERE description ILIKE ? 
               OR hsn_code ILIKE ?
            ORDER BY LENGTH(description) ASC
            LIMIT ?
            """;
        try {
            results.addAll(jdbcTemplate.queryForList(sql1, searchToken, searchToken, limit));
        } catch (Exception e) {
            log.warn("Strategy 1 failed: {}", e.getMessage());
        }

        // Strategy 2: If no results, try with all words (broader search)
        if (results.isEmpty() && words.length > 1) {
            StringBuilder whereClause = new StringBuilder();
            List<String> params = new ArrayList<>();
            
            for (String word : words) {
                if (word.length() >= 3) {
                    if (whereClause.length() > 0) {
                        whereClause.append(" OR ");
                    }
                    whereClause.append("description ILIKE ?");
                    params.add("%" + word + "%");
                }
            }
            
            if (whereClause.length() > 0) {
                String sql2 = "SELECT hsn_code, description, gst_rate, cgst_rate, sgst_rate, igst_rate, cess_rate, chapter, heading, sub_heading FROM hsn_codes WHERE "
                        + whereClause.toString()
                        + " ORDER BY LENGTH(description) ASC LIMIT ?";
                params.add(String.valueOf(limit));
                
                try {
                    results.addAll(jdbcTemplate.queryForList(sql2, params.toArray()));
                } catch (Exception e) {
                    log.warn("Strategy 2 failed: {}", e.getMessage());
                }
            }
        }

        try {
            log.info("Text search returned {} rows", results.size());
            List<HsnCode> codes = new ArrayList<>();
            for (Map<String, Object> row : results) {
                codes.add(mapRowToHsnCode(row));
            }
            log.info("✅ Text matching found {} candidate HSN codes from dataset.", codes.size());
            
            if (codes.isEmpty()) {
                log.warn("⚠️ No text matches found for description: {} (tried keyword: {})", description, keyword);
            }
            
            return codes;
        } catch (Exception e) {
            log.error("❌ Text-based context retrieval failed: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Maps raw database records safely to the domain object model
     */
    private HsnCode mapRowToHsnCode(Map<String, Object> row) {

        HsnCode code = new HsnCode();

        code.setHsnCode((String) row.get("hsn_code"));
        code.setDescription((String) row.get("description"));

        BigDecimal gstRate = toBigDecimal(row.get("gst_rate"));
        BigDecimal cgstRate = toBigDecimal(row.get("cgst_rate"));
        BigDecimal sgstRate = toBigDecimal(row.get("sgst_rate"));
        BigDecimal igstRate = toBigDecimal(row.get("igst_rate"));

        if (igstRate == null) {
            igstRate = gstRate;
        }
        if ((cgstRate == null || sgstRate == null) && gstRate != null) {
            BigDecimal halfRate = gstRate.divide(BigDecimal.valueOf(2));
            if (cgstRate == null) {
                cgstRate = halfRate;
            }
            if (sgstRate == null) {
                sgstRate = halfRate;
            }
        }

        code.setGstRate(gstRate);
        code.setCgstRate(cgstRate);
        code.setSgstRate(sgstRate);
        code.setIgstRate(igstRate);
        code.setCessRate(toBigDecimal(row.get("cess_rate")));

        return code;
    }
    private BigDecimal toBigDecimal(Object value) {

        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal bd) {
            return bd;
        }

        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }

        return new BigDecimal(value.toString());
    }
    public void loadHsnEmbeddings() {
        log.info("🧠 Syncing HSN embedding counts...");
        long count = hsnCodeRepository.count();
        log.info("📊 Total production HSN codes registered in database: {}", count);
    }
}
