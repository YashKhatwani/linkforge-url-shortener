
package com.yash.url_shortner.service;

import com.yash.url_shortner.repository.UrlClickAnalyticsRepository;
import org.springframework.stereotype.Service;

@Service
public class UrlAnalyticsService {

    private final UrlClickAnalyticsRepository analyticsRepository;

    public UrlAnalyticsService(
            UrlClickAnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    public UrlClickAnalyticsRepository.ClickAnalytics
    getAnalytics(String shortCode) {

        return analyticsRepository.findAnalyticsByShortCode(shortCode);
    }

    public java.util.Map<String, Long> getClickCounts(
            java.util.List<String> shortCodes) {
        return analyticsRepository.findClickCounts(shortCodes);
    }
}
