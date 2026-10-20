package com.yash.url_shortner.service;

public class UrlExpiredException extends RuntimeException {
    public UrlExpiredException(String shortCode) {
        super("Short URL has expired: " + shortCode);
    }
}
