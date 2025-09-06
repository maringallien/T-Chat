package com.MarinGallien.JavaChatApp;

import com.MarinGallien.JavaChatApp.API.APIService;
import com.MarinGallien.JavaChatApp.DTOs.DataEntities.ChatDTO;
import com.MarinGallien.JavaChatApp.DTOs.DataEntities.ContactDTO;
import com.MarinGallien.JavaChatApp.DTOs.DataEntities.FileDTO;
import com.MarinGallien.JavaChatApp.DTOs.DataEntities.MessageDTO;
import com.MarinGallien.JavaChatApp.WebSocket.ChatService;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// ClientManager - handles business logic, not parsing
public class ClientManager {

    // Network services
    private final APIService apiService;
    private final ChatService chatService;

    // UI service
    private final LanternaUI lanternaUI;

    // Local parameters
    private String currentChatId;

    public ClientManager(APIService apiService, ChatService chatService, LanternaUI lanternaUI) {
        this.apiService = apiService;
        this.chatService = chatService;
        this.lanternaUI = lanternaUI;
    }


    // ========== AUTHENTICATION METHODS ==========

    public void handleLogin(String email, String password) {
        try {
            boolean success = apiService.login(email, password);

            if (success) {
                lanternaUI.showInfo(UserSession.getInstance().getUserId());
                lanternaUI.showLoginSuccess(UserSession.getInstance().getUsername());
                chatService.startChat();
            } else {
                lanternaUI.showLoginFailure();
            }
        } catch (Exception e) {
            lanternaUI.showError("Login failed: " + e.getMessage());
        }
    }

    public void handleRegister(String username, String email, String password) {
        try {
            boolean success = apiService.register(username, email, password);

            if (success) {
                UserSession.getInstance().setUsername(username);
                UserSession.getInstance().setEmail(email);

                lanternaUI.showRegistrationSuccess();
            } else {
                lanternaUI.showRegistrationFailure();
            }
        } catch (Exception e) {
            lanternaUI.showError("Registration failed: " + e.getMessage());
        }
    }


    // ========== CHAT MODE UTILITIES FOR CMDPARSER ==========

    public boolean isInChatMode() {
        return lanternaUI.isInChatMode();
    }

    public void sendMessage(String message) {
        if (currentChatId == null) {
            lanternaUI.showError("Not in a chat. Use 'chat <contact>' to start chatting.");
            return;
        }

        if (!chatService.isConnected()) {
            lanternaUI.showError("Not connected to chat server.");
            return;
        }

        try {
            chatService.sendMessage(currentChatId, message);
            lanternaUI.showSentMessage(message);
        } catch (Exception e) {
            lanternaUI.showError("Failed to send message: " + e.getMessage());

        }
    }

    public void exitCurrentChat() {
        if (currentChatId != null) {
            this.currentChatId = null;
            lanternaUI.exitChatMode();
        }
    }

    public void disconnect() {
        try {
            // Stop chat service (sends offline status and disconnects WebSocket)
            if (chatService != null && chatService.isConnected()) {
                chatService.stopChat();
            }

            // Clear JWT token (logout from API)
            if (apiService != null) {
                apiService.logout();
            }

            // Exit any current chat
            if (currentChatId != null) {
                exitCurrentChat();
            }


        } catch (Exception e) {
            lanternaUI.showError("Error during disconnect: " + e.getMessage());
        }
    }



    // ========== CHAT MANAGEMENT METHODS ==========

    // METHOD NEEDS WORK. NEED TO FIND CHAT ID FROM USERNAME OR USER ID
    public void enterPrivateChat(String contactUname) {
        try {
            // Retrieve user ID
            String contactUserId = apiService.getUserIdFromUsername(contactUname);

            if (contactUserId == null || contactUserId.isEmpty()) {
                lanternaUI.showError("Failed to retrieve contact user ID");
                return;
            }

            // Determine private chat ID and chat name (they are predictable)
            String chatId = determineChatId(contactUserId);
            String chatName = determineChatName(UserSession.getInstance().getUsername(), contactUname);

            if (chatId == null) {
                lanternaUI.showChatNotFound(contactUname);
                return;
            }

            this.currentChatId = chatId;

            if (!lanternaUI.enterChatMode(chatName, chatId)) {
                lanternaUI.showError("Failed to enter chat mode");
                return;
            }

            List<MessageDTO> messages = apiService.getChatMessages(chatId);
            if (!messages.isEmpty()) {
                lanternaUI.showMessages(messages);
            }

        } catch (Exception e) {
            lanternaUI.showError("Failed to enter chat: " + e.getMessage());
        }
    }

    private String determineChatId(String userId2) {
        String localUserId = UserSession.getInstance().getUserId();
        String[] sortedIds = {localUserId, userId2};
        Arrays.sort(sortedIds);
        return "PRIVATE_" + sortedIds[0] + "_" + sortedIds[1];
    }

    private String determineChatName(String username1, String username2) {
        String[] sortedUnames = {username1, username2};
        Arrays.sort(sortedUnames);
        return sortedUnames[0] + "-" + sortedUnames[1];
    }

    public void enterGroupChat(String chatName) {
        try {
            // Retrieve chat ID from chat name
            String chatId = apiService.getChatIdFromChatName(chatName);
            if (chatId == null || chatId.isEmpty()) {
                lanternaUI.showError("Failed to retrieve chat ID");
                return;
            }

            this.currentChatId = chatId;
            if (!lanternaUI.enterChatMode(chatName, chatId)) {
                lanternaUI.showError("Failed to enter chat mode");
                return;
            }
            lanternaUI.showMessages(apiService.getChatMessages(chatId));

        } catch (Exception e) {
            lanternaUI.showError("Failed to enter chat: " + e.getMessage());
        }
    }

    public void createPrivateChat(String contactUname) {
        try {
            // Retrieve contact ID from contact username
            String contactId = apiService.getUserIdFromUsername(contactUname);
            if (contactId == null || contactId.isEmpty()) {
                lanternaUI.showError("Failed to retrieve user ID");
                return;
            }

            boolean success = apiService.createPrivateChat(contactId);

            if (success) {
                lanternaUI.showPrivateChatCreated(contactUname);
            } else {
                lanternaUI.showError("Failed to create private chat");
            }
        } catch (Exception e) {
            lanternaUI.showError("Failed to create private chat: " + e.getMessage());
        }
    }

    public void createGroupChat(String chatName, List<String> memberUnames) {
        try {
            // Retrieve member IDs from member usernames
            List<String> memberIds = apiService.getUserIdsFromUsernames(memberUnames);
            if (memberIds == null || memberIds.isEmpty()) {
                lanternaUI.showError("Failed to retrieve user IDs");
                return;
            }

            Set<String> memberSet = new HashSet<>(memberIds);
            boolean success = apiService.createGroupChat(memberSet, chatName);

            if (success) {
                lanternaUI.showGroupChatCreated(chatName);
            } else {
                lanternaUI.showError("Failed to create group chat");
            }
        } catch (Exception e) {
            lanternaUI.showError("Failed to create group chat: " + e.getMessage());
        }
    }

    public void deleteChat(String chatName) {
        try {
            // For private chats, we need to determine chat Id locally, for group chats we can look it up
            // Retrieve chat ID from chat name
            String chatId = apiService.getChatIdFromChatName(chatName);
            if (chatId == null || chatId.isEmpty()) {
                lanternaUI.showError("Failed to retrieve chat ID");
                return;
            }

            boolean success = apiService.deleteChat(chatId);

            if (success) {
                lanternaUI.showSuccess("Chat deleted successfully");

                // Exit chat mode if we're currently in this chat
                if (chatId.equals(currentChatId)) {
                    exitCurrentChat();
                }
            } else {
                lanternaUI.showError("Failed to delete chat");
            }
        } catch (Exception e) {
            lanternaUI.showError("Failed to delete chat: " + e.getMessage());
        }
    }

    public void addMemberToChat(String chatName, String username) {
        try {
            // Retrieve chat and member IDs from chat name and username
            String chatId = apiService.getChatIdFromChatName(chatName);
            String memberId = apiService.getUserIdFromUsername(username);
            if (memberId == null || chatId == null || memberId.isEmpty() || chatId.isEmpty()) {
                lanternaUI.showError("Failed to retrieve chat and/or user ID");
                return;
            }

            boolean success = apiService.addMemberToChat(memberId, chatId);

            if (success) {
                lanternaUI.showSuccess("Member added to chat successfully");
            } else {
                lanternaUI.showError("Failed to add member");
            }
        } catch (Exception e) {
            lanternaUI.showError("Failed to add member: " + e.getMessage());
        }
    }

    public void removeMemberFromChat(String chatName, String username) {
        try {
            // Retrieve chat and member IDs from chat name and username
            String chatId = apiService.getChatIdFromChatName(chatName);
            String memberId = apiService.getUserIdFromUsername(username);
            if (memberId == null || chatId == null || memberId.isEmpty() || chatId.isEmpty()) {
                lanternaUI.showError("Failed to retrieve chat and/or user ID");
                return;
            }

            boolean success = apiService.removeMemberFromChat(memberId, chatId);

            if (success) {
                lanternaUI.showSuccess("Member removed from chat successfully");
            } else {
                lanternaUI.showError("Failed to remove member");
            }
        } catch (Exception e) {
            lanternaUI.showError("Failed to remove member: " + e.getMessage());
        }
    }

    public void getUserChats() {
        try {
            List<ChatDTO> chats = apiService.getUserChats();

            // Check for null or empty
            if (chats == null || chats.isEmpty()) {
                lanternaUI.showError("No chats were found");
                return;
            }

            // Display chats
            lanternaUI.showChats(chats);
        } catch (Exception e) {
            lanternaUI.showError("Failed to get chats: " + e.getMessage());
        }
    }


    // ========== CONTACT MANAGEMENT METHODS ==========

    public void addContact(String contactUname) {
        try {
            // Retrieve contact ID from contact username
            String contactId = apiService.getUserIdFromUsername(contactUname);
            if (contactId == null || contactId.isEmpty()) {
                lanternaUI.showError("Failed to retrieve contact ID");
                return;
            }

            boolean success = apiService.createContact(contactId);

            if (success) {
                lanternaUI.showContactAdded(contactId);
            } else {
                lanternaUI.showError("Failed to add contact");
            }
        } catch (Exception e) {
            lanternaUI.showError("Failed to add contact: " + e.getMessage());
        }
    }

    public void removeContact(String contactUname) {
        try {
            // Retrieve contact ID from contact username
            String contactId = apiService.getUserIdFromUsername(contactUname);
            if (contactId == null || contactId.isEmpty()) {
                lanternaUI.showError("Failed to retrieve contact ID");
            }

            boolean success = apiService.removeContact(contactId);

            if (success) {
                lanternaUI.showContactRemoved(contactId);
            } else {
                lanternaUI.showError("Failed to remove contact");
            }
        } catch (Exception e) {
            lanternaUI.showError("Failed to remove contact: " + e.getMessage());
        }
    }

    public void getUserContacts() {
        try {
            List<ContactDTO> contacts = apiService.getUserContacts();

            if (contacts != null && !contacts.isEmpty()) {
                lanternaUI.showContacts(contacts);
            } else {
                lanternaUI.showError("No contacts found");
            }
        } catch (Exception e) {
            lanternaUI.showError("Failed to get contacts: " + e.getMessage());
        }
    }


    // ========== MESSAGE METHODS ==========

    public void getChatMessages(String chatName) {
        try {
            // Retrieve chat ID from chat name
            String chatId = apiService.getChatIdFromChatName(chatName);
            if (chatName == null || chatName.isEmpty()) {
                lanternaUI.showError("Failed to retrieve chat ID");
                return;
            }

            List<MessageDTO> chatMessages = apiService.getChatMessages(chatId);

            if (chatMessages != null && !chatMessages.isEmpty()) {
                lanternaUI.showMessages(chatMessages);
            } else {
                lanternaUI.showError("Failed to get messages");
            }
        } catch (Exception e) {
            lanternaUI.showError("Failed to get messages: " + e.getMessage());
        }
    }


    // ========== USER UPDATE METHODS ==========

    public void updateUsername(String newUsername) {
        try {
            boolean success = apiService.updateUsername(newUsername);

            if (success) {
                lanternaUI.showSuccess("Username updated successfully");
            } else {
                lanternaUI.showError("Failed to update username");
            }
        } catch (Exception e) {
            lanternaUI.showError("Failed to update username: " + e.getMessage());
        }
    }

    public void updateEmail(String newEmail) {
        try {
            boolean success = apiService.updateEmail(newEmail);

            if (success) {
                lanternaUI.showSuccess("Email updated successfully");
            } else {
                lanternaUI.showError("Failed to update email");
            }
        } catch (Exception e) {
            lanternaUI.showError("Failed to update email: " + e.getMessage());
        }
    }

    public void updatePassword(String oldPassword, String newPassword) {
        try {
            boolean success = apiService.updatePassword(oldPassword, newPassword);

            if (success) {
                lanternaUI.showSuccess("Password updated successfully");
            } else {
                lanternaUI.showError("Failed to update password");
            }
        } catch (Exception e) {
            lanternaUI.showError("Failed to update password: " + e.getMessage());
        }
    }


    // ========== FILE METHODS ==========

    public void uploadFile(String chatName, String filepath) {
        try {
            if (filepath == null || filepath.isEmpty()) {
                lanternaUI.showError("Failed to upload file: filepath is null or empty");
                return;
            }

            // Create file object from filepath
            File file = new File(filepath);

            // Make sure file exists
            if (!file.exists() || !file.isFile()) {
                lanternaUI.showError("Failed to upload file: file does not exist or is not a file");
            }

            String chatId = apiService.getChatIdFromChatName(chatName);

            if (chatId == null || chatId.isEmpty()) {
                lanternaUI.showError("Failed to upload file: chat does not exist");
                return;
            }

            boolean success = apiService.uploadFile(chatId, file);
            if (success) {
                lanternaUI.showSuccess("File uploaded successfully");
            } else {
                lanternaUI.showError("Failed to upload file");
            }

        } catch (Exception e) {
            lanternaUI.showError("Failed to upload file: " + e.getMessage());
        }
    }

    public void downloadFile(String chatName, String filename, String filePath) {
        try {
            if (filename == null || filePath == null || filename.isEmpty() || filePath.isEmpty()) {
                lanternaUI.showError("Failed to download file: Filename or file path empty/null");
                return;
            }

            // Retrieve chatId
            String chatId = apiService.getChatIdFromChatName(chatName);
            if (chatId == null || chatId.isEmpty()) {
                lanternaUI.showError("Failed to download file: chat ID not found");
                return;
            }

            // Retrieve file ID
            String fileId = apiService.getFileIdFromFilename(filename, chatId);
            if (fileId == null || fileId.isEmpty()) {
                lanternaUI.showError("Failed to download file: no corresponding file ID");
                return;
            }

            String completeFilePath = filePath + "/" + filename;
            boolean success = apiService.downloadFile(chatId, fileId, completeFilePath);
            if (success) {
                lanternaUI.showSuccess("File downloaded successfully");
            } else {
                lanternaUI.showError("Failed to download file, api service returned false");
            }

        } catch (Exception e) {
            lanternaUI.showError("Failed to download file: " + e.getMessage());
        }
    }

    public void getChatFiles(String chatName) {
        try {
            if (chatName == null || chatName.isEmpty()) {
                lanternaUI.showError("Failed to retrieve chat files: chat name is null or empty");
                return;
            }

            String chatId = apiService.getChatIdFromChatName(chatName);
            if (chatId == null || chatId.isEmpty()) {
                lanternaUI.showError("Failed to retrieve chat files: chat does not exist");
                return;
            }

            List<FileDTO> chatFiles = apiService.getChatFiles(chatId);

            if (chatFiles == null || chatFiles.isEmpty()) {
                lanternaUI.showInfo("No files in this chat");
                return;
            }

            lanternaUI.showFiles(chatFiles);

        } catch (Exception e) {
            lanternaUI.showError("Failed to retrieve chat files: " + e.getMessage());
        }
    }

    public void deleteFile(String chatName, String filename) {
        try {
            String chatId = apiService.getChatIdFromChatName(chatName);
            if (chatId == null || chatId.isEmpty()) {
                lanternaUI.showError("Failed to delete file: chat ID is null or empty");
                return;
            }

            String fileId = apiService.getFileIdFromFilename(filename, chatId);
            if (fileId == null || fileId.isEmpty()) {
                lanternaUI.showError("Failed to delete file: file ID is null or empty");
                return;
            }

            boolean success = apiService.deleteFile(chatId, fileId);

            if (!success) {
                lanternaUI.showError("Failed to delete file");
                return;
            }

            lanternaUI.showError("Successfully delete file");

        } catch (Exception e) {
            lanternaUI.showError("Failed to delete file: " + e.getMessage());
        }
    }

    // ========== UTILITY METHODS ==========

    public void showHelp() {
        lanternaUI.showHelp();
    }

    public void displayError(String error) {
        lanternaUI.showError(error);
    }

    public void clearScreen() { lanternaUI.clearScreen(); }

}

