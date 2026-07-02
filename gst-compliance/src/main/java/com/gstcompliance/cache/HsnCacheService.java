package com.gstcompliance.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class HsnCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String HSN_CACHE_PREFIX = "hsn:class:";
    private static final Duration HSN_CACHE_TTL = Duration.ofDays(30);

    /**
     * Cache HSN classification result
     */
    public void cacheClassification(String description, Map<String, Object> classificationResult) {
        String key = HSN_CACHE_PREFIX + normalizeKey(description);
        try {
            redisTemplate.opsForValue().set(key, classificationResult, HSN_CACHE_TTL);
            log.debug("Cached HSN classification for: {}", description);
        } catch (Exception e) {
            log.warn("Failed to cache HSN classification for {}: {}", description, e.getMessage());
        }
    }

    /**
     * Get cached HSN classification.
     *
     * GenericJackson2JsonRedisSerializer deserialises JSON numbers as Integer/Double,
     * not BigDecimal. Every numeric field is normalised here before returning so callers
     * can safely cast to BigDecimal without ClassCastException.
     */
    public Map<String, Object> getCachedClassification(String description) {
        String key = HSN_CACHE_PREFIX + normalizeKey(description);
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("Cache hit for HSN classification: {}", description);
                Map<String, Object> raw =
                        objectMapper.convertValue(cached, new TypeReference<Map<String, Object>>() {});
                return normaliseBigDecimals(raw);
            }
            log.debug("Cache miss for HSN classification: {}", description);
        } catch (Exception e) {
            log.warn("Failed to retrieve cached HSN classification for {}: {}", description, e.getMessage());
        }
        return null;
    }

    /**
     * Convert every Number value in the map to BigDecimal so callers can cast safely.
     * GenericJackson2JsonRedisSerializer returns Doubles/Integers for JSON numbers.
     */
    private Map<String, Object> normaliseBigDecimals(Map<String, Object> map) {
        if (map == null) return null;
        map.replaceAll((k, v) -> {
            if (v instanceof Number && !(v instanceof java.math.BigDecimal)) {
                return new java.math.BigDecimal(v.toString());
            }
            return v;
        });
        return map;
    }

    /**
     * Check if classification is cached
     */
    public boolean isCached(String description) {
        String key = HSN_CACHE_PREFIX + normalizeKey(description);
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("Failed to check cache for {}: {}", description, e.getMessage());
            return false;
        }
    }

    /**
     * Clear HSN cache for a specific description
     */
    public void evict(String description) {
        String key = HSN_CACHE_PREFIX + normalizeKey(description);
        try {
            redisTemplate.delete(key);
            log.debug("Evicted HSN cache for: {}", description);
        } catch (Exception e) {
            log.warn("Failed to evict HSN cache for {}: {}", description, e.getMessage());
        }
    }

    /**
     * Clear all HSN cache
     */
    public void evictAll() {
        try {
            redisTemplate.delete(redisTemplate.keys(HSN_CACHE_PREFIX + "*"));
            log.info("Evicted all HSN cache entries");
        } catch (Exception e) {
            log.warn("Failed to evict all HSN cache: {}", e.getMessage());
        }
    }

    /**
     * Normalize key for consistent caching
     */
    private String normalizeKey(String description) {
        return description.toLowerCase()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-zA-Z0-9_]", "");
    }
}
