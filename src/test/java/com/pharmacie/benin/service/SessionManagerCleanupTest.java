package com.pharmacie.benin.service;

import com.pharmacie.benin.model.AdminUser;
import com.pharmacie.benin.model.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SessionManager scheduled cleanup functionality.
 * 
 * Tests the automatic cleanup of expired sessions that runs every hour.
 * Uses reflection to manually create expired sessions for testing purposes.
 * 
 * Validates Requirements: 5.1
 */
class SessionManagerCleanupTest {
    
    private SessionManager sessionManager;
    
    @BeforeEach
    void setUp() {
        sessionManager = new SessionManager();
    }
    
    @Test
    void shouldCleanExpiredSessions() throws Exception {
        // Given - Create some valid and expired sessions
        AdminUser user1 = createTestUser(1L, "admin1", "admin1@test.com");
        AdminUser user2 = createTestUser(2L, "admin2", "admin2@test.com");
        AdminUser user3 = createTestUser(3L, "admin3", "admin3@test.com");
        
        // Create valid sessions
        String validToken1 = sessionManager.createSession(user1);
        String validToken2 = sessionManager.createSession(user2);
        
        // Create an expired session manually
        String expiredToken = createExpiredSession(user3);
        
        // Verify initial state
        assertThat(sessionManager.getActiveSessionCount()).isEqualTo(3);
        assertThat(sessionManager.getSession(validToken1)).isPresent();
        assertThat(sessionManager.getSession(validToken2)).isPresent();
        
        // When
        sessionManager.cleanExpiredSessions();
        
        // Then
        assertThat(sessionManager.getActiveSessionCount()).isEqualTo(2);
        assertThat(sessionManager.getSession(validToken1)).isPresent();
        assertThat(sessionManager.getSession(validToken2)).isPresent();
        assertThat(sessionManager.getSession(expiredToken)).isEmpty();
    }
    
    @Test
    void shouldNotRemoveValidSessions() {
        // Given
        AdminUser user1 = createTestUser(1L, "admin1", "admin1@test.com");
        AdminUser user2 = createTestUser(2L, "admin2", "admin2@test.com");
        
        String token1 = sessionManager.createSession(user1);
        String token2 = sessionManager.createSession(user2);
        
        assertThat(sessionManager.getActiveSessionCount()).isEqualTo(2);
        
        // When
        sessionManager.cleanExpiredSessions();
        
        // Then - No sessions should be removed
        assertThat(sessionManager.getActiveSessionCount()).isEqualTo(2);
        assertThat(sessionManager.getSession(token1)).isPresent();
        assertThat(sessionManager.getSession(token2)).isPresent();
    }
    
    @Test
    void shouldHandleCleanupWithNoSessions() {
        // Given - No sessions
        assertThat(sessionManager.getActiveSessionCount()).isEqualTo(0);
        
        // When/Then - Should not throw exception
        sessionManager.cleanExpiredSessions();
        
        assertThat(sessionManager.getActiveSessionCount()).isEqualTo(0);
    }
    
    @Test
    void shouldHandleCleanupWithAllExpiredSessions() throws Exception {
        // Given - Create only expired sessions
        AdminUser user1 = createTestUser(1L, "admin1", "admin1@test.com");
        AdminUser user2 = createTestUser(2L, "admin2", "admin2@test.com");
        
        createExpiredSession(user1);
        createExpiredSession(user2);
        
        assertThat(sessionManager.getActiveSessionCount()).isEqualTo(2);
        
        // When
        sessionManager.cleanExpiredSessions();
        
        // Then - All sessions should be removed
        assertThat(sessionManager.getActiveSessionCount()).isEqualTo(0);
    }
    
    @Test
    void shouldRemoveExpiredSessionOnRetrieval() throws Exception {
        // Given - Create an expired session
        AdminUser user = createTestUser(1L, "admin", "admin@test.com");
        String expiredToken = createExpiredSession(user);
        
        assertThat(sessionManager.getActiveSessionCount()).isEqualTo(1);
        
        // When - Try to retrieve the expired session
        Optional<Session> retrievedSession = sessionManager.getSession(expiredToken);
        
        // Then - Session should be removed and return empty
        assertThat(retrievedSession).isEmpty();
        assertThat(sessionManager.getActiveSessionCount()).isEqualTo(0);
    }
    
    /**
     * Helper method to create a test AdminUser.
     */
    private AdminUser createTestUser(Long id, String username, String email) {
        AdminUser user = new AdminUser();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("$2a$12$hashedpassword");
        user.setCreatedAt(LocalDateTime.now());
        user.setActive(true);
        return user;
    }
    
    /**
     * Helper method to create an expired session for testing.
     * Uses reflection to access the private sessions map and insert an expired session.
     */
    private String createExpiredSession(AdminUser user) throws Exception {
        // Create a session that expired 1 hour ago
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAt = now.minusHours(9); // Created 9 hours ago
        LocalDateTime expiresAt = now.minusHours(1); // Expired 1 hour ago
        
        String token = java.util.UUID.randomUUID().toString();
        
        Session expiredSession = new Session(
            token,
            user.getId(),
            user.getUsername(),
            createdAt,
            createdAt,
            expiresAt
        );
        
        // Use reflection to access the private sessions map
        Field sessionsField = SessionManager.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, Session> sessions = 
            (ConcurrentHashMap<String, Session>) sessionsField.get(sessionManager);
        sessions.put(token, expiredSession);
        
        return token;
    }
}
