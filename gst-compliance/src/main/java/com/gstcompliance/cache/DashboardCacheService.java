package com.gstcompliance.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String DASHBOARD_STATS_PREFIX = "dashboard:stats:";
    private static final Duration DASHBOARD_STATS_TTL = Duration.ofMinutes(5);
    private static final String SUMMARY_CARDS_PREFIX = "dashboard:summary:";
    private static final Duration SUMMARY_CARDS_TTL = Duration.ofMinutes(10);

    /**
     * Cache dashboard statistics for a user
     */
    public void cacheDashboardStats(String email, Map<String, Object> stats) {
        String key = DASHBOARD_STATS_PREFIX + email;
        try {
            redisTemplate.opsForValue().set(key, stats, DASHBOARD_STATS_TTL);
            log.debug("Cached dashboard stats for user: {}", email);
        } catch (Exception e) {
            log.warn("Failed to cache dashboard stats for {}: {}", email, e.getMessage());
        }
    }

    /**
     * Get cached dashboard statistics
     */
    public Map<String, Object> getCachedDashboardStats(String email) {
        String key = DASHBOARD_STATS_PREFIX + email;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("Cache hit for dashboard stats: {}", email);
                return objectMapper.convertValue(cached, new TypeReference<Map<String, Object>>() {});
            }
            log.debug("Cache miss for dashboard stats: {}", email);
        } catch (Exception e) {
            log.warn("Failed to retrieve cached dashboard stats for {}: {}", email, e.getMessage());
        }
        return null;
    }

    /**
     * Cache summary cards (Invoices, GSTR2B, ITC, Compliance Score)
     */
    public void cacheSummaryCards(String email, Map<String, Object> summary) {
        String key = SUMMARY_CARDS_PREFIX + email;
        try {
            redisTemplate.opsForValue().set(key, summary, SUMMARY_CARDS_TTL);
            log.debug("Cached summary cards for user: {}", email);
        } catch (Exception e) {
            log.warn("Failed to cache summary cards for {}: {}", email, e.getMessage());
        }
    }

    /**
     * Get cached summary cards
     */
    public Map<String, Object> getCachedSummaryCards(String email) {
        String key = SUMMARY_CARDS_PREFIX + email;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("Cache hit for summary cards: {}", email);
                return objectMapper.convertValue(cached, new TypeReference<Map<String, Object>>() {});
            }
            log.debug("Cache miss for summary cards: {}", email);
        } catch (Exception e) {
            log.warn("Failed to retrieve cached summary cards for {}: {}", email, e.getMessage());
        }
        return null;
    }

    /**
     * Evict dashboard cache for a user
     */
    public void evictUserCache(String email) {
        try {
            redisTemplate.delete(DASHBOARD_STATS_PREFIX + email);
            redisTemplate.delete(SUMMARY_CARDS_PREFIX + email);
            log.debug("Evicted dashboard cache for user: {}", email);
        } catch (Exception e) {
            log.warn("Failed to evict dashboard cache for {}: {}", email, e.getMessage());
        }
    }

    /**
     * Evict all dashboard cache
     */
    public void evictAll() {
        try {
            redisTemplate.delete(redisTemplate.keys(DASHBOARD_STATS_PREFIX + "*"));
            redisTemplate.delete(redisTemplate.keys(SUMMARY_CARDS_PREFIX + "*"));
            log.info("Evicted all dashboard cache entries");
        } catch (Exception e) {
            log.warn("Failed to evict all dashboard cache: {}", e.getMessage());
        }
    }
}
