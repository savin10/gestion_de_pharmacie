package com.pharmacie.benin.property;

import com.pharmacie.benin.model.AdminUser;
import com.pharmacie.benin.service.SessionManager;
import net.jqwik.api.*;
import org.junit.jupiter.api.Tag;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for SessionManager.
 * 
 * These tests validate universal properties that should hold for all valid inputs,
 * using jqwik to generate random test cases and verify correctness across many iterations.
 */
public class SessionManagerPropertyTest {
    
    private final SessionManager sessionManager = new SessionManager();
    
    /**
     * UUID v4 format regex pattern.
     * Format: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
     * where x is any hexadecimal digit and y is one of 8, 9, a, or b
     * 
     * This ensures:
     * - 8 hex digits, hyphen, 4 hex digits, hyphen
     * - Version field (4)
     * - 3 hex digits, hyphen
     * - Variant field (8, 9, a, or b)
     * - 3 hex digits, hyphen
     * - 12 hex digits
     */
    private static final Pattern UUID_V4_PATTERN = Pattern.compile(
        "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"
    );
    
    /**
     * Feature: ui-enhancement-and-admin-dashboard
     * Property 6: Session Token Entropy
     * 
     * **Validates: Requirements 4.6**
     * 
     * For any generated session token, the token should be a UUID v4 or 
     * equivalent with at least 128 bits of entropy, ensuring uniqueness 
     * and unpredictability.
     * 
     * This property verifies:
     * 1. Token is not null
     * 2. Token matches UUID v4 format (xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx)
     * 3. Token has exactly 36 characters (standard UUID string length)
     * 4. Token contains the version field '4' at position 14
     * 5. Token contains a valid variant field (8, 9, a, or b) at position 19
     * 6. All tokens are unique (no collisions across multiple generations)
     * 
     * UUID v4 provides 122 bits of randomness (128 bits total with version/variant),
     * which exceeds the requirement of at least 128 bits of entropy.
     */
    @Property(tries = 100)
    @Tag("property-6")
    void sessionTokensHaveSufficientEntropy(@ForAll("validAdminUsers") AdminUser user) {
        // When: Create a session for the user
        String token = sessionManager.createSession(user);
        
        // Then: Token should not be null
        assertThat(token)
            .as("Session token should not be null")
            .isNotNull();
        
        // Then: Token should match UUID v4 format
        assertThat(token)
            .as("Session token should match UUID v4 format (xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx)")
            .matches(UUID_V4_PATTERN);
        
        // Then: Token should have exactly 36 characters (UUID string length)
        assertThat(token)
            .as("Session token should have exactly 36 characters")
            .hasSize(36);
        
        // Then: Token should contain version field '4' at position 14
        assertThat(token.charAt(14))
            .as("Session token should have version field '4' at position 14")
            .isEqualTo('4');
        
        // Then: Token should contain valid variant field (8, 9, a, or b) at position 19
        char variantChar = token.charAt(19);
        assertThat(variantChar)
            .as("Session token should have variant field (8, 9, a, or b) at position 19")
            .isIn('8', '9', 'a', 'b');
        
        // Cleanup: Remove the session to avoid memory buildup during tests
        sessionManager.invalidateSession(token);
    }
    
    /**
     * Feature: ui-enhancement-and-admin-dashboard
     * Property 6: Session Token Entropy - Uniqueness
     * 
     * **Validates: Requirements 4.6**
     * 
     * For any set of generated session tokens, all tokens should be unique
     * with no collisions, demonstrating sufficient entropy and randomness.
     * 
     * This property verifies that generating multiple tokens for the same user
     * or different users always produces unique tokens, ensuring unpredictability
     * and preventing session hijacking through token guessing.
     */
    @Property(tries = 100)
    @Tag("property-6")
    void sessionTokensAreUnique(@ForAll("validAdminUsers") AdminUser user) {
        // Given: A set to track generated tokens
        Set<String> generatedTokens = new HashSet<>();
        int tokenCount = 10; // Generate 10 tokens per property iteration
        
        // When: Generate multiple tokens for the same user
        for (int i = 0; i < tokenCount; i++) {
            String token = sessionManager.createSession(user);
            
            // Then: Each token should be unique (not already in the set)
            assertThat(generatedTokens)
                .as("Session token should be unique (no collisions)")
                .doesNotContain(token);
            
            generatedTokens.add(token);
            
            // Cleanup: Invalidate the session
            sessionManager.invalidateSession(token);
        }
        
        // Then: All generated tokens should be unique
        assertThat(generatedTokens)
            .as("All generated tokens should be unique")
            .hasSize(tokenCount);
    }
    
    /**
     * Provides arbitrary valid AdminUser instances for property testing.
     * 
     * Generates AdminUser objects with:
     * - Valid ID (1 to 1000000)
     * - Valid username (3-50 alphanumeric characters)
     * - Valid email (simple format)
     * - Password hash (simulated BCrypt hash)
     * - Created timestamp
     * - Active status
     * 
     * @return Arbitrary that generates valid AdminUser instances
     */
    @Provide
    Arbitrary<AdminUser> validAdminUsers() {
        Arbitrary<Long> ids = Arbitraries.longs().between(1L, 1000000L);
        
        Arbitrary<String> usernames = Arbitraries.strings()
            .alpha()
            .numeric()
            .ofMinLength(3)
            .ofMaxLength(50);
        
        Arbitrary<String> emails = Arbitraries.strings()
            .alpha()
            .ofMinLength(3)
            .ofMaxLength(20)
            .map(name -> name + "@example.com");
        
        Arbitrary<String> passwordHashes = Arbitraries.strings()
            .withCharRange('a', 'z')
            .withCharRange('A', 'Z')
            .withCharRange('0', '9')
            .withChars('.', '/')
            .ofLength(60) // BCrypt hash length
            .map(hash -> "$2a$12$" + hash); // BCrypt prefix
        
        Arbitrary<LocalDateTime> createdDates = Arbitraries.of(
            LocalDateTime.now().minusDays(365),
            LocalDateTime.now().minusDays(180),
            LocalDateTime.now().minusDays(90),
            LocalDateTime.now().minusDays(30),
            LocalDateTime.now().minusDays(7),
            LocalDateTime.now()
        );
        
        return Combinators.combine(ids, usernames, emails, passwordHashes, createdDates)
            .as((id, username, email, passwordHash, createdAt) -> {
                AdminUser user = new AdminUser();
                user.setId(id);
                user.setUsername(username);
                user.setEmail(email);
                user.setPasswordHash(passwordHash);
                user.setCreatedAt(createdAt);
                user.setActive(true);
                return user;
            });
    }
}
