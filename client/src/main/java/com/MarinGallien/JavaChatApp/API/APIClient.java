package com.MarinGallien.JavaChatApp.API;

import ch.qos.logback.classic.Level;
import com.MarinGallien.JavaChatApp.DTOs.GetterMessages.GetterRequests.*;
import com.MarinGallien.JavaChatApp.DTOs.GetterMessages.GetterResponses.*;
import com.MarinGallien.JavaChatApp.DTOs.HTTPMessages.HTTPRequests.*;
import com.MarinGallien.JavaChatApp.DTOs.HTTPMessages.HTTPResponses.*;


import com.MarinGallien.JavaChatApp.UserSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class APIClient {
    private static final Logger logger = LoggerFactory.getLogger(APIClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private String jwtToken;

    public APIClient() {
        ((ch.qos.logback.classic.Logger) logger).setLevel(Level.OFF);
        this.baseUrl = UserSession.getInstance().getHttpBaseUrl();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
        this.objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    // ========== AUTHENTICATION METHODS ==========

    // Authenticate user with email and password
    public LoginResponse login(LoginRequest request) {
        try {
            String jsonBody = objectMapper.writeValueAsString(request);

            // Create HTTP request to login endpoint
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            // Send request and get response
            HttpResponse<String> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            // Deserialize response from JSON
            LoginResponse loginResponse = objectMapper.readValue(response.body(), LoginResponse.class);

            // Init userSession
            if (loginResponse.success()) {
                UserSession.getInstance().initUserSession(loginResponse.userId(), loginResponse.JwtToken());
            }

            return loginResponse;

        } catch (Exception e) {
            logger.error("Login failed: {}", e.getMessage());
            return new LoginResponse(false, "Login failed: " + e.getMessage(), null, null,null);
        }
    }

    // Register a new user account
    public GenericResponse register(RegisterRequest request) {
        try {
            String jsonBody = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/auth/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            // Send request and get response
            HttpResponse<String> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            // Deserialize and return response
            return objectMapper.readValue(response.body(), GenericResponse.class);

        } catch (Exception e) {
            logger.error("Registration failed: {}", e.getMessage());
            return new GenericResponse(false, "Registration failed: " + e.getMessage());
        }
    }

    // ========== CHAT METHODS ==========

    public GenericResponse createPrivateChat(CreatePcRequest request) {
        try {
            return sendAuthenticatedRequest("/api/chat/private", request, GenericResponse.class, "POST");
        } catch (Exception e) {
            logger.error("Failed to create private chat: {}", e.getMessage());
            return new GenericResponse(false, "Failed to create private chat: " + e.getMessage());
        }
    }

    public GenericResponse createGroupChat(CreateGcRequest request) {
        try {
            return sendAuthenticatedRequest("/api/chat/group", request, GenericResponse.class, "POST");
        } catch (Exception e) {
            logger.error("Failed to create group chat: {}", e.getMessage());
            return new GenericResponse(false, "Failed to create group chat: " + e.getMessage());
        }
    }

    public GenericResponse deleteChat(DeleteChatRequest request) {
        try {
            return sendAuthenticatedRequest("/api/chat/delete", request, GenericResponse.class, "POST");
        } catch (Exception e) {
            logger.error("Failed to delete chat: {}", e.getMessage());
            return new GenericResponse(false, "Failed to delete chat: " + e.getMessage());
        }
    }

    public GenericResponse addMemberToChat(AddOrRemoveMemberRequest request) {
        try {
            return sendAuthenticatedRequest("/api/chat/member/add", request, GenericResponse.class, "POST");
        } catch (Exception e) {
            logger.error("Failed to add member to chat: {}", e.getMessage());
            return new GenericResponse(false, "Failed to add member to chat: " + e.getMessage());
        }
    }

    public GenericResponse removeMemberFromChat(AddOrRemoveMemberRequest request) {
        try {
            return sendAuthenticatedRequest("/api/chat/member/remove", request, GenericResponse.class, "POST");
        } catch (Exception e) {
            logger.error("Failed to remove member from chat: {}", e.getMessage());
            return new GenericResponse(false, "Failed to remove member from chat: " + e.getMessage());
        }
    }

    public GetUserChatsResponse getUserChats(GetUserChatsRequest request) {
        try {
            return sendAuthenticatedRequest("/api/chat/chats", request, GetUserChatsResponse.class, "POST");
        } catch (Exception e) {
            logger.error("Failed to get user chats: {}", e.getMessage());
            return new GetUserChatsResponse(false, "Failed to get user chats: " + e.getMessage(), null);
        }
    }

    // ========== CONTACT METHODS ==========

    public GenericResponse createContact(CreateOrRemoveContactRequest request) {
        try {
            return sendAuthenticatedRequest("/api/contact/create", request, GenericResponse.class, "POST");
        } catch (Exception e) {
            logger.error("Failed to create contact: {}", e.getMessage());
            return new GenericResponse(false, "Failed to create contact: " + e.getMessage());
        }
    }

    public GenericResponse removeContact(CreateOrRemoveContactRequest request) {
        try {
            return sendAuthenticatedRequest("/api/contact/delete", request, GenericResponse.class, "POST");
        } catch (Exception e) {
            logger.error("Failed to remove contact: {}", e.getMessage());
            return new GenericResponse(false, "Failed to remove contact: " + e.getMessage());
        }
    }

    public GetUserContactsResponse getUserContacts(GetUserContactsRequest request) {
        try {
            return sendAuthenticatedRequest("/api/contact/contacts", request, GetUserContactsResponse.class, "POST");
        } catch (Exception e) {
            logger.error("Failed to get user contacts: {}", e.getMessage());
            return new GetUserContactsResponse(false, "Failed to get user contacts: " + e.getMessage(), null);
        }
    }

    // ========== MESSAGE METHODS ==========

    public GetChatMessagesResponse getChatMessages(GetChatMessagesRequest request) {
        try {
            return sendAuthenticatedRequest("/api/message/messages", request, GetChatMessagesResponse.class, "POST");
        } catch (Exception e) {
            logger.error("Failed to get chat messages: {}", e.getMessage());
            return new GetChatMessagesResponse(false, "Failed to get chat messages: " + e.getMessage(), null);
        }
    }

    // ========== USER METHODS ==========

    public GenericResponse updateUsername(UpdateUnameRequest request) {
        try {
            return sendAuthenticatedRequest("/api/user/update/username", request, GenericResponse.class, "POST");
        } catch (Exception e) {
            logger.error("Failed to update username: {}", e.getMessage());
            return new GenericResponse(false, "Failed to update username: " + e.getMessage());
        }
    }

    public GenericResponse updateEmail(UpdateEmailRequest request) {
        try {
            return sendAuthenticatedRequest("/api/user/update/email", request, GenericResponse.class, "POST");
        } catch (Exception e) {
            logger.error("Failed to update email: {}", e.getMessage());
            return new GenericResponse(false, "Failed to update email: " + e.getMessage());
        }
    }

    public GenericResponse updatePassword(UpdatePasswdRequest request) {
        try {
            return sendAuthenticatedRequest("/api/user/update/password", request, GenericResponse.class, "POST");
        } catch (Exception e) {
            logger.error("Failed to update password: {}", e.getMessage());
            return new GenericResponse(false, "Failed to update password: " + e.getMessage());
        }
    }

    // ========== FILE METHODS ==========

    public GenericResponse uploadFile(String userId, String chatId, File file) {
        try {
            // Create RestTemplate
            RestTemplate restTemplate = new RestTemplate();

            // Create headers with JWT token
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(jwtToken);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // Create multipart form data
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("userId", userId);
            body.add("chatId", chatId);
            body.add("file", new FileSystemResource(file));

            // Create HTTP entity
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Send request
            ResponseEntity<GenericResponse> response = restTemplate.postForEntity(
                    baseUrl + "/api/file/upload",
                    requestEntity,
                    GenericResponse.class
            );

            return response.getBody();
        } catch (Exception e) {
            logger.error("Failed to upload file: {}", e.getMessage());
            return new GenericResponse(false, "Failed to upload file: " + e.getMessage());
        }
    }

    public byte[] downloadFile(String userId, String chatId, String fileId) {
        try {
            // Create download request
            DownloadFileRequest request = new DownloadFileRequest(userId, chatId, fileId);

            // Send authenticatedRequest
            String jsonBody = objectMapper.writeValueAsString(request);

            System.out.println("DEBUG: Sending download request: " + jsonBody);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/file/download"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwtToken)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

            // Send request and get response as bytes
            HttpResponse<byte[]> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofByteArray());

            System.out.println("DEBUG: Response status: " + response.statusCode());
            System.out.println("DEBUG: Response headers: " + response.headers().map());
            System.out.println("DEBUG: Response body length: " + (response.body() != null ? response.body().length : 0));

            if (response.body() != null && response.body().length > 0) {
                // Check if response looks like JSON (error response)
                String responseStart = new String(response.body(), 0, Math.min(100, response.body().length));
                System.out.println("DEBUG: Response start: " + responseStart);
            }

            // Check if response is successful (200 status code)
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                logger.error("Failed to download file");
                return null;
            }

        } catch (Exception e) {
            logger.error("Failed to download file {}", e.getMessage());
            return null;
        }
    }

    public GetChatFilesResponse getChatFiles(GetChatFilesRequest request) {
        try {
            return sendAuthenticatedRequest("/api/file/files", request, GetChatFilesResponse.class, "POST");
        } catch (Exception e) {
            logger.error("Failed to get chat files: {}", e.getMessage());
            return new GetChatFilesResponse(false, "Failed to get chat files", null);
        }
    }

    public GenericResponse deleteFile(DeleteFileRequest request) {
        try {
            return sendAuthenticatedRequest("/api/file/delete", request, GenericResponse.class, "POST");
        } catch (Exception e) {
            logger.error("Failed to delete file: {}", e.getMessage());
            return new GenericResponse(false, "Failed to delete file: " + e.getMessage());
        }
    }

    // ========== NETWORK GETTER METHODS ==========
    public UserIdResponse getUserIdFromUsername(UserIdRequest request) {
        try {
            return sendAuthenticatedRequest("/api/user/userId", request, UserIdResponse.class, "POST");
        } catch (Exception e) {
            logger.error("Failed to retrieve user ID: {}", e.getMessage());
            return new UserIdResponse(false, null);
        }
    }

    public UserIdsResponse getUserIdsFromUsernames(UserIdsRequest request) {
        try {
            return sendAuthenticatedRequest("/api/user/userIds", request, UserIdsResponse.class, "POST");
        } catch (Exception e) {
            logger.error("Failed to retrieve user IDs: {}", e.getMessage());
            return new UserIdsResponse(false, null);
        }
    }

    public FileIdResponse getFileIdFromFilename(FileIdRequest request) {
        try {
            return sendAuthenticatedRequest("/api/file/fileId", request, FileIdResponse.class, "POST");
        } catch (Exception e) {
            logger.error("Failed to retrieve file ID: {}", e.getMessage());
            return new FileIdResponse(false, null);
        }
    }

    public ChatIdResponse getChatIdFromChatName(ChatIdRequest request) {
        try {
            return sendAuthenticatedRequest("/api/chat/chatId", request, ChatIdResponse.class, "POST");
        } catch (Exception e) {
            logger.error("Failed to retrieve chat ID: {}", e.getMessage());
            return new ChatIdResponse(false, null);
        }
    }

    // ========== UTILITY METHODS ==========

    private <T, R> R sendAuthenticatedRequest(String endpoint, T requestBody, Class<R> responseClass, String method)
            throws IOException, InterruptedException {

        // Check if user is authenticated before making request
        if (jwtToken == null) {
            throw new IllegalStateException("Not authenticated. Please login first.");
        }

        // Serialize request body to JSON
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        // Build basic HTTP request with auth headers
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwtToken);

        // Set HTTP method
        switch (method.toUpperCase()) {
            case "POST":
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
                break;
            case "DELETE":
                requestBuilder.DELETE();
                break;
            case "GET":
            default:
                requestBuilder.GET();
                break;
        }

        // Build and send request
        HttpRequest httpRequest = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(httpRequest,
                HttpResponse.BodyHandlers.ofString());

        // Deserialize response to specified class type
        return objectMapper.readValue(response.body(), responseClass);
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public boolean isAuthenticated() {
        return jwtToken != null && !jwtToken.trim().isEmpty();
    }

    public void logout() {
        this.jwtToken = null;
    }
}