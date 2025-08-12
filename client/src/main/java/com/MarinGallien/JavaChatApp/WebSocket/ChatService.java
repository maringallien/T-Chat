package com.MarinGallien.JavaChatApp.WebSocket;

import ch.qos.logback.classic.Level;
import com.MarinGallien.JavaChatApp.DTOs.WebsocketMessages.OnlineStatusMessage;
import com.MarinGallien.JavaChatApp.DTOs.WebsocketMessages.WebSocketMessage;
import com.MarinGallien.JavaChatApp.Enums.OnlineStatus;
import com.MarinGallien.JavaChatApp.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatService implements WebSocketClient.MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final WebSocketClient webSocketClient;
    private MessageListener messageListener;
    private boolean connected = false;

    // Interface for components that want to receive chat messages
    public interface MessageListener {
        void onMessageReceived(String chatId, String senderId, String username, String message);
        void onStatusChanged(String userId, String username, OnlineStatus status);
        void onConnectionChanged(boolean connected);
        void onError(String error);
    }

    public ChatService(WebSocketClient webSocketClient) {
        ((ch.qos.logback.classic.Logger) logger).setLevel(Level.OFF);
        this.webSocketClient = webSocketClient;
    }


    // ========== LISTENER MANAGEMENT ==========

    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }


    // ========== CONNECTION MANAGEMENT METHODS ==========

    // Start chat session
    public void startChat() {
        // Attempt to connect to the WebSocket server with JWT authentication
        webSocketClient.connect(this)
            .thenRun(() -> {
                logger.info("Chat started");

                // Broadcast online status to contacts
                sendOnlineStatus(OnlineStatus.ONLINE);

            })
            .exceptionally(error -> {
                connected = false;
                logger.error("Failed to connect: {}", error.getMessage());

                // Notify message listener
                if (messageListener != null) {
                    messageListener.onError("Failed to connect: " + error.getMessage());
                }
                return null;
            });
    }

    public void stopChat() {
        sendOnlineStatus(OnlineStatus.OFFLINE);
        webSocketClient.disconnect();
        connected = false;

        if (messageListener != null) {
            messageListener.onConnectionChanged(false);
        }
    }

    public boolean isConnected() {
        return connected;
    }


    // ========== COMMUNICATION METHODS =========

    public void sendMessage(String chatId, String content) {
        if (!connected) {
            logger.warn("Cannot send message: not connected");
            return;
        }
        String username = UserSession.getInstance().getUsername();
        logger.info("SENDING - Username from UserSession: '{}'", username);

        WebSocketMessage message = new WebSocketMessage(UserSession.getInstance().getUserId(), chatId, content, UserSession.getInstance().getUsername());
        logger.info("Sending message sender ID: {}, username: {}, chat ID: {}, content: {}",
                UserSession.getInstance().getUserId(), UserSession.getInstance().getUsername(), chatId, content);
        webSocketClient.sendMessage(message);
    }

    private void sendOnlineStatus(OnlineStatus status) {
        if (!connected) {
            logger.warn("Cannot send message: not connected");
            return;
        }

        OnlineStatusMessage statusMessage = new OnlineStatusMessage(status, UserSession.getInstance().getUserId(), UserSession.getInstance().getUsername());
        webSocketClient.sendStatus(statusMessage);
    }


    // ========== WebsocketClient.MessageHandler INTERFACE IMPLEMENTATION ==========

    @Override
    public void onMessage(WebSocketMessage message) {

        if (messageListener != null) {
            logger.info("Received message from username: {}", message.getUsername());
            messageListener.onMessageReceived(message.getChatID(), message.getSenderID(), message.getUsername(), message.getContent());
        }

    }

    @Override
    public void onStatusUpdate(OnlineStatusMessage status) {
        logger.debug("Received status update from {}: {}", status.getSenderID(), status.getStatus());

        if (messageListener != null) {
            messageListener.onStatusChanged(status.getSenderID(), status.getUsername(), status.getStatus());
        }
    }

    @Override
    public void onConnected() {
        logger.info("WebSocket connection established");
        connected = true;
        if (messageListener != null) {
            messageListener.onConnectionChanged(true);
        }
    }

    @Override
    public void onDisconnected() {
        logger.info("WebSocket connection lost");
        connected = false;
        if (messageListener != null) {
            messageListener.onConnectionChanged(false);
        }
    }

    @Override
    public void onError(String error) {
        logger.error("WebSocket error: {}", error);
        if (messageListener != null) {
            messageListener.onError("Connection error: " + error);
        }
    }
}


