package com.MarinGallien.JavaChatApp.Services;

import com.MarinGallien.JavaChatApp.Database.DatabaseServices.UserDbService;
import com.MarinGallien.JavaChatApp.Database.JPAEntities.User;
import com.MarinGallien.JavaChatApp.Services.AuthService.JWTService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTests {

    @Mock
    private UserDbService userDbService;

    @Mock
    private JWTService jwtService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private String userId = "user1";
    private String username = "testuser";
    private String email = "test@example.com";
    private String password = "password123";
    private String newPassword = "newpassword456";

    @BeforeEach
    void setUp() {
        testUser = new User();
    }

    // ==========================================================================
    // CREATE USER TESTS
    // ==========================================================================

    @Test
    void createUser_ValidInputs_CreatesUser() {
        // Given
        when(userDbService.createUser(username, email, password)).thenReturn(testUser);

        // When
        User result = userService.createUser(username, email, password);

        // Then
        assertNotNull(result);
        assertEquals(testUser, result);
        verify(userDbService).createUser(username, email, password);
    }

    @Test
    void createUser_InvalidParameters_ReturnsNull() {
        // Test null parameters
        assertNull(userService.createUser(null, email, password));
        assertNull(userService.createUser(username, null, password));
        assertNull(userService.createUser(username, email, null));

        // Test empty parameters
        assertNull(userService.createUser("", email, password));
        assertNull(userService.createUser(username, "", password));
        assertNull(userService.createUser(username, email, ""));

        // Test whitespace parameters
        assertNull(userService.createUser("   ", email, password));
        assertNull(userService.createUser(username, "   ", password));
        assertNull(userService.createUser(username, email, "   "));

        // Test invalid email (no @ or .)
        assertNull(userService.createUser(username, "invalidemail", password));
        assertNull(userService.createUser(username, "invalid@", password));
        assertNull(userService.createUser(username, "invalid.com", password));

        // Test username too short/long
        assertNull(userService.createUser("ab", email, password)); // too short
        assertNull(userService.createUser("a".repeat(51), email, password)); // too long

        // Test password too short
        assertNull(userService.createUser(username, email, "12345")); // too short

        // Verify no database calls were made
        verify(userDbService, never()).createUser(any(), any(), any());
    }

    @Test
    void createUser_DatabaseFailure_ReturnsNull() {
        // Given
        when(userDbService.createUser(username, email, password)).thenReturn(null);

        // When
        User result = userService.createUser(username, email, password);

        // Then
        assertNull(result);
        verify(userDbService).createUser(username, email, password);
    }

    // ==========================================================================
    // LOGIN TESTS
    // ==========================================================================

    @Test
    void login_ValidInputs_ReturnsTrue() {
        // Given
        User mockUser = new User("testuser", email, "hashedPassword");
        when(userDbService.login(email, password)).thenReturn(true);
        when(userDbService.getUserByEmail(email)).thenReturn(mockUser);
        when(jwtService.generateToken(mockUser.getUserId(), mockUser.getUsername())).thenReturn("jwt.token.here");

        // When
        String token = userService.login(email, password);

        // Then
        assertNotNull(token);
        assertEquals("jwt.token.here", token);
        verify(userDbService).login(email, password);
        verify(userDbService).getUserByEmail(email);
        verify(jwtService).generateToken(mockUser.getUserId(), mockUser.getUsername());
    }

    @Test
    void login_InvalidParameters_ReturnsNull() {
        // Test null parameters
        assertNull(userService.login(null, password));
        assertNull(userService.login(email, null));

        // Test empty parameters
        assertNull(userService.login("", password));
        assertNull(userService.login(email, ""));

        // Test whitespace parameters
        assertNull(userService.login("   ", password));
        assertNull(userService.login(email, "   "));

        // Test invalid email
        assertNull(userService.login("invalidemail", password));

        // Verify no database calls were made
        verify(userDbService, never()).login(any(), any());
        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    void login_DatabaseFailure_ReturnsNull() {
        // Given
        when(userDbService.login(email, password)).thenReturn(false);

        // When
        String token = userService.login(email, password);

        // Then
        assertNull(token);
        verify(userDbService).login(email, password);
        verify(userDbService, never()).getUserByEmail(any());
        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    void login_UserNotFoundAfterValidation_ReturnsNull() {
        // Given
        when(userDbService.login(email, password)).thenReturn(true);
        when(userDbService.getUserByEmail(email)).thenReturn(null);

        // When
        String token = userService.login(email, password);

        // Then
        assertNull(token);
        verify(userDbService).login(email, password);
        verify(userDbService).getUserByEmail(email);
        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    void login_JwtGenerationFailure_ReturnsNull() {
        // Given
        User mockUser = new User("testuser", email, "hashedPassword");
        when(userDbService.login(email, password)).thenReturn(true);
        when(userDbService.getUserByEmail(email)).thenReturn(mockUser);
        when(jwtService.generateToken(mockUser.getUserId(), mockUser.getUsername())).thenReturn(null);

        // When
        String token = userService.login(email, password);

        // Then
        assertNull(token);
        verify(userDbService).login(email, password);
        verify(userDbService).getUserByEmail(email);
        verify(jwtService).generateToken(mockUser.getUserId(), mockUser.getUsername());
    }

    // ==========================================================================
    // DELETE USER TESTS
    // ==========================================================================

    @Test
    void deleteUser_ValidInputs_DeletesUser() {
        // Given
        when(userDbService.deleteUser(userId, password)).thenReturn(true);

        // When
        boolean result = userService.deleteUser(userId, password);

        // Then
        assertTrue(result);
        verify(userDbService).deleteUser(userId, password);
    }

    @Test
    void deleteUser_InvalidParameters_ReturnsFalse() {
        // Test null parameters
        assertFalse(userService.deleteUser(null, password));
        assertFalse(userService.deleteUser(userId, null));

        // Test empty parameters
        assertFalse(userService.deleteUser("", password));
        assertFalse(userService.deleteUser(userId, ""));

        // Test whitespace parameters
        assertFalse(userService.deleteUser("   ", password));
        assertFalse(userService.deleteUser(userId, "   "));

        // Verify no database calls were made
        verify(userDbService, never()).deleteUser(any(), any());
    }

    @Test
    void deleteUser_DatabaseFailure_ReturnsFalse() {
        // Given
        when(userDbService.deleteUser(userId, password)).thenReturn(false);

        // When
        boolean result = userService.deleteUser(userId, password);

        // Then
        assertFalse(result);
        verify(userDbService).deleteUser(userId, password);
    }

    // ==========================================================================
    // UPDATE USERNAME TESTS
    // ==========================================================================

    @Test
    void updateUsername_ValidInputs_UpdatesUsername() {
        // Given
        String newUsername = "newUsername";
        when(userDbService.updateUsername(userId, newUsername)).thenReturn(newUsername);

        // When
        String result = userService.updateUsername(userId, newUsername);

        // Then
        assertNotNull(result);
        assertEquals(newUsername, result);
        verify(userDbService).updateUsername(userId, newUsername);
    }

    @Test
    void updateUsername_InvalidParameters_ReturnsNull() {
        // Test null parameters
        assertNull(userService.updateUsername(null, username));
        assertNull(userService.updateUsername(userId, null));

        // Test empty parameters
        assertNull(userService.updateUsername("", username));
        assertNull(userService.updateUsername(userId, ""));

        // Test whitespace parameters
        assertNull(userService.updateUsername("   ", username));
        assertNull(userService.updateUsername(userId, "   "));

        // Test username too short/long
        assertNull(userService.updateUsername(userId, "ab")); // too short
        assertNull(userService.updateUsername(userId, "a".repeat(51))); // too long

        // Verify no database calls were made
        verify(userDbService, never()).updateUsername(any(), any());
    }

    @Test
    void updateUsername_DatabaseFailure_ReturnsNull() {
        // Given
        String newUsername = "newUsername";
        when(userDbService.updateUsername(userId, newUsername)).thenReturn(null);

        // When
        String result = userService.updateUsername(userId, newUsername);

        // Then
        assertNull(result);
        verify(userDbService).updateUsername(userId, newUsername);
    }

    // ==========================================================================
    // UPDATE EMAIL TESTS
    // ==========================================================================

    @Test
    void updateEmail_ValidInputs_UpdatesEmail() {
        // Given
        String newEmail = "newemail@example.com";
        when(userDbService.updateEmail(userId, newEmail)).thenReturn(newEmail);

        // When
        String result = userService.updateEmail(userId, newEmail);

        // Then
        assertNotNull(result);
        assertEquals(newEmail, result);
        verify(userDbService).updateEmail(userId, newEmail);
    }

    @Test
    void updateEmail_InvalidParameters_ReturnsNull() {
        // Test null parameters
        assertNull(userService.updateEmail(null, email));
        assertNull(userService.updateEmail(userId, null));

        // Test empty parameters
        assertNull(userService.updateEmail("", email));
        assertNull(userService.updateEmail(userId, ""));

        // Test whitespace parameters
        assertNull(userService.updateEmail("   ", email));
        assertNull(userService.updateEmail(userId, "   "));

        // Test invalid email
        assertNull(userService.updateEmail(userId, "invalidemail"));

        // Verify no database calls were made
        verify(userDbService, never()).updateEmail(any(), any());
    }

    @Test
    void updateEmail_DatabaseFailure_ReturnsNull() {
        // Given
        String newEmail = "newemail@example.com";
        when(userDbService.updateEmail(userId, newEmail)).thenReturn(null);

        // When
        String result = userService.updateEmail(userId, newEmail);

        // Then
        assertNull(result);
        verify(userDbService).updateEmail(userId, newEmail);
    }

    // ==========================================================================
    // UPDATE PASSWORD TESTS
    // ==========================================================================

    @Test
    void updatePassword_ValidInputs_UpdatesPassword() {
        // Given
        when(userDbService.updatePassword(userId, password, newPassword)).thenReturn(true);

        // When
        boolean result = userService.updatePassword(userId, password, newPassword);

        // Then
        assertTrue(result);
        verify(userDbService).updatePassword(userId, password, newPassword);
    }

    @Test
    void updatePassword_InvalidParameters_ReturnsFalse() {
        // Test null parameters
        assertFalse(userService.updatePassword(null, password, newPassword));
        assertFalse(userService.updatePassword(userId, null, newPassword));
        assertFalse(userService.updatePassword(userId, password, null));

        // Test empty parameters
        assertFalse(userService.updatePassword("", password, newPassword));
        assertFalse(userService.updatePassword(userId, "", newPassword));
        assertFalse(userService.updatePassword(userId, password, ""));

        // Test whitespace parameters
        assertFalse(userService.updatePassword("   ", password, newPassword));
        assertFalse(userService.updatePassword(userId, "   ", newPassword));
        assertFalse(userService.updatePassword(userId, password, "   "));

        // Test new password too short
        assertFalse(userService.updatePassword(userId, password, "12345"));

        // Verify no database calls were made
        verify(userDbService, never()).updatePassword(any(), any(), any());
    }

    @Test
    void updatePassword_DatabaseFailure_ReturnsFalse() {
        // Given
        when(userDbService.updatePassword(userId, password, newPassword)).thenReturn(false);

        // When
        boolean result = userService.updatePassword(userId, password, newPassword);

        // Then
        assertFalse(result);
        verify(userDbService).updatePassword(userId, password, newPassword);
    }
}