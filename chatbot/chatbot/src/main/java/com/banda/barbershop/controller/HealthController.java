package com.banda.barbershop.controller;

import com.banda.barbershop.service.ConversationStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ConversationStateService conversationStateService;

    /**
     * Simple health check for load balancers
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(health);
    }

    /**
     * Detailed health check with all service statuses
     */
    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("timestamp", LocalDateTime.now().toString());

        // Check MySQL
        Map<String, Object> mysqlHealth = checkMySQL();
        health.put("mysql", mysqlHealth);

        // Check Redis
        Map<String, Object> redisHealth = checkRedis();
        health.put("redis", redisHealth);

        // Conversation cache status
        health.put("conversationCache", conversationStateService.getCacheStatus());

        // Overall status - app works even if Redis is down (has fallback)
        boolean mysqlUp = "UP".equals(mysqlHealth.get("status"));
        boolean redisUp = "UP".equals(redisHealth.get("status"));

        String status;
        if (mysqlUp && redisUp) {
            status = "UP";
        } else if (mysqlUp) {
            status = "DEGRADED"; // Redis down but app still works
        } else {
            status = "DOWN"; // MySQL is required
        }
        health.put("status", status);

        return ResponseEntity.ok(health);
    }

    /**
     * Readiness check - is the app ready to receive traffic?
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readinessCheck() {
        Map<String, Object> readiness = new HashMap<>();

        boolean mysqlReady = "UP".equals(checkMySQL().get("status"));
        boolean redisReady = "UP".equals(checkRedis().get("status"));

        boolean ready = mysqlReady && redisReady;
        readiness.put("ready", ready);
        readiness.put("mysql", mysqlReady);
        readiness.put("redis", redisReady);

        if (ready) {
            return ResponseEntity.ok(readiness);
        } else {
            return ResponseEntity.status(503).body(readiness);
        }
    }

    /**
     * Liveness check - is the app alive?
     */
    @GetMapping("/live")
    public ResponseEntity<Map<String, String>> livenessCheck() {
        Map<String, String> liveness = new HashMap<>();
        liveness.put("status", "UP");
        return ResponseEntity.ok(liveness);
    }

    private Map<String, Object> checkMySQL() {
        Map<String, Object> status = new HashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(5)) {
                status.put("status", "UP");
                status.put("database", conn.getCatalog());
            } else {
                status.put("status", "DOWN");
                status.put("error", "Connection not valid");
            }
        } catch (Exception e) {
            log.error("MySQL health check failed: {}", e.getMessage());
            status.put("status", "DOWN");
            status.put("error", e.getMessage());
        }
        return status;
    }

    private Map<String, Object> checkRedis() {
        Map<String, Object> status = new HashMap<>();
        try {
            String pong = redisTemplate.getConnectionFactory()
                .getConnection()
                .ping();
            if ("PONG".equals(pong)) {
                status.put("status", "UP");
            } else {
                status.put("status", "DOWN");
                status.put("error", "Unexpected ping response: " + pong);
            }
        } catch (Exception e) {
            log.error("Redis health check failed: {}", e.getMessage());
            status.put("status", "DOWN");
            status.put("error", e.getMessage());
        }
        return status;
    }
}
