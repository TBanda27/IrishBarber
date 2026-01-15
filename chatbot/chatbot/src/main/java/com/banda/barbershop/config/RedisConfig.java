package com.banda.barbershop.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    private ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register JavaTimeModule for LocalDateTime, LocalDate, etc.
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Register Hibernate6Module to handle lazy-loaded proxies
        Hibernate6Module hibernateModule = new Hibernate6Module();
        // Force loading of lazy associations to include them in serialization
        hibernateModule.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, true);
        mapper.registerModule(hibernateModule);

        // Don't fail on empty beans (Hibernate proxies)
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // Enable default typing to preserve type information during serialization
        mapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        return mapper;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        GenericJackson2JsonRedisSerializer jsonSerializer =
            new GenericJackson2JsonRedisSerializer(redisObjectMapper());

        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use JSON serializer with Java 8 time support for values
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer jsonSerializer =
            new GenericJackson2JsonRedisSerializer(redisObjectMapper());

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(jsonSerializer))
            .disableCachingNullValues();

        // Cache-specific configurations
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // Bookings cache - 10 minutes TTL (bookings can change)
        cacheConfigs.put("customerBookings", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // Services cache - 1 hour TTL (rarely changes)
        cacheConfigs.put("services", defaultConfig.entryTtl(Duration.ofHours(1)));

        // Barbers cache - 1 hour TTL (rarely changes)
        cacheConfigs.put("barbers", defaultConfig.entryTtl(Duration.ofHours(1)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig.entryTtl(Duration.ofMinutes(30)))
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }
}
