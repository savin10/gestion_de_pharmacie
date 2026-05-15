package com.pharmacie.benin.service;

import com.pharmacie.benin.model.AdminUser;
import com.pharmacie.benin.model.Session;
import com.pharmacie.benin.repository.AdminUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for handling authentication operations.
 * 
 * This service provides the core authentication logic including:
 * - Validating credentials using BCrypt password hashing
 * - Creating and managing sessions through SessionManager
 * - Integrating with BruteForceProtectionService for security
 * - Validating session tokens
 * - Retrieving the current authenticated user
 * 
 * The authentication flow:
 * 1. Check if IP is blocked by BruteForceProtectionService
 * 2. Validate username and password length (3-50 characters)
 * 3. Look up user in database
 * 4. Verify password using BCrypt
 * 5. Create session if authentication succeeds
 * 6. Update last login timestamp
 * 
 * Validates Requirements: 4.1, 4.2, 4.3, 4.4, 5.2, 5.5
 */
@Service
public class AuthenticationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    
    private static final int MIN_CREDENTIAL_LENGTH = 3;
    private static final int MAX_CREDENTIAL_LENGTH = 50;
    
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionManager sessionManager;
    private final BruteForceProtectionService bruteForceProtectionService;
    
    /**
     * Constructs the AuthenticationService with required dependencies.
     * 
     * @param adminUserRepository repository for admin user data access
     * @param passwordEncoder BCrypt password encoder for password validation
     * @param sessionManager manager for session creation and validation
     * @param bruteForceProtectionService service for brute force attack protection
     */
    public AuthenticationService(
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder,
            SessionManager sessionManager,
            BruteForceProtectionService bruteForceProtectionService) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionManager = sessionManager;
        this.bruteForceProtectionService = bruteForceProtectionService;
    }
    
    /**
     * Authenticates a user with the provided credentials.
     * 
     * This method performs the following steps:
     * 1. Validates input length (3-50 characters for both username and password)
     * 2. Looks up the user by username in the database
     * 3. Verifies the password using BCrypt
     * 4. Creates a new session if authentication succeeds
     * 5. Updates the user's last login timestamp
     * 
     * Note: Brute force protection checks should be performed by the caller
     * before invoking this method. This service focuses on credential validation.
     * 
     * @param username the username to authenticate
     * @param password the plain-text password to verify
     * @return AuthenticationResult containing success status, session token, and user info
     * @throws IllegalArgumentException if username or password is null or blank
     */
    @Transactional
    public AuthenticationResult authenticate(String username, String password) {
        // Validate inputs are not null or blank
        if (username == null || username.isBlank()) {
            logger.warn("Authentication attempt with null or blank username");
            return AuthenticationResult.failure("Username is required");
        }
        
        if (password == null || password.isBlank()) {
            logger.warn("Authentication attempt with null or blank password for username: {}", username);
            return AuthenticationResult.failure("Password is required");
        }
        
        // Validate input length (Requirement 4.4)
        if (username.length() < MIN_CREDENTIAL_LENGTH || username.length() > MAX_CREDENTIAL_LENGTH) {
            logger.warn("Authentication attempt with invalid username length: {} characters for username: {}", 
                       username.length(), username);
            return AuthenticationResult.failure(
                String.format("Username must be between %d and %d characters", 
                             MIN_CREDENTIAL_LENGTH, MAX_CREDENTIAL_LENGTH));
        }
        
        if (password.length() < MIN_CREDENTIAL_LENGTH || password.length() > MAX_CREDENTIAL_LENGTH) {
            logger.warn("Authentication attempt with invalid password length: {} characters for username: {}", 
                       password.length(), username);
            return AuthenticationResult.failure(
                String.format("Password must be between %d and %d characters", 
                             MIN_CREDENTIAL_LENGTH, MAX_CREDENTIAL_LENGTH));
        }
        
        // Look up user by username
        Optional<AdminUser> userOptional = adminUserRepository.findByUsername(username);
        
        if (userOptional.isEmpty()) {
            logger.warn("Authentication failed: user not found for username: {}", username);
            return AuthenticationResult.failure("Invalid username or password");
        }
        
        AdminUser user = userOptional.get();
        
        // Check if user account is active
        if (!user.getActive()) {
            logger.warn("Authentication failed: inactive account for username: {}", username);
            return AuthenticationResult.failure("Account is inactive");
        }
        
        // Verify password using BCrypt (Requirement 4.3)
        boolean passwordMatches = passwordEncoder.matches(password, user.getPasswordHash());
        
        if (!passwordMatches) {
            logger.warn("Authentication failed: invalid password for username: {}", username);
            return AuthenticationResult.failure("Invalid username or password");
        }
        
        // Authentication successful - create session (Requirement 4.1)
        String sessionToken = sessionManager.createSession(user);
        
        // Update last login timestamp
        user.setLastLoginAt(LocalDateTime.now());
        adminUserRepository.save(user);
        
        logger.info("Authentication successful for user: {} (ID: {})", username, user.getId());
        
        return AuthenticationResult.success(sessionToken, user);
    }
    
    /**
     * Logs out a user by invalidating their session.
     * 
     * This method calls SessionManager.invalidateSession to remove the session
     * from memory, making the session token unusable for subsequent requests.
     * 
     * @param sessionToken the session token to invalidate
     */
    public void logout(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            logger.warn("Logout attempt with null or blank session token");
            return;
        }
        
        // Retrieve session info for logging before invalidation
        Optional<Session> sessionOptional = sessionManager.getSession(sessionToken);
        
        if (sessionOptional.isPresent()) {
            Session session = sessionOptional.get();
            logger.info("Logging out user: {} (ID: {})", session.getUsername(), session.getUserId());
        }
        
        // Invalidate the session (Requirement 5.5)
        sessionManager.invalidateSession(sessionToken);
    }
    
    /**
     * Validates if a session token is valid and not expired.
     * 
     * This method checks if:
     * 1. The session token exists in the SessionManager
     * 2. The session has not expired (within 8 hours of creation)
     * 
     * @param sessionToken the session token to validate
     * @return true if the session is valid and not expired, false otherwise
     */
    public boolean validateSession(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return false;
        }
        
        // SessionManager.getSession already checks expiration and returns empty if expired
        Optional<Session> sessionOptional = sessionManager.getSession(sessionToken);
        
        boolean isValid = sessionOptional.isPresent();
        
        if (isValid) {
            Session session = sessionOptional.get();
            logger.debug("Session validation successful for user: {} (ID: {})", 
                        session.getUsername(), session.getUserId());
        } else {
            logger.debug("Session validation failed for token: {}", sessionToken);
        }
        
        return isValid;
    }
    
    /**
     * Retrieves the current authenticated user from a session token.
     * 
     * This method:
     * 1. Validates the session token
     * 2. Retrieves the user ID from the session
     * 3. Looks up the full user details from the database
     * 
     * @param sessionToken the session token
     * @return Optional containing the AdminUser if session is valid, empty otherwise
     */
    public Optional<AdminUser> getCurrentUser(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return Optional.empty();
        }
        
        // Get session from SessionManager
        Optional<Session> sessionOptional = sessionManager.getSession(sessionToken);
        
        if (sessionOptional.isEmpty()) {
            logger.debug("Cannot get current user: invalid or expired session");
            return Optional.empty();
        }
        
        Session session = sessionOptional.get();
        Long userId = session.getUserId();
        
        // Look up user in database
        Optional<AdminUser> userOptional = adminUserRepository.findById(userId);
        
        if (userOptional.isEmpty()) {
            logger.error("Session exists for user ID {} but user not found in database", userId);
            // Invalidate the orphaned session
            sessionManager.invalidateSession(sessionToken);
            return Optional.empty();
        }
        
        AdminUser user = userOptional.get();
        
        // Check if user is still active
        if (!user.getActive()) {
            logger.warn("Session exists for inactive user: {} (ID: {})", user.getUsername(), userId);
            // Invalidate session for inactive user
            sessionManager.invalidateSession(sessionToken);
            return Optional.empty();
        }
        
        logger.debug("Retrieved current user: {} (ID: {})", user.getUsername(), userId);
        
        return Optional.of(user);
    }
    
    /**
     * Result object for authentication operations.
     * 
     * Contains:
     * - success: whether authentication succeeded
     * - sessionToken: the generated session token (if successful)
     * - user: the authenticated user (if successful)
     * - errorMessage: error message (if failed)
     */
    public static class AuthenticationResult {
        private final boolean success;
        private final String sessionToken;
        private final AdminUser user;
        private final String errorMessage;
        
        private AuthenticationResult(boolean success, String sessionToken, AdminUser user, String errorMessage) {
            this.success = success;
            this.sessionToken = sessionToken;
            this.user = user;
            this.errorMessage = errorMessage;
        }
        
        /**
         * Creates a successful authentication result.
         * 
         * @param sessionToken the generated session token
         * @param user the authenticated user
         * @return AuthenticationResult with success=true
         */
        public static AuthenticationResult success(String sessionToken, AdminUser user) {
            return new AuthenticationResult(true, sessionToken, user, null);
        }
        
        /**
         * Creates a failed authentication result.
         * 
         * @param errorMessage the error message explaining why authentication failed
         * @return AuthenticationResult with success=false
         */
        public static AuthenticationResult failure(String errorMessage) {
            return new AuthenticationResult(false, null, null, errorMessage);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getSessionToken() {
            return sessionToken;
        }
        
        public AdminUser getUser() {
            return user;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
