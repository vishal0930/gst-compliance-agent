package com.gstcompliance.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class VendorCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String VENDOR_LIST_PREFIX = "vendor:list:";
    private static final Duration VENDOR_CACHE_TTL = Duration.ofHours(1);

    /**
     * Cache vendor list for a user
     */
    public void cacheVendorList(String email, List<String> vendors) {
        String key = VENDOR_LIST_PREFIX + email;
        try {
            redisTemplate.opsForValue().set(key, vendors, VENDOR_CACHE_TTL);
            log.debug("Cached vendor list for user: {}, count: {}", email, vendors.size());
        } catch (Exception e) {
            log.warn("Failed to cache vendor list for {}: {}", email, e.getMessage());
        }
    }

    /**
     * Get cached vendor list
     */
    public List<String> getCachedVendorList(String email) {
        String key = VENDOR_LIST_PREFIX + email;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("Cache hit for vendor list: {}", email);
                return objectMapper.convertValue(cached, new TypeReference<List<String>>() {});
            }
            log.debug("Cache miss for vendor list: {}", email);
        } catch (Exception e) {
            log.warn("Failed to retrieve cached vendor list for {}: {}", email, e.getMessage());
        }
        return null;
    }

    /**
     * Evict vendor cache for a user
     */
    public void evictUserCache(String email) {
        try {
            redisTemplate.delete(VENDOR_LIST_PREFIX + email);
            log.debug("Evicted vendor cache for user: {}", email);
        } catch (Exception e) {
            log.warn("Failed to evict vendor cache for {}: {}", email, e.getMessage());
        }
    }

    /**
     * Evict all vendor cache
     */
    public void evictAll() {
        try {
            redisTemplate.delete(redisTemplate.keys(VENDOR_LIST_PREFIX + "*"));
            log.info("Evicted all vendor cache entries");
        } catch (Exception e) {
            log.warn("Failed to evict all vendor cache: {}", e.getMessage());
        }
    }
}
