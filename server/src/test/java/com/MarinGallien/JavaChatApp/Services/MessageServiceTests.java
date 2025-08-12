package com.MarinGallien.JavaChatApp.Services;

import com.MarinGallien.JavaChatApp.DTOs.DataEntities.MessageDTO;
import com.MarinGallien.JavaChatApp.Database.DatabaseServices.MessageDbService;
import com.MarinGallien.JavaChatApp.Database.JPAEntities.Message;
import com.MarinGallien.JavaChatApp.Database.Mappers.MessageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MessageServiceTests {

    @Mock
    private MessageDbService messageDbService;

    @InjectMocks
    private MessageService messageService;

    @Mock
    private MessageMapper messageMapper;

    private Message testMessage;
    private MessageDTO testMessageDTO;
    private String senderId = "user1";
    private String senderUname = "bob";
    private String chatId = "chat1";
    private String content = "Hello World!";

    @BeforeEach
    void setUp() {
        testMessage = new Message();

        testMessageDTO = new MessageDTO(
                "msg1",
                senderId,
                senderUname,
                chatId,
                content,
                LocalDateTime.now()
        );
    }

    // ==========================================================================
    // SAVE MESSAGE TESTS
    // ==========================================================================

    @Test
    void saveMessage_ValidInputs_SavesMessage() {
        // Given
        when(messageDbService.saveMessage(senderId, chatId, content)).thenReturn(testMessage);

        // When
        Message result = messageService.saveMessage(senderId, chatId, content);

        // Then
        assertNotNull(result);
        assertEquals(testMessage, result);
        verify(messageDbService).saveMessage(senderId, chatId, content);
    }

    @Test
    void saveMessage_InvalidParameters_ReturnsNull() {
        // Test all null parameters
        assertNull(messageService.saveMessage(null, chatId, content));
        assertNull(messageService.saveMessage(senderId, null, content));
        assertNull(messageService.saveMessage(senderId, chatId, null));

        // Test all empty parameters
        assertNull(messageService.saveMessage("", chatId, content));
        assertNull(messageService.saveMessage(senderId, "", content));
        assertNull(messageService.saveMessage(senderId, chatId, ""));

        // Test all whitespace parameters
        assertNull(messageService.saveMessage("   ", chatId, content));
        assertNull(messageService.saveMessage(senderId, "   ", content));
        assertNull(messageService.saveMessage(senderId, chatId, "   "));

        // Verify no database calls were made
        verify(messageDbService, never()).saveMessage(any(), any(), any());
    }

    @Test
    void saveMessage_DatabaseFailure_ReturnsNull() {
        // Given
        when(messageDbService.saveMessage(senderId, chatId, content)).thenReturn(null);

        // When
        Message result = messageService.saveMessage(senderId, chatId, content);

        // Then
        assertNull(result);
        verify(messageDbService).saveMessage(senderId, chatId, content);
    }

    // ==========================================================================
    // GET CHAT MESSAGES TESTS
    // ==========================================================================

    @Test
    void getChatMessages_ValidInputs_ReturnsMessagesList() {
        // Given
        List<Message> messagesList = List.of(testMessage);
        List<MessageDTO> messageDTOsList = List.of(testMessageDTO);

        when(messageDbService.getChatMessages(senderId, chatId)).thenReturn(messagesList);
        when(messageMapper.toDTOList(messagesList)).thenReturn(messageDTOsList);

        // When
        List<MessageDTO> result = messageService.getChatMessages(senderId, chatId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testMessageDTO, result.get(0));

        verify(messageDbService).getChatMessages(senderId, chatId);
        verify(messageMapper).toDTOList(messagesList);
    }

    @Test
    void getChatMessages_InvalidParameters_ReturnsEmptyList() {
        // Test all null parameters
        assertTrue(messageService.getChatMessages(null, chatId).isEmpty());
        assertTrue(messageService.getChatMessages(senderId, null).isEmpty());

        // Test all empty parameters
        assertTrue(messageService.getChatMessages("", chatId).isEmpty());
        assertTrue(messageService.getChatMessages(senderId, "").isEmpty());

        // Test all whitespace parameters
        assertTrue(messageService.getChatMessages("   ", chatId).isEmpty());
        assertTrue(messageService.getChatMessages(senderId, "   ").isEmpty());

        // Verify no database calls were made
        verify(messageDbService, never()).getChatMessages(any(), any());
    }

    @Test
    void getChatMessages_MultipleMessages_ReturnsAllMessages() {
        // Given
        Message message1 = new Message();
        Message message2 = new Message();
        Message message3 = new Message();
        List<Message> messagesList = List.of(message1, message2, message3);

        MessageDTO messageDTO1 = new MessageDTO("msg1", senderId, senderUname, chatId, "Message 1", LocalDateTime.now());
        MessageDTO messageDTO2 = new MessageDTO("msg2", senderId, senderUname, chatId, "Message 2", LocalDateTime.now());
        MessageDTO messageDTO3 = new MessageDTO("msg3", senderId, senderUname, chatId, "Message 3", LocalDateTime.now());
        List<MessageDTO> messageDTOsList = List.of(messageDTO1, messageDTO2, messageDTO3);

        when(messageDbService.getChatMessages(senderId, chatId)).thenReturn(messagesList);
        when(messageMapper.toDTOList(messagesList)).thenReturn(messageDTOsList);

        // When
        List<MessageDTO> result = messageService.getChatMessages(senderId, chatId);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(messageDTO1, result.get(0));
        assertEquals(messageDTO2, result.get(1));
        assertEquals(messageDTO3, result.get(2));

        verify(messageDbService).getChatMessages(senderId, chatId);
        verify(messageMapper).toDTOList(messagesList);
    }

    @Test
    void getChatMessages_DatabaseFailureOrNoMessagesFound_ReturnsEmptyList() {
        // Given - simulate database service returning null (error case)
        when(messageDbService.getChatMessages(senderId, chatId)).thenReturn(List.of());

        // When
        List<MessageDTO> result = messageService.getChatMessages(senderId, chatId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(messageDbService).getChatMessages(senderId, chatId);
    }

    // ==========================================================================
    // EDGE CASE TESTS
    // ==========================================================================

    @Test
    void saveMessage_LongContent_SavesMessage() {
        // Given
        String longContent = "This is a very long message content that simulates a user typing a lot of text to test " +
                "how the service handles longer messages which might be common in chat applications.";
        when(messageDbService.saveMessage(senderId, chatId, longContent)).thenReturn(testMessage);

        // When
        Message result = messageService.saveMessage(senderId, chatId, longContent);

        // Then
        assertNotNull(result);
        assertEquals(testMessage, result);
        verify(messageDbService).saveMessage(senderId, chatId, longContent);
    }

    @Test
    void saveMessage_SpecialCharacters_SavesMessage() {
        // Given
        String specialContent = "Hello! 😀 How are you? #hashtag @mention https://example.com";
        when(messageDbService.saveMessage(senderId, chatId, specialContent)).thenReturn(testMessage);

        // When
        Message result = messageService.saveMessage(senderId, chatId, specialContent);

        // Then
        assertNotNull(result);
        assertEquals(testMessage, result);
        verify(messageDbService).saveMessage(senderId, chatId, specialContent);
    }

}
