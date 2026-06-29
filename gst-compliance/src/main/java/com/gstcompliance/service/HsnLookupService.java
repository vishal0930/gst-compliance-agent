package com.gstcompliance.service;

import com.gstcompliance.model.HsnCode;
import com.gstcompliance.repository.HsnCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.datatype.jsr310.DecimalUtils.toBigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class HsnLookupService {

    private final HsnCodeRepository hsnCodeRepository;
    private final EmbeddingService embeddingService;
    private final JdbcTemplate jdbcTemplate;

    // Minimum similarity threshold configuration (e.g., 0.60 means 60% semantic match)
    private static final double SIMILARITY_THRESHOLD = 0.60;

    @Cacheable(value = "hsnLookup", key = "#description", unless = "#result.isEmpty()")
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
                        1 - (embedding <-> CAST(? AS vector)) as similarity
                FROM hsn_codes
                    WHERE embedding IS NOT NULL
                            ORDER BY embedding <-> CAST(? AS vector)
                            LIMIT ?
                """;


            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                    sql,
                    vectorString,
                    vectorString,
                    limit
            );
            for (Map<String, Object> row : results) {
                log.info(
                        "Similarity={} HSN={} GST={} Desc={}",
                        row.get("similarity"),
                        row.get("hsn_code"),
                        row.get("gst_rate"),
                        row.get("description")
                );
            }
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
            log.error("❌ Semantic vector execution failure: {}", e.getMessage(), e);
            return List.of();
        }
    }

    // ✅ DATABASE RAG TEXT FALLBACK
    private List<HsnCode> performTextSearch(String description, int limit) {
        log.info("📝 Executing text-token matching for: {}", description);

        String sql = """
            SELECT hsn_code, description, gst_rate, chapter, heading, sub_heading
            FROM hsn_codes
            WHERE description ILIKE ? 
               OR hsn_code ILIKE ?
            ORDER BY LENGTH(description) ASC
            LIMIT ?
            """;

        String searchToken = "%" + description.trim().toLowerCase() + "%";

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, searchToken, searchToken, limit);
            List<HsnCode> codes = new ArrayList<>();
            for (Map<String, Object> row : results) {
                codes.add(mapRowToHsnCode(row));
            }
            log.info("✅ Text matching found {} candidate HSN codes from dataset.", codes.size());
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

        code.setGstRate(toBigDecimal(row.get("gst_rate")));
        code.setCgstRate(toBigDecimal(row.get("cgst_rate")));
        code.setSgstRate(toBigDecimal(row.get("sgst_rate")));
        code.setIgstRate(toBigDecimal(row.get("igst_rate")));
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