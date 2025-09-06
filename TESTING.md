# TESTING

### Overview

---

This document outlines the testing methodologies and coverage for T-Chat. The project employs different testing approaches for the server and client components based on technical requirements.

### Server Testing

---

The Server module employs unit testing for all components except networking layer, which includes controllers and some WebSocketServer functionality

- **Framework** - JUnit 5 with Spring Boot Test
- **Mocking** - Mockito
- **Database** - H2 database

**Running Server Unit Tests:**

```bash
# Compile the project
mvn clean compile

# Run server tests
mvn test -pl server
```

**Test Coverage:**

Database Tests:

- `ChatDbServiceTests`
- `ContactDbServiceTests`
- `FileDbServiceTests`
- `MessageDbServiceTests`
- `SessionDbServiceTests`
- `UserDbServiceTests`

Service Tests:

- `ChatServiceTests`
- `ContactServiceTests`
- `FileServiceTests`
- `JWTServiceTests`
- `MessageServiceTests`
- `SessionServiceTests`
- `UserServiceTests`

WebSocket Server Tests

- `ChatManagerTests`
- `ChatTests`
- `StatusManagerTests`

EventSystem Tests

- `EventBusTests`

### Client Testing

---

The Client module was manually tested using integration tests. Please find below the test cases that were covered:

**Registration/Login:**

- ☑️ User Registration 
- ☑️ User Login

**Group Chat Operations**

- ☑️ Displaying chats 
- ☑️ Group chat creation 
- ☑️ Group chat deletion by creator 
- ☑️ Group chat deletion by non-creator 
- ☑️ Entering group chat 
- ☑️ Exiting group chat 
- ☑️ Adding member to group chat 
- ☑️ Removing member from group chat

**Private Chat Operations**

- ☑️ Private chat creation
- ☑️ Private chat deletion
- ☑️ Entering private chat
- ☑️ Exiting private chat

**Message Operations**

- ☑️ Message history loading
- ☑️ Sending message in private chat
- ☑️ Sending message in group chat
- ☑️ Message reception in private chat
- ☑️ Message reception in group chat

**Contact Operations**

- ☑️ Contact creation
- ☑️ Contact deletion
- ☑️  Display contacts list

**File Operations**

- ☑️ Upload file
- ☑️ Download file
- ☑️ Delete file
- ☑️ List chat files

**Personal Info Management**

- ☑️ Update password
- ☑️ Update username
- ☑️ Update email

**Utilities**

- ☑️ Help command
- ☑️ Exit chat application
- ☑️ Clear screen