package com.pharmacie.benin.service;

import com.pharmacie.benin.model.AdminUser;
import com.pharmacie.benin.model.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for SessionManager component.
 * 
 * Tests the core functionality of session management including:
 * - Session creation with UUID v4 tokens
 * - Session retrieval and validation
 * - Session invalidation
 * - Expired session cleanup
 * 
 * Validates Requirements: 4.6, 4.7, 5.1, 5.5
 */
class SessionManagerTest {
    
    private SessionManager sessionManager;
    
    @BeforeEach
    void setUp() {
        sessionManager = new SessionManager();
    }
    
    @Test
    void shouldCreateSessionWithValidUser() {
        // Given
        AdminUser user = createTestUser(1L, "admin", "admin@test.com");
        
        // When
        String token = sessionManager.createSession(user);
        
        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotBlank();
        // Verify UUID v4 format (8-4-4-4-12 hexadecimal characters)
        assertThat(token).matches("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
        assertThat(token.length()).isEqualTo(36); // UUID v4 string length
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingSessionWithNullUser() {
        // When/Then
        assertThatThrownBy(() -> sessionManager.createSession(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User cannot be null");
    }
    
    @Test
    void shouldThrowExceptionWhenCreatingSessionWithNullUserId() {
        // Given
        AdminUser user = createTestUser(null, "admin", "admin@test.com");
        
        // When/Then
        assertThatThrownBy(() -> sessionManager.createSession(user))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User ID cannot be null");
    }
    
    @Test
    void shouldRetrieveValidSession() {
        // Given
        AdminUser user = createTestUser(1L, "admin", "admin@test.com");
        String token = sessionManager.createSession(user);
        
        // When
        Optional<Session> retrievedSession = sessionManager.getSession(token);
        
        // Then
        assertThat(retrievedSession).isPresent();
        assertThat(retrievedSession.get().getToken()).isEqualTo(token);
        assertThat(retrievedSession.get().getUserId()).isEqualTo(1L);
        assertThat(retrievedSession.get().getUsername()).isEqualTo("admin");
        assertThat(retrievedSession.get().getCreatedAt()).isNotNull();
        assertThat(retrievedSession.get().getLastAccessedAt()).isNotNull();
        assertThat(retrievedSession.get().getExpiresAt()).isNotNull();
        
        // Verify expiration is set to 8 hours from creation
        LocalDateTime expectedExpiration = retrievedSession.get().getCreatedAt().plusHours(8);
        assertThat(retrievedSession.get().getExpiresAt()).isEqualToIgnoringSeconds(expectedExpiration);
    }
    
    @Test
    void shouldReturnEmptyForNonExistentSession() {
        // Given
        String nonExistentToken = UUID.randomUUID().toString();
        
        // When
        Optional<Session> retrievedSession = sessionManager.getSession(nonExistentToken);
        
        // Then
        assertThat(retrievedSession).isEmpty();
    }
    
    @Test
    void shouldReturnEmptyForNullToken() {
        // When
        Optional<Session> retrievedSession = sessionManager.getSession(null);
        
        // Then
        assertThat(retrievedSession).isEmpty();
    }
    
    @Test
    void shouldReturnEmptyForBlankToken() {
        // When
        Optional<Session> retrievedSession = sessionManager.getSession("   ");
        
        // Then
        assertThat(retrievedSession).isEmpty();
    }
    
    @Test
    void shouldInvalidateSession() {
        // Given
        AdminUser user = createTestUser(1L, "admin", "admin@test.com");
        String token = sessionManager.createSession(user);
        
        // Verify session exists
        assertThat(sessionManager.getSession(token)).isPresent();
        
        // When
        sessionManager.invalidateSession(token);
        
        // Then
        assertThat(sessionManager.getSession(token)).isEmpty();
    }
    
    @Test
    void shouldHandleInvalidatingNonExistentSession() {
        // Given
        String nonExistentToken = UUID.randomUUID().toString();
        
        // When/Then - should not throw exception
        sessionManager.invalidateSession(nonExistentToken);
    }
    
    @Test
    void shouldHandleInvalidatingNullToken() {
        // When/Then - should not throw exception
        sessionManager.invalidateSession(null);
    }
    
    @Test
    void shouldUpdateLastAccessedOnRetrieval() throws InterruptedException {
        // Given
        AdminUser user = createTestUser(1L, "admin", "admin@test.com");
        String token = sessionManager.createSession(user);
        
        Optional<Session> firstRetrieval = sessionManager.getSession(token);
        assertThat(firstRetrieval).isPresent();
        LocalDateTime firstAccessTime = firstRetrieval.get().getLastAccessedAt();
        
        // Wait a bit to ensure time difference
        Thread.sleep(100);
        
        // When
        Optional<Session> secondRetrieval = sessionManager.getSession(token);
        
        // Then
        assertThat(secondRetrieval).isPresent();
        LocalDateTime secondAccessTime = secondRetrieval.get().getLastAccessedAt();
        assertThat(secondAccessTime).isAfter(firstAccessTime);
    }
    
    @Test
    void shouldTrackActiveSessionCount() {
        // Given
        assertThat(sessionManager.getActiveSessionCount()).isEqualTo(0);
        
        AdminUser user1 = createTestUser(1L, "admin1", "admin1@test.com");
        AdminUser user2 = createTestUser(2L, "admin2", "admin2@test.com");
        
        // When
        String token1 = sessionManager.createSession(user1);
        assertThat(sessionManager.getActiveSessionCount()).isEqualTo(1);
        
        String token2 = sessionManager.createSession(user2);
        assertThat(sessionManager.getActiveSessionCount()).isEqualTo(2);
        
        sessionManager.invalidateSession(token1);
        assertThat(sessionManager.getActiveSessionCount()).isEqualTo(1);
        
        sessionManager.invalidateSession(token2);
        assertThat(sessionManager.getActiveSessionCount()).isEqualTo(0);
    }
    
    @Test
    void shouldClearAllSessions() {
        // Given
        AdminUser user1 = createTestUser(1L, "admin1", "admin1@test.com");
        AdminUser user2 = createTestUser(2L, "admin2", "admin2@test.com");
        
        sessionManager.createSession(user1);
        sessionManager.createSession(user2);
        
        assertThat(sessionManager.getActiveSessionCount()).isEqualTo(2);
        
        // When
        sessionManager.clearAllSessions();
        
        // Then
        assertThat(sessionManager.getActiveSessionCount()).isEqualTo(0);
    }
    
    @Test
    void shouldGenerateUniqueTokensForMultipleSessions() {
        // Given
        AdminUser user = createTestUser(1L, "admin", "admin@test.com");
        
        // When
        String token1 = sessionManager.createSession(user);
        String token2 = sessionManager.createSession(user);
        String token3 = sessionManager.createSession(user);
        
        // Then
        assertThat(token1).isNotEqualTo(token2);
        assertThat(token1).isNotEqualTo(token3);
        assertThat(token2).isNotEqualTo(token3);
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
}
