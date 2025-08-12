package com.MarinGallien.JavaChatApp.DTOs.HTTPMessages;

import jakarta.validation.constraints.*;

import java.util.Set;

// Consolidated file containing all HTTPMessages Response records.
// Organized by category using nested static classes.

public class HTTPRequests {

    // ========== Authentication-related requests ==========
    public static record LoginRequest(
            @NotBlank(message = "Email cannot be blank")
            String email,

            @NotBlank(message = "Password cannot be blank")
            String password
    ) implements ApiReqResInterface {}

    public static record RegisterRequest(
            @NotBlank(message = "Username cannot be blank")
            String username,

            @NotBlank(message = "Email cannot be blank")
            String email,

            @NotBlank(message = "Password cannot be blank")
            String password
    ) implements ApiReqResInterface {}


    // ========== Chat-related requests ==========
    public static record CreatePcRequest(
            @NotBlank(message = "First user ID is required")
            String userId1,

            @NotBlank(message = "Second user ID is required")
            String userId2
    ) implements ApiReqResInterface {}

    public static record CreateGcRequest(
            @NotBlank(message = "Creator ID is required")
            String creatorId,

            @NotEmpty(message = "Member list cannot be empty")
            @Size(min = 1, max = 50, message = "Group chat must have 1-50 members")
            Set<String> memberIds,

            @NotBlank(message = "Chat name is required")
            @Size(max = 100, message = "Chat name cannot exceed 100 characters")
            String chatName
    ) implements ApiReqResInterface {}

    public static record DeleteChatRequest(
            @NotBlank(message = "Creator ID is required")
            String creatorId,

            @NotBlank(message = "Chat ID is required")
            String chatId
    ) implements ApiReqResInterface {}

    public static record AddOrRemoveMemberRequest(
            @NotBlank(message = "Creator ID is required")
            String creatorId,

            @NotBlank(message = "User ID is required")
            String memberId,

            @NotBlank(message = "Chat ID is required")
            String chatId
    ) implements ApiReqResInterface {}

    public static record GetUserChatsRequest(
            @NotBlank(message = "User ID is required")
            String userId
    ) implements ApiReqResInterface {}


    // ========== Contact-related requests ==========
    public static record CreateOrRemoveContactRequest(
            @NotBlank(message = "User ID cannot be blank")
            String userId,

            @NotBlank(message = "Contact ID cannot be blank")
            String contactId
    ) implements ApiReqResInterface {}

    public static record GetUserContactsRequest(
            @NotBlank(message = "User ID cannot be blank")
            String userId
    ) implements ApiReqResInterface {}


        // ========== File-related requests ==========
    public static record GetChatFilesRequest(
            @NotBlank(message = "User ID is required")
            String userId,

            @NotBlank(message = "Chat ID is required")
            String chatId
    ) implements ApiReqResInterface {}

    public static record DownloadFileRequest(
            @NotBlank(message = "User ID is required")
            String userId,

            @NotBlank(message = "Chat ID is required")
            String chatId,

            @NotBlank(message = "file ID is required")
            String fileId
    ) implements ApiReqResInterface {}

    public static record DeleteFileRequest(
            @NotBlank(message = "User ID is required")
            String userId,

            @NotBlank(message = "Chat ID is required")
            String chatId,

            @NotBlank(message = "file ID is required")
            String fileId
    ) implements ApiReqResInterface {}


    // ========== Message-related requests ==========
    public static record GetChatMessagesRequest(
            @NotBlank(message = "User ID cannot be blank")
            String userId,

            @NotBlank(message = "Chat ID cannot be blank")
            String chatId
    ) implements ApiReqResInterface {}


    // ========== User-related requests ==========
    public static record UpdateUnameRequest(
            @NotBlank(message = "User ID cannot be blank")
            String userId,

            @NotBlank(message = "Username cannot be blank")
            String username
    ) implements ApiReqResInterface {}

    public static record UpdateEmailRequest(
            @NotBlank(message = "User ID cannot be blank")
            String userId,

            @NotBlank(message = "Email cannot be blank")
            String email
    ) implements ApiReqResInterface {}

    public static record UpdatePasswdRequest(
            @NotBlank(message = "User ID cannot be blank")
            String userId,

            @NotBlank(message = "Old password cannot be blank")
            String oldPassword,

            @NotBlank(message = "New password cannot be blank")
            String newPassword
    ) implements ApiReqResInterface {}

}