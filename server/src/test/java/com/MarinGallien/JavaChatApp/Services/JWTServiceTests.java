package com.MarinGallien.JavaChatApp.Services.AuthService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class JWTServiceTests {

    private JWTService jwtService;
    private final String testUserId = "user123";
    private final String testUsername = "testuser";

    @BeforeEach
    void setUp() {
        jwtService = new JWTService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "testSecretKeyForJWTTokensThatIsLongEnoughForHS256Algorithm");
        ReflectionTestUtils.setField(jwtService, "tokenExpirationHours", 24L);
    }

    @Test
    void generateToken_ValidInputs_ReturnsToken() {
        String token = jwtService.generateToken(testUserId, testUsername);

        assertNotNull(token);
        assertTrue(token.contains("."));
    }

    @Test
    void generateToken_NullInputs_ReturnsNull() {
        assertNull(jwtService.generateToken(null, testUsername));
        assertNull(jwtService.generateToken(testUserId, null));
    }

    @Test
    void extractUserId_ValidToken_ReturnsUserId() {
        String token = jwtService.generateToken(testUserId, testUsername);

        assertEquals(testUserId, jwtService.extractUserId(token));
    }

    @Test
    void extractUserId_InvalidToken_ReturnsNull() {
        assertNull(jwtService.extractUserId("invalid-token"));
        assertNull(jwtService.extractUserId(null));
    }

    @Test
    void extractUsername_ValidToken_ReturnsUsername() {
        String token = jwtService.generateToken(testUserId, testUsername);

        assertEquals(testUsername, jwtService.extractUsername(token));
    }

    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        String token = jwtService.generateToken(testUserId, testUsername);

        assertTrue(jwtService.validateToken(token));
    }

    @Test
    void validateToken_InvalidToken_ReturnsFalse() {
        assertFalse(jwtService.validateToken("invalid-token"));
        assertFalse(jwtService.validateToken(null));
    }

    @Test
    void isTokenExpired_ValidToken_ReturnsFalse() {
        String token = jwtService.generateToken(testUserId, testUsername);

        assertFalse(jwtService.isTokenExpired(token));
    }

    @Test
    void getExpirationDate_ValidToken_ReturnsDate() {
        String token = jwtService.generateToken(testUserId, testUsername);
        Date expirationDate = jwtService.getExpirationDate(token);

        assertNotNull(expirationDate);
        assertTrue(expirationDate.after(new Date()));
    }

    @Test
    void extractTokenFromHeader_ValidHeader_ReturnsToken() {
        String token = "test.jwt.token";
        String authHeader = "Bearer " + token;

        assertEquals(token, jwtService.extractTokenFromHeader(authHeader));
    }

    @Test
    void extractTokenFromHeader_InvalidHeader_ReturnsNull() {
        assertNull(jwtService.extractTokenFromHeader("InvalidHeader"));
        assertNull(jwtService.extractTokenFromHeader(null));
    }
}