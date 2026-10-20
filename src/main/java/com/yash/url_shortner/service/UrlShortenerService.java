package com.yash.url_shortner.service;

import com.yash.url_shortner.model.UrlMapping;
import com.yash.url_shortner.repository.UrlMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.yash.url_shortner.event.UrlClickEvent;
import com.yash.url_shortner.producer.UrlClickProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.security.SecureRandom;





@Service
public class UrlShortenerService {

    private static final Logger log = LoggerFactory.getLogger(UrlShortenerService.class);

    private static final Duration DEFAULT_CACHE_TTL = Duration.ofMinutes(10);
    private static final String CACHE_KEY_PREFIX = "url:";

    private final UrlClickProducer urlClickProducer;

    private final StringRedisTemplate redisTemplate;

    private static final String CHARACTERS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final int SHORT_CODE_LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    private final UrlMappingRepository urlMappingRepository;

    public UrlShortenerService(
            UrlClickProducer urlClickProducer, StringRedisTemplate redisTemplate, UrlMappingRepository urlMappingRepository) {
        this.urlClickProducer = urlClickProducer;
        this.redisTemplate = redisTemplate;
        this.urlMappingRepository = urlMappingRepository;
    }

    @Transactional
    public UrlMapping createShortUrl(String originalUrl, Instant expiresAt) {
        return createShortUrl(originalUrl, expiresAt, null);
    }


    public Page<UrlMapping> getAllUrls(Pageable pageable) {
        return urlMappingRepository.findAll(pageable);
    }

    @Transactional
    public UrlMapping createShortUrl(String originalUrl, Instant expiresAt, String customAlias) {
        Instant now = Instant.now();

        if (expiresAt != null && !expiresAt.isAfter(now)) {
            throw new IllegalArgumentException(
                    "Expiration time must be in the future"
            );
        }
        if(expiresAt == null){
            expiresAt = now.plus(Duration.ofDays(7));
        }

        String shortCode = resolveShortCode(customAlias);

        UrlMapping mapping = new UrlMapping();
        mapping.setShortCode(shortCode);
        mapping.setOriginalUrl(originalUrl);
        mapping.setCreatedAt(now);
        mapping.setExpiresAt(expiresAt);



        return urlMappingRepository.save(mapping);
    }

    private String resolveShortCode(String customAlias) {
        if (customAlias == null || customAlias.isBlank()) {
            return generateUniqueShortCode();
        }

        String alias = customAlias.trim();
        if (alias.length() < 4 || alias.length() > 20) {
            throw new IllegalArgumentException("Custom alias must be between 4 and 20 characters.");
        }

        if (!alias.matches("^[A-Za-z0-9]+$")) {
            throw new IllegalArgumentException("Custom alias can contain only letters and numbers.");
        }

        if (urlMappingRepository.existsByShortCode(alias)) {
            throw new IllegalArgumentException(
                    "Alias already exists. Please choose a different custom alias."
            );
        }

        return alias;
    }

    private String generateUniqueShortCode() {
        String shortCode;
        do {
            shortCode = generateShortCode();
        } while (urlMappingRepository.existsByShortCode(shortCode));

        return shortCode;
    }

    private String generateShortCode() {
        StringBuilder code = new StringBuilder(SHORT_CODE_LENGTH);

        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            code.append(CHARACTERS.charAt(index));
        }

        return code.toString();
    }

    @Transactional(readOnly = true)
    public String getOriginalUrlByShortCode(String shortCode) {
        String cacheKey = CACHE_KEY_PREFIX + shortCode;
        String originalUrl = null;

        // 1. Try Redis first
        try {
            String cachedUrl = redisTemplate.opsForValue().get(cacheKey);

            if (cachedUrl != null) {
                originalUrl = cachedUrl;
            }
        } catch (DataAccessException ex) {
            // Redis is unavailable; continue with PostgreSQL.
        }

        // 2. Cache miss: fetch from PostgreSQL
        if (originalUrl == null) {
            UrlMapping mapping = urlMappingRepository.findByShortCode(shortCode)
                    .orElseThrow(() -> new UrlNotFoundException(shortCode));

            Instant now = Instant.now();

            // 3. Never redirect an expired URL
            if (mapping.getExpiresAt() != null
                    && !mapping.getExpiresAt().isAfter(now)) {
                throw new UrlExpiredException(shortCode);
            }

            originalUrl = mapping.getOriginalUrl();

            // 4. Calculate TTL, respecting URL expiration
            Duration ttl = DEFAULT_CACHE_TTL;

            if (mapping.getExpiresAt() != null) {
                Duration remaining = Duration.between(now, mapping.getExpiresAt());

                if (remaining.compareTo(ttl) < 0) {
                    ttl = remaining;
                }
            }

            // 5. Populate Redis; redirect still works if caching fails
            try {
                redisTemplate.opsForValue().set(
                        cacheKey,
                        originalUrl,
                        ttl
                );
            } catch (DataAccessException ex) {
                // Ignore cache-write failure; PostgreSQL has the mapping.
            }
        }
        UrlClickEvent event = new UrlClickEvent(
                java.util.UUID.randomUUID().toString(),
                shortCode,
                Instant.now()
        );

        urlClickProducer.publishClick(
                event
        );

        return originalUrl;
    }
}

