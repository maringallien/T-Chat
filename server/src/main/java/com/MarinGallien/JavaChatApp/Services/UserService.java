package com.MarinGallien.JavaChatApp.Services;

import com.MarinGallien.JavaChatApp.Database.DatabaseServices.UserDbService;
import com.MarinGallien.JavaChatApp.Database.JPAEntities.User;
import com.MarinGallien.JavaChatApp.Services.AuthService.JWTService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private static Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserDbService userDbService;
    private final JWTService jwtService;

    public UserService(UserDbService userDbService, JWTService jwtService) {
        this.userDbService = userDbService;
        this.jwtService = jwtService;
    }

    public User createUser(String username, String email, String password) {
        // Validate input
        if (!validateString(username) || !validateEmail(email) || !validateString(password)) {
            logger.warn("Registration failed: invalid registration input");
            return null;
        }

        if (username.length() < 3 || username.length() > 50 || password.length() < 6) {
            logger.warn("Registration failed: invalid registration input");
            return null;
        }

        // Create user
        User user = userDbService.createUser(username, email, password);

        // Make sure user was created
        if (user == null) {
            logger.warn("Failed to create user: Email or username already exists");
            return null;
        }

        // Return new user
        logger.info("Successfully registered user: {} with email {}", user.getUserId(), user.getEmail());
        return user;
    }

    public String login(String email, String password) {
        // Validate input parameters
        if (!validateEmail(email) || !validateString(password)) {
            logger.warn("Login failed: invalid username or password input");
            return null;
        }

        Boolean loggedIn = userDbService.login(email, password);

        // Make sure user was logged in
        if (!loggedIn) {
            logger.error("User found during validation but not found during retrieval");
            return null;
        }

        // Retrieve user details for JWT generation
        User user = userDbService.getUserByEmail(email);

        if (user == null) {
            logger.warn("Failed to retrieve user by email");
            return null;
        }

        // Generate JWT token
        String token = jwtService.generateToken(user.getUserId(), user.getUsername());

        if (token == null) {
            logger.warn("Failed to generate token for user {}", user.getUserId());
            return null;
        }

        logger.info("Successful login for user with email {}", email);
        return token;
    }

    public boolean deleteUser(String userId, String password) {
        // Validate input
        if (!validateId(userId) || !validateString(password)) {
            logger.warn("Failed to delete user: invalid input parameters");
            return false;
        }

        // Call database service
        boolean deleted = userDbService.deleteUser(userId, password);

        if (!deleted) {
            logger.warn("Failed to delete user {}", userId);
            return false;
        }

        logger.info("Successfully deleted user {}", userId);
        return true;
    }

    public String updateUsername(String userId, String username) {
        // Validate input
        if (!validateId(userId) || !validateString(username)) {
            logger.warn("Failed to update username: invalid input parameters");
            return null;
        }

        if (username.length() < 3 || username.length() > 50) {
            logger.warn("Failed to update username: username must be between 3 and 50 characters");
            return null;
        }

        // Call database service
        String updatedUsername = userDbService.updateUsername(userId, username);

        if (updatedUsername == null) {
            logger.warn("Failed to update username for user {}", userId);
            return null;
        }

        logger.info("Successfully updated username for user {}", userId);
        return updatedUsername;
    }

    public String updateEmail(String userId, String email) {
        // Validate input
        if (!validateId(userId) || !validateEmail(email)) {
            logger.warn("Failed to update email: invalid input parameters");
            return null;
        }

        // Call database service
        String updatedEmail = userDbService.updateEmail(userId, email);

        if (updatedEmail == null) {
            logger.warn("Failed to update email for user {}", userId);
            return null;
        }

        logger.info("Successfully updated email for user {}", userId);
        return updatedEmail;
    }

    public boolean updatePassword(String userId, String oldPassword, String newPassword) {
        // Validate input
        if (!validateId(userId) || !validateString(oldPassword) || !validateString(newPassword)) {
            logger.warn("Failed to update password: invalid input parameters");
            return false;
        }

        if (newPassword.length() < 6) {
            logger.warn("Failed to update password: password must be at least 6 characters");
            return false;
        }

        // Call database service with new password
        boolean updated = userDbService.updatePassword(userId, oldPassword, newPassword);

        if (!updated) {
            logger.warn("Failed to update password for user {}", userId);
            return false;
        }

        logger.info("Successfully updated password for user {}", userId);
        return true;
    }

    public String getUserIdFromUsername(String username) {
        if (username == null || username.isEmpty()) {
            logger.warn("Failed to retrieve user Id: username is null or empty");
            return null;
        }
        return userDbService.getUserIdByUsername(username);
    }

    public String getUsernameByUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            logger.error("Failed to retrieve username from user ID");
            return null;
        }
        return userDbService.getUsernameByUserId(userId);
    }

    public String getUsernameByEmail(String email) {
        if (email == null || email.isEmpty()) {
            logger.error("Failed to retrieve username from user email");
            return null;
        }
        User user =  userDbService.getUserByEmail(email);
        return user.getUsername();
    }

    private boolean validateEmail(String email) {
        return email != null && !email.trim().isEmpty() && email.contains("@") && email.contains(".");
    }

    private boolean validateId(String id) {
        return id != null && !id.trim().isEmpty();
    }

    private boolean validateString(String str) {
        return str != null && !str.trim().isEmpty();
    }

}
