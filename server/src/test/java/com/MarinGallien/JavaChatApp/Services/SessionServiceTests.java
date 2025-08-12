package com.MarinGallien.JavaChatApp.Services;

import com.MarinGallien.JavaChatApp.Database.DatabaseServices.SessionDbService;
import com.MarinGallien.JavaChatApp.Enums.OnlineStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SessionServiceTests {

    @Mock
    private SessionDbService sessionDbService;

    @InjectMocks
    private SessionService sessionService;

    private String userId = "user1";

    @BeforeEach
    void setUp() {
        // Setup test data if needed
    }

    // ==========================================================================
    // UPDATE USER STATUS TESTS
    // ==========================================================================

    @Test
    void updateUserStatus_OnlineToOffline_UpdatesStatus() {
        // Given
        when(sessionDbService.updateStatus(userId, OnlineStatus.OFFLINE)).thenReturn(OnlineStatus.OFFLINE);

        // When
        OnlineStatus result = sessionService.updateUserStatus(userId, OnlineStatus.OFFLINE);

        // Then
        assertNotNull(result);
        assertEquals(OnlineStatus.OFFLINE, result);
        verify(sessionDbService).updateStatus(userId, OnlineStatus.OFFLINE);
    }

    @Test
    void updateUserStatus_OfflineToOnline_UpdatesStatus() {
        // Given
        when(sessionDbService.updateStatus(userId, OnlineStatus.ONLINE)).thenReturn(OnlineStatus.ONLINE);

        // When
        OnlineStatus result = sessionService.updateUserStatus(userId, OnlineStatus.ONLINE);

        // Then
        assertNotNull(result);
        assertEquals(OnlineStatus.ONLINE, result);
        verify(sessionDbService).updateStatus(userId, OnlineStatus.ONLINE);
    }

    @Test
    void updateUserStatus_InvalidParameters_ReturnsNull() {
        // Test null user ID
        assertNull(sessionService.updateUserStatus(null, OnlineStatus.ONLINE));
        assertNull(sessionService.updateUserStatus(userId, null));

        // Test empty user ID
        assertNull(sessionService.updateUserStatus("", OnlineStatus.ONLINE));

        // Test whitespace user ID
        assertNull(sessionService.updateUserStatus("   ", OnlineStatus.ONLINE));

        // Verify no database calls were made
        verify(sessionDbService, never()).updateStatus(any(), any());
    }

    @Test
    void updateUserStatus_DatabaseFailure_ReturnsNull() {
        // Given
        when(sessionDbService.updateStatus(userId, OnlineStatus.ONLINE)).thenReturn(null);

        // When
        OnlineStatus result = sessionService.updateUserStatus(userId, OnlineStatus.ONLINE);

        // Then
        assertNull(result);
        verify(sessionDbService).updateStatus(userId, OnlineStatus.ONLINE);
    }

    @Test
    void updateUserStatus_DatabaseReturnsDifferentStatus_ReturnsNull() {
        // Given - database returns a different status than what was requested
        when(sessionDbService.updateStatus(userId, OnlineStatus.ONLINE)).thenReturn(OnlineStatus.OFFLINE);

        // When
        OnlineStatus result = sessionService.updateUserStatus(userId, OnlineStatus.ONLINE);

        // Then
        assertNull(result);
        verify(sessionDbService).updateStatus(userId, OnlineStatus.ONLINE);
    }

    // ==========================================================================
    // EDGE CASE TESTS
    // ==========================================================================

    @Test
    void updateUserStatus_DifferentUsers_UpdatesCorrectly() {
        // Given
        String user1Id = "user1";
        String user2Id = "user2";
        when(sessionDbService.updateStatus(user1Id, OnlineStatus.ONLINE)).thenReturn(OnlineStatus.ONLINE);
        when(sessionDbService.updateStatus(user2Id, OnlineStatus.OFFLINE)).thenReturn(OnlineStatus.OFFLINE);

        // When
        OnlineStatus result1 = sessionService.updateUserStatus(user1Id, OnlineStatus.ONLINE);
        OnlineStatus result2 = sessionService.updateUserStatus(user2Id, OnlineStatus.OFFLINE);

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(OnlineStatus.ONLINE, result1);
        assertEquals(OnlineStatus.OFFLINE, result2);
        verify(sessionDbService).updateStatus(user1Id, OnlineStatus.ONLINE);
        verify(sessionDbService).updateStatus(user2Id, OnlineStatus.OFFLINE);
    }

    @Test
    void updateUserStatus_SameStatusMultipleTimes_UpdatesCorrectly() {
        // Given
        when(sessionDbService.updateStatus(userId, OnlineStatus.ONLINE)).thenReturn(OnlineStatus.ONLINE);

        // When
        OnlineStatus result1 = sessionService.updateUserStatus(userId, OnlineStatus.ONLINE);
        OnlineStatus result2 = sessionService.updateUserStatus(userId, OnlineStatus.ONLINE);

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(OnlineStatus.ONLINE, result1);
        assertEquals(OnlineStatus.ONLINE, result2);
        verify(sessionDbService, times(2)).updateStatus(userId, OnlineStatus.ONLINE);
    }
}