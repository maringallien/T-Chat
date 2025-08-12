package com.MarinGallien.JavaChatApp;

import com.MarinGallien.JavaChatApp.DTOs.DataEntities.ChatDTO;
import com.MarinGallien.JavaChatApp.DTOs.DataEntities.ContactDTO;
import com.MarinGallien.JavaChatApp.DTOs.DataEntities.FileDTO;
import com.MarinGallien.JavaChatApp.DTOs.DataEntities.MessageDTO;
import com.MarinGallien.JavaChatApp.Enums.OnlineStatus;
import com.MarinGallien.JavaChatApp.WebSocket.ChatService;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LanternaUI implements ChatService.MessageListener {

    private Terminal terminal;
    private Screen screen;
    private TextGraphics textGraphics;

    // UI State
    private boolean inChatMode = false;
    private String currentChatId;
    private String currentChatName;
    private final Queue<String> pendingOutput = new ConcurrentLinkedQueue<>();
    private StringBuilder currentInput = new StringBuilder();
    private int cursorPosition = 0;
    private int scrollOffset = 0;
    private final List<String> outputHistory = new ArrayList<>();

    // Colors - keeping it simple
    private static final TextColor BG_COLOR = TextColor.ANSI.DEFAULT;
    private static final TextColor FG_COLOR = TextColor.ANSI.DEFAULT;
    private static final TextColor ERROR_COLOR = TextColor.ANSI.RED;
    private static final TextColor SUCCESS_COLOR = TextColor.ANSI.GREEN;
    private static final TextColor INFO_COLOR = TextColor.ANSI.YELLOW;
    private static final TextColor CYAN_COLOR = TextColor.ANSI.CYAN;

    public LanternaUI() throws IOException {
        // Constructor remains lightweight
    }

    public void initialize() throws IOException {
        DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory();

        terminal = terminalFactory.createTerminal();
        screen = new TerminalScreen(terminal);
        textGraphics = screen.newTextGraphics();

        screen.startScreen();
        screen.setCursorPosition(null);

        // Set default colors
        textGraphics.setBackgroundColor(BG_COLOR);
        textGraphics.setForegroundColor(FG_COLOR);
    }

    public void cleanup() {
        try {
            if (screen != null) {
                screen.stopScreen();
            }
            if (terminal != null) {
                terminal.close();
            }
        } catch (IOException e) {
            // Silently fail on cleanup
        }
    }

    // ========== OUTPUT METHODS ==========

    private void addToOutput(String text) {
        outputHistory.add(text);
        // Keep history manageable
        if (outputHistory.size() > 1000) {
            outputHistory.remove(0);
        }
    }

    private void displayOutput(String text) {
        addToOutput(text);
        pendingOutput.offer(text);
    }

    private void displayOutput(String text, TextColor color) {
        addToOutput(text);
        pendingOutput.offer(text);
    }

    private void refreshDisplay() throws IOException {
        screen.clear();

        screen.doResizeIfNecessary();

        TerminalSize size = screen.getTerminalSize();

        // Calculate display area (leave room for input line)
        int maxDisplayLines = size.getRows() - 2;

        // Display output history
        int startIdx = Math.max(0, outputHistory.size() - maxDisplayLines - scrollOffset);
        int endIdx = Math.min(outputHistory.size(), startIdx + maxDisplayLines);

        int row = 0;
        for (int i = startIdx; i < endIdx && row < maxDisplayLines; i++) {
            String line = outputHistory.get(i);

            // Simple color detection based on content
            TextColor color = FG_COLOR;
            if (line.startsWith("❌") || line.contains("failed") || line.contains("error")) {
                color = ERROR_COLOR;
            } else if (line.startsWith("✓") || line.contains("successful")) {
                color = SUCCESS_COLOR;
            } else if (line.startsWith("ℹ️") || line.startsWith("===")) {
                color = CYAN_COLOR;
            } else if (line.startsWith("📱") || line.startsWith("🔗")) {
                color = INFO_COLOR;
            }

            textGraphics.setForegroundColor(color);

            // Handle long lines by wrapping
            if (line.length() > size.getColumns()) {
                String wrapped = line.substring(0, size.getColumns() - 1);
                textGraphics.putString(0, row++, wrapped);
            } else {
                textGraphics.putString(0, row++, line);
            }
        }

        // Draw input line at bottom
        textGraphics.setForegroundColor(FG_COLOR);
        String prompt = getPrompt();
        int inputRow = size.getRows() - 1;
        textGraphics.putString(0, inputRow, prompt + currentInput.toString() + " ");

        // Position cursor
        screen.setCursorPosition(new TerminalPosition(
                prompt.length() + cursorPosition,
                inputRow
        ));

        screen.refresh();
    }

    private String getPrompt() {
        if (inChatMode && currentChatName != null) {
            return "[" + currentChatName + "] > ";
        }
        return "> ";
    }

    public void clearScreen() {
        // Clear the history but keep welcome message
        outputHistory.clear();
        scrollOffset = 0;

        // Re-add the welcome message
        displayOutput("=== Java Chat Client ===");
        displayOutput("Type 'help' for commands");
        displayOutput("========================");

        try {
            refreshDisplay();
        } catch (IOException e) {
            // Ignore display errors
        }
    }

    // ========== INPUT HANDLING ==========

    public String readCommandInput() throws IOException {
        return readInput();
    }

    public String readChatInput() throws IOException {
        return readInput();
    }

    private String readInput() throws IOException {
        currentInput.setLength(0);
        cursorPosition = 0;
        scrollOffset = 0;

        // Process any pending output
        while (!pendingOutput.isEmpty()) {
            pendingOutput.poll();
        }

        refreshDisplay();

        while (true) {
            KeyStroke keyStroke = screen.readInput();

            if (keyStroke.getKeyType() == KeyType.Enter) {
                String result = currentInput.toString();
                currentInput.setLength(0);
                cursorPosition = 0;

                // Add the input to history with prompt
                if (!inChatMode) {
                    addToOutput(getPrompt() + result);
                }
                return result;

            } else if (keyStroke.getKeyType() == KeyType.Escape) {
                return "exit";

            } else if (keyStroke.getKeyType() == KeyType.Backspace) {
                if (cursorPosition > 0) {
                    currentInput.deleteCharAt(cursorPosition - 1);
                    cursorPosition--;
                }

            } else if (keyStroke.getKeyType() == KeyType.ArrowLeft) {
                if (cursorPosition > 0) {
                    cursorPosition--;
                }

            } else if (keyStroke.getKeyType() == KeyType.ArrowRight) {
                if (cursorPosition < currentInput.length()) {
                    cursorPosition++;
                }

            } else if (keyStroke.getKeyType() == KeyType.ArrowUp) {
                // Scroll up through history
                if (scrollOffset < outputHistory.size() - 1) {
                    scrollOffset++;
                }

            } else if (keyStroke.getKeyType() == KeyType.ArrowDown) {
                // Scroll down through history
                if (scrollOffset > 0) {
                    scrollOffset--;
                }

            } else if (keyStroke.getKeyType() == KeyType.Character) {
                char ch = keyStroke.getCharacter();
                currentInput.insert(cursorPosition, ch);
                cursorPosition++;
            }

            refreshDisplay();
        }
    }

    // ========== CHAT MODE DISPLAY MANAGEMENT ==========

    public boolean enterChatMode(String chatName, String chatId) {
        try {
            this.inChatMode = true;
            this.currentChatName = chatName;
            this.currentChatId = chatId;
            displayOutput("=== Entered chat: " + chatName + " ===");
            displayOutput("Type messages to send. Type '/exit' to leave.");

            // Return true to indicate successful entry into chat mode
            return true;
        } catch (Exception e) {
            // If something goes wrong, reset the state
            this.inChatMode = false;
            this.currentChatName = null;
            this.currentChatId = null;
            displayOutput("Failed to enter chat mode: " + e.getMessage());
            return false;
        }
    }

    public void exitChatMode() {
        this.inChatMode = false;
        this.currentChatName = null;
        displayOutput("=== Exited chat ===");
    }

    public void showSentMessage(String message) {
        displayOutput("[You]: " + message);
    }

    // ========== ChatService.MessageListener IMPLEMENTATION ==========

    @Override
    public void onMessageReceived(String chatId, String senderId, String username, String message) {
        String currentUserId = UserSession.getInstance().getUserId();
        if (currentUserId != null && currentUserId.equals(senderId)) {
            return;
        }

        // If we're in the same chat, show the message
        if (inChatMode && currentChatId != null && currentChatId.equals(chatId)) {
            displayOutput("[" + username + "]: " + message);
        }

        try {
            if (inChatMode) {
                refreshDisplay();
            }
        } catch (IOException e) {
            // Ignore display errors
        }
    }

    @Override
    public void onStatusChanged(String userId, String username, OnlineStatus status) {
        String statusText = status == OnlineStatus.ONLINE ? "came online" : "went offline";
        displayOutput((status == OnlineStatus.ONLINE ? "● " : "○ ") + username + " " + statusText);
    }

    @Override
    public void onConnectionChanged(boolean connected) {
        String status = connected ? "Connected to" : "Disconnected from";
        displayOutput((connected ? "✓ " : "❌ ") + status + " chat server");
    }

    @Override
    public void onError(String error) {
        displayOutput("❌ " + error);
    }

    // ========== DISPLAY METHODS FOR API DATA ==========

    public void showContacts(List<ContactDTO> contacts) {
        displayOutput("=== Your Contacts ===");
        if (contacts.isEmpty()) {
            displayOutput("No contacts found.");
        } else {
            for (ContactDTO contact : contacts) {
                String indicator = contact.getOnlineStatus() == OnlineStatus.ONLINE ? "● " : "○ ";
                displayOutput(indicator + " " + contact.getUsername());
            }
        }
    }

    public void showChats(List<ChatDTO> chats) {
        displayOutput("=== Your Chats ===");
        for (ChatDTO chat : chats) {
            String chatName = chat.getChatName();
            displayOutput("- " + chatName + " (" + chat.getChatType() + ")");
        }
    }

    public void showMessages(List<MessageDTO> messages) {
        displayOutput("=== Chat Messages ===");
        for (MessageDTO message : messages) {
            displayOutput("[" + message.getSenderUname() + "]: " + message.getContent());
        }
    }

    public void showFiles(List<FileDTO> files) {
        displayOutput("=== Chat Files ===");
        for (FileDTO file : files) {
            displayOutput(file.getFilename());
        }
    }

    public void showLoginSuccess(String username) {
        displayOutput("✓ Login successful! Welcome " + username);
    }

    public void showLoginFailure() {
        displayOutput("❌ Login failed. Please check your credentials.");
    }

    public void showRegistrationSuccess() {
        displayOutput("✓ Registration successful! You can now login.");
    }

    public void showRegistrationFailure() {
        displayOutput("❌ Registration failed. Please try again.");
    }

    public void showChatNotFound(String chatName) {
        displayOutput("❌ Chat '" + chatName + "' not found.");
    }

    public void showContactAdded(String contactName) {
        displayOutput("✓ Added " + contactName + " to your contacts.");
    }

    public void showContactRemoved(String contactName) {
        displayOutput("✓ Removed " + contactName + " from your contacts.");
    }

    public void showPrivateChatCreated(String contactName) {
        displayOutput("✓ Created private chat with " + contactName);
    }

    public void showGroupChatCreated(String chatName) {
        displayOutput("✓ Created group chat: " + chatName);
    }

    public void showError(String error) {
        displayOutput("❌ " + error);
    }

    public void showSuccess(String message) {
        displayOutput("✓ " + message);
    }

    public void showInfo(String message) {
        displayOutput("ℹ️  " + message);
    }

    public void showNotLoggedIn() {
        displayOutput("❌ Please login first.");
    }

    public void showWelcome() {
        displayOutput("=== Java Chat Client ===");
        displayOutput("Type 'help' for commands");
        displayOutput("========================");
    }

    public void showHelp() {
        displayOutput("=== Available commands ===");
        displayOutput("");

        displayOutput("=== Authentication ===");
        displayOutput("  login <email> <password>                    - Login to your account");
        displayOutput("  register <username> <email> <password>      - Create new account");
        displayOutput("");

        displayOutput("=== Chat Management ===");
        displayOutput("  chat -g <group_name>                        - Join group chat");
        displayOutput("  chat -p <contact_name>                      - Start private chat");
        displayOutput("  create-pc <contact_username>                - Create private chat");
        displayOutput("  create-gc <chat_name> <contact1> <contact2> - Create group chat with contacts");
        displayOutput("  delete-chat <chat_name>                     - Delete a chat");
        displayOutput("  chats                                       - Show your chats");
        displayOutput("  messages <chat_name>                        - Show chat messages");
        displayOutput("");

        displayOutput("=== Group Chat Management ===");
        displayOutput("  add-member <chat_name> <contact_username>   - Add member to group chat");
        displayOutput("  remove-member <chat_name> <contact_uname>   - Remove member from group chat");
        displayOutput("");

        displayOutput("=== Contact Management ===");
        displayOutput("  add-contact <contact_username>              - Add contact by username");
        displayOutput("  remove-contact <contact_username>           - Remove contact by username");
        displayOutput("  contacts                                    - Show your contacts");
        displayOutput("");

        displayOutput("=== File Management ===");
        displayOutput("  upload-file <chatname> <filepath>           - Upload file to chat");
        displayOutput("  download-file <chatname> <filename> <path>  - Download file from chat");
        displayOutput("  get-files <chatname>                        - List files in chat");
        displayOutput("  delete-file <chatname> <filename>           - List files in chat");
        displayOutput("");

        displayOutput("=== Account Settings ===");
        displayOutput("  update-username <new_username>              - Update your username");
        displayOutput("  update-email <new_email>                    - Update your email");
        displayOutput("  update-password <old_passwd> <new_passwd>   - Update your password");
        displayOutput("");

        displayOutput("=== Utility ===");
        displayOutput("  help                                        - Show this help");
        displayOutput("  exit                                        - Exit application");
        displayOutput("  clear                                       - clear screen");
        displayOutput("");

        displayOutput("=== In Chat Mode ===");
        displayOutput("  Type messages, press enter to send ");
        displayOutput("  /exit                                       - Leave current chat");
    }

    public void showGoodbye() {
        displayOutput("Goodbye!");
    }

    // ========== GETTERS ==========

    public boolean isInChatMode() {
        return inChatMode;
    }

    public String getCurrentChatName() {
        return currentChatName;
    }
}
