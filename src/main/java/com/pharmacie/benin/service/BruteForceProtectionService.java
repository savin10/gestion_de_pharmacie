package com.pharmacie.benin.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pharmacie.benin.model.BruteForceAttempt;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Service for protecting against brute force login attacks.
 * 
 * This service tracks failed login attempts by IP address and temporarily blocks
 * IPs that exceed the threshold. The protection mechanism:
 * - Tracks failed login attempts per IP address
 * - Blocks an IP after 3 failed attempts within 5 minutes
 * - Maintains the block for 15 minutes
 * - Automatically expires tracking data after 15 minutes of inactivity
 * 
 * Implementation uses Caffeine cache for in-memory storage with automatic expiration.
 * 
 * Validates Requirements: 4.5
 */
@Service
public class BruteForceProtectionService {
    
    private static final int MAX_ATTEMPTS = 3;
    private static final int ATTEMPT_WINDOW_MINUTES = 5;
    private static final int BLOCK_DURATION_MINUTES = 15;
    private static final int CACHE_EXPIRATION_MINUTES = 15;
    
    /**
     * In-memory cache storing BruteForceAttempt objects by IP address.
     * Cache entries automatically expire after 15 minutes of inactivity.
     */
    private final Cache<String, BruteForceAttempt> attemptCache;
    
    /**
     * Constructs the BruteForceProtectionService with a Caffeine cache.
     * Cache is configured to expire entries 15 minutes after last write.
     */
    public BruteForceProtectionService() {
        this.attemptCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(CACHE_EXPIRATION_MINUTES))
                .maximumSize(10000) // Limit cache size to prevent memory issues
                .build();
    }
    
    /**
     * Checks if the given IP address is currently blocked from making login attempts.
     * 
     * An IP is considered blocked if:
     * 1. It has 3 or more failed attempts within the last 5 minutes
     * 2. The block duration (15 minutes) has not yet expired
     * 
     * @param ipAddress the IP address to check
     * @return true if the IP is blocked, false otherwise
     */
    public boolean isBlocked(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return false;
        }
        
        BruteForceAttempt attempt = attemptCache.getIfPresent(ipAddress);
        
        // No record means not blocked
        if (attempt == null) {
            return false;
        }
        
        // Check if the attempt tracking has expired
        if (attempt.isExpired()) {
            attemptCache.invalidate(ipAddress);
            return false;
        }
        
        // Check if currently blocked
        if (attempt.isBlocked()) {
            return true;
        }
        
        // Check if attempts are within the 5-minute window
        if (attempt.getFirstAttemptAt() != null) {
            LocalDateTime windowStart = attempt.getFirstAttemptAt();
            LocalDateTime windowEnd = windowStart.plusMinutes(ATTEMPT_WINDOW_MINUTES);
            LocalDateTime now = LocalDateTime.now();
            
            // If we're outside the window, the attempts don't count anymore
            if (now.isAfter(windowEnd)) {
                attemptCache.invalidate(ipAddress);
                return false;
            }
            
            // Within window, check if threshold exceeded
            return attempt.getAttemptCount() >= MAX_ATTEMPTS;
        }
        
        return false;
    }
    
    /**
     * Registers a failed login attempt for the given IP address.
     * 
     * This method:
     * 1. Retrieves or creates a BruteForceAttempt record for the IP
     * 2. Increments the attempt count
     * 3. Updates timestamps
     * 4. Sets block expiration if threshold is reached (3 attempts)
     * 
     * If the IP already has expired attempts, they are reset before registering
     * the new attempt.
     * 
     * @param ipAddress the IP address that made the failed login attempt
     */
    public void registerFailedAttempt(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return;
        }
        
        BruteForceAttempt attempt = attemptCache.getIfPresent(ipAddress);
        
        // Create new attempt record if none exists or if expired
        if (attempt == null || attempt.isExpired()) {
            attempt = new BruteForceAttempt();
            attempt.setIpAddress(ipAddress);
        }
        
        // Check if we need to reset due to window expiration
        if (attempt.getFirstAttemptAt() != null) {
            LocalDateTime windowEnd = attempt.getFirstAttemptAt().plusMinutes(ATTEMPT_WINDOW_MINUTES);
            if (LocalDateTime.now().isAfter(windowEnd)) {
                // Outside the 5-minute window, reset and start fresh
                attempt.reset();
            }
        }
        
        // Register the failed attempt
        attempt.registerFailedAttempt();
        
        // Store back in cache
        attemptCache.put(ipAddress, attempt);
    }
    
    /**
     * Resets all failed login attempts for the given IP address.
     * 
     * This method should be called after a successful login to clear any
     * previous failed attempts and unblock the IP if it was blocked.
     * 
     * @param ipAddress the IP address to reset
     */
    public void resetAttempts(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return;
        }
        
        attemptCache.invalidate(ipAddress);
    }
    
    /**
     * Gets the current attempt record for an IP address (for testing/monitoring).
     * 
     * @param ipAddress the IP address to query
     * @return the BruteForceAttempt record, or null if none exists
     */
    BruteForceAttempt getAttempt(String ipAddress) {
        return attemptCache.getIfPresent(ipAddress);
    }
    
    /**
     * Clears all attempt records from the cache (for testing).
     */
    void clearAllAttempts() {
        attemptCache.invalidateAll();
    }
}
