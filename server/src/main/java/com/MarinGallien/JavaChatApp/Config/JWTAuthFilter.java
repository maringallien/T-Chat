package com.MarinGallien.JavaChatApp.Config;

import com.MarinGallien.JavaChatApp.Database.JPAEntities.User;
import com.MarinGallien.JavaChatApp.Database.JPARepositories.UserRepo;
import com.MarinGallien.JavaChatApp.Services.AuthService.JWTService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JWTAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JWTAuthFilter.class);

    private JWTService jwtService;
    private UserRepo userRepo;

    public JWTAuthFilter(JWTService jwtService, UserRepo userRepo) {
        this.jwtService = jwtService;
        this.userRepo = userRepo;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/api")) {
            logger.info("DEBUG: Processing API request to: {}", path);
            logger.info("DEBUG: Request method: {}", request.getMethod());
        }

        // Extract token from authorization header
        String authHeader = request.getHeader("Authorization");
        logger.info("DEBUG: Authorization header: {}", authHeader != null ? "Present" : "Missing");

        String jwt = jwtService.extractTokenFromHeader(authHeader);
        logger.info("DEBUG: Extracted JWT: {}", jwt != null ? "Present" : "Missing");

        // If no token present, continue without authentication
        if (jwt == null) {
            logger.warn("DEBUG: No JWT token found, continuing without authentication");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Validate token
            if (!jwtService.validateToken(jwt)) {
                logger.warn("Invalid JWT token in request");
                filterChain.doFilter(request, response);
                return;
            }

            // Extract user information from token
            String userId = jwtService.extractUserId(jwt);
            logger.info("DEBUG: Extracted userId from JWT: {}", userId);

            // Check if user is already authenticated in this request
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Load user from database
                User user = userRepo.findUserById(userId);
                logger.info("DEBUG: User found in database: {}", user != null);

                if (user != null) {
                    // Create authentication token
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user.getUserId(),
                            null,
                            new ArrayList<>()
                    );
                    logger.info("DEBUG: Authentication token created successfully");

                    // Set additional details
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    logger.info("DEBUG: Authentication details set successfully");

                    // Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.info("DEBUG: Authentication set in security context successfully");

                    logger.debug("Successfully authenticated user: {}", userId);
                } else {
                    logger.warn("User not found in database for userId: {}", userId);
                }
            }

        } catch (Exception e) {
            logger.error("Error processing JWT token: {}", e.getMessage());
            // Continue without authentication
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // Skip JWT filter for authentication endpoints and public endpoints
        return path.equals("/auth/login") ||
                path.equals("/auth/register") ||
                path.equals("/h2-console") ||
                path.equals("/public") ||
                path.startsWith("/ws");
    }

}
