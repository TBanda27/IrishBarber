package com.banda.barbershop.service;

import com.banda.barbershop.entity.ConversationState;
import com.banda.barbershop.enums.ConversationStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ConversationStateService {

    private final RedisTemplate<String, Object> redisTemplate;

    // In-memory fallback cache when Redis is unavailable
    private final Map<String, ConversationState> fallbackCache = new ConcurrentHashMap<>();
    private volatile boolean redisAvailable = true;
    private volatile long lastRedisCheck = 0;
    private static final long REDIS_CHECK_INTERVAL_MS = 30_000; // Re-check Redis every 30 seconds

    private static final String CONVERSATION_KEY_PREFIX = "conversation:";
    private static final long CONVERSATION_TTL_HOURS = 24;

    public ConversationStateService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Get existing conversation state or create a new one
     */
    public ConversationState getOrCreate(String phoneNumber) {
        String key = getKey(phoneNumber);

        // Try Redis first if available
        if (isRedisAvailable()) {
            try {
                ConversationState state = (ConversationState) redisTemplate.opsForValue().get(key);
                if (state == null) {
                    state = createNewConversation(phoneNumber);
                    log.debug("Created new conversation state for {}", phoneNumber);
                } else {
                    redisTemplate.expire(key, CONVERSATION_TTL_HOURS, TimeUnit.HOURS);
                    log.debug("Retrieved conversation state from Redis for {}: Step={}",
                            phoneNumber, state.getCurrentStep());
                }
                return state;
            } catch (RedisConnectionFailureException e) {
                handleRedisFailure(e);
            } catch (Exception e) {
                log.warn("Redis error, falling back to memory: {}", e.getMessage());
                markRedisUnavailable();
            }
        }

        // Fallback to in-memory cache
        return getOrCreateFromMemory(phoneNumber);
    }

    /**
     * Update the current conversation step
     */
    public void updateStep(String phoneNumber, ConversationStep step) {
        ConversationState state = getOrCreate(phoneNumber);
        state.setCurrentStep(step);
        state.touch();
        saveState(phoneNumber, state);
        log.debug("Updated step for {}: {}", phoneNumber, step);
    }

    /**
     * Update the context data
     */
    public void updateContext(String phoneNumber, String contextData) {
        ConversationState state = getOrCreate(phoneNumber);
        state.setContextData(contextData);
        state.touch();
        saveState(phoneNumber, state);
        log.debug("Updated context for {}", phoneNumber);
    }

    /**
     * Update both step and context atomically
     */
    public void updateStepAndContext(String phoneNumber, ConversationStep step, String contextData) {
        ConversationState state = getOrCreate(phoneNumber);
        state.setCurrentStep(step);
        state.setContextData(contextData);
        state.touch();
        saveState(phoneNumber, state);
        log.debug("Updated step and context for {}: Step={}", phoneNumber, step);
    }

    /**
     * Clear the context data while keeping current step
     */
    public void clearContext(String phoneNumber) {
        ConversationState state = getOrCreate(phoneNumber);
        state.setContextData(null);
        state.touch();
        saveState(phoneNumber, state);
        log.debug("Cleared context for {}", phoneNumber);
    }

    /**
     * Reset conversation to main menu
     */
    public void resetToMainMenu(String phoneNumber) {
        ConversationState state = getOrCreate(phoneNumber);
        state.setCurrentStep(ConversationStep.MAIN_MENU);
        state.setContextData(null);
        state.touch();
        saveState(phoneNumber, state);
        log.debug("Reset conversation to main menu for {}", phoneNumber);
    }

    /**
     * Delete conversation state
     */
    public void deleteConversation(String phoneNumber) {
        String key = getKey(phoneNumber);
        fallbackCache.remove(key);

        if (isRedisAvailable()) {
            try {
                redisTemplate.delete(key);
            } catch (Exception e) {
                log.warn("Failed to delete from Redis: {}", e.getMessage());
            }
        }
        log.debug("Deleted conversation state for {}", phoneNumber);
    }

    /**
     * Check if Redis is currently available
     */
    public boolean isRedisAvailable() {
        if (!redisAvailable) {
            // Periodically re-check if Redis has recovered
            long now = System.currentTimeMillis();
            if (now - lastRedisCheck > REDIS_CHECK_INTERVAL_MS) {
                lastRedisCheck = now;
                try {
                    String pong = redisTemplate.getConnectionFactory()
                        .getConnection()
                        .ping();
                    if ("PONG".equals(pong)) {
                        redisAvailable = true;
                        log.info("Redis connection restored");
                        // Migrate in-memory cache to Redis
                        migrateToRedis();
                    }
                } catch (Exception e) {
                    log.debug("Redis still unavailable: {}", e.getMessage());
                }
            }
        }
        return redisAvailable;
    }

    // ---- Private helper methods ----

    private ConversationState createNewConversation(String phoneNumber) {
        ConversationState newState = ConversationState.builder()
            .phoneNumber(phoneNumber)
            .currentStep(ConversationStep.MAIN_MENU)
            .contextData("show_initial")
            .lastActivity(LocalDateTime.now())
            .build();

        saveState(phoneNumber, newState);
        return newState;
    }

    private void saveState(String phoneNumber, ConversationState state) {
        String key = getKey(phoneNumber);

        // Always save to in-memory as backup
        fallbackCache.put(key, state);

        // Try to save to Redis
        if (isRedisAvailable()) {
            try {
                redisTemplate.opsForValue().set(key, state, CONVERSATION_TTL_HOURS, TimeUnit.HOURS);
            } catch (RedisConnectionFailureException e) {
                handleRedisFailure(e);
            } catch (Exception e) {
                log.warn("Failed to save to Redis: {}", e.getMessage());
                markRedisUnavailable();
            }
        }
    }

    private ConversationState getOrCreateFromMemory(String phoneNumber) {
        String key = getKey(phoneNumber);
        return fallbackCache.computeIfAbsent(key, k -> {
            log.debug("Created new conversation in memory for {}", phoneNumber);
            return ConversationState.builder()
                .phoneNumber(phoneNumber)
                .currentStep(ConversationStep.MAIN_MENU)
                .contextData("show_initial")
                .lastActivity(LocalDateTime.now())
                .build();
        });
    }

    private void handleRedisFailure(RedisConnectionFailureException e) {
        log.error("Redis connection failed: {}. Falling back to in-memory cache.", e.getMessage());
        markRedisUnavailable();
    }

    private void markRedisUnavailable() {
        if (redisAvailable) {
            redisAvailable = false;
            lastRedisCheck = System.currentTimeMillis();
            log.warn("Redis marked as unavailable. Using in-memory fallback.");
        }
    }

    private void migrateToRedis() {
        if (fallbackCache.isEmpty()) {
            return;
        }

        log.info("Migrating {} conversation states from memory to Redis", fallbackCache.size());
        for (Map.Entry<String, ConversationState> entry : fallbackCache.entrySet()) {
            try {
                redisTemplate.opsForValue().set(
                    entry.getKey(),
                    entry.getValue(),
                    CONVERSATION_TTL_HOURS,
                    TimeUnit.HOURS
                );
            } catch (Exception e) {
                log.warn("Failed to migrate conversation {} to Redis", entry.getKey());
            }
        }
        // Clear memory cache after successful migration
        fallbackCache.clear();
        log.info("Migration to Redis completed");
    }

    private String getKey(String phoneNumber) {
        return CONVERSATION_KEY_PREFIX + phoneNumber;
    }

    /**
     * Get current cache status (for health checks)
     */
    public Map<String, Object> getCacheStatus() {
        return Map.of(
            "redisAvailable", redisAvailable,
            "inMemoryCount", fallbackCache.size(),
            "mode", redisAvailable ? "REDIS" : "IN_MEMORY"
        );
    }
}
