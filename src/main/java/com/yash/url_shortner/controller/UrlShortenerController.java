
package com.yash.url_shortner.controller;

import com.yash.url_shortner.model.UrlMapping;
import com.yash.url_shortner.repository.UrlClickAnalyticsRepository;
import com.yash.url_shortner.service.UrlShortenerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import com.yash.url_shortner.service.UrlAnalyticsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import java.time.Instant;

@RestController
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;
    private final UrlAnalyticsService urlAnalyticsService;

    public UrlShortenerController(
            UrlShortenerService urlShortenerService, UrlAnalyticsService urlAnalyticsService) {
        this.urlShortenerService = urlShortenerService;
        this.urlAnalyticsService = urlAnalyticsService;
    }

    @GetMapping("/api/hello")
    public String testHello(){
        return "Hello";
    }

    @GetMapping("/api/urls/{shortCode}/analytics")
    public ResponseEntity<UrlClickAnalyticsRepository.ClickAnalytics>
    getAnalytics(@PathVariable String shortCode) {

        var analytics = urlAnalyticsService.getAnalytics(shortCode);

        if (analytics == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(analytics);
    }

    @PostMapping("/api/urls")
    public ResponseEntity<CreateUrlResponse> createShortUrl(
            @Valid @RequestBody CreateUrlRequest request) {

        UrlMapping mapping =
                urlShortenerService.createShortUrl(
                        request.originalUrl(),
                        request.expiresAt(),
                        request.customAlias()
                );

        String shortUrl =
                "http://localhost:8080/" + mapping.getShortCode();

        CreateUrlResponse response = new CreateUrlResponse(
                mapping.getShortCode(),
                shortUrl,
                mapping.getOriginalUrl(),
                mapping.getExpiresAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{shortCode:[a-zA-Z0-9]+}")
    public RedirectView redirect(
            @PathVariable String shortCode) {

        String originalUrl =
                urlShortenerService.getOriginalUrlByShortCode(shortCode);


        RedirectView redirectView =
                new RedirectView(originalUrl);

        redirectView.setStatusCode(HttpStatus.FOUND);
        return redirectView;
    }

    public record CreateUrlRequest(
            @NotBlank(message = "Original URL is required")
            @Pattern(
                    regexp = "^https?://.+$",
                    message = "URL must start with http:// or https://"
            )
            String originalUrl,
            Instant expiresAt,
            @Pattern(
                    regexp = "^[A-Za-z0-9]+$",
                    message = "Custom alias can contain only letters and numbers"
            )
            String customAlias
    ) {
    }

    public record CreateUrlResponse(
            String shortCode,
            String shortUrl,
            String originalUrl,
            Instant expiresAt) {
    }



    @GetMapping("/api/urls")
    public ResponseEntity<UrlListResponse> getAllUrls(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page must be non-negative and size must be between 1 and 100"
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<UrlMapping> urlPage =
                urlShortenerService.getAllUrls(pageable);

        List<String> shortCodes = urlPage.getContent().stream()
                .map(UrlMapping::getShortCode)
                .toList();

        java.util.Map<String, Long> clickCounts =
                urlAnalyticsService.getClickCounts(shortCodes);

        List<UrlListItem> urls = urlPage.getContent().stream()
                .map(url -> new UrlListItem(
                        url.getShortCode(),
                        "http://localhost:8080/" + url.getShortCode(),
                        url.getOriginalUrl(),
                        url.getCreatedAt(),
                        url.getExpiresAt(),
                        clickCounts.getOrDefault(url.getShortCode(), 0L)
                ))
                .toList();

        UrlListResponse response = new UrlListResponse(
                urls,
                urlPage.getNumber(),
                urlPage.getSize(),
                urlPage.getTotalElements(),
                urlPage.getTotalPages()
        );

        return ResponseEntity.ok(response);
    }

    public record UrlListItem(
            String shortCode,
            String shortUrl,
            String originalUrl,
            Instant createdAt,
            Instant expiresAt,
            long totalClicks
    ) {}

    public record UrlListResponse(
            List<UrlListItem> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}


}
