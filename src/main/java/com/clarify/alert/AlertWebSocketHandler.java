package com.clarify.alert;

import com.clarify.security.JwtService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Budget-alert socket at /ws.
 * <ol>
 *   <li>client connects</li>
 *   <li>client sends {@code {"type":"auth","token":"<JWT>"}} within 10 s</li>
 *   <li>server replies {@code {"type":"AUTH_OK"}}, then pushes {@link BudgetAlert}s for that user only</li>
 * </ol>
 * The token travels in the first message rather than the URL so it stays out
 * of access logs. Any auth failure closes the socket with code 4401.
 */
@Component
public class AlertWebSocketHandler extends TextWebSocketHandler implements DisposableBean {

    static final CloseStatus UNAUTHORIZED = new CloseStatus(4401, "Unauthorized");
    static final String AUTH_OK = "{\"type\":\"AUTH_OK\"}";

    private static final Duration AUTH_TIMEOUT = Duration.ofSeconds(10);
    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int SEND_BUFFER_LIMIT_BYTES = 64 * 1024;

    /** Authenticated sockets by session id. */
    private final ConcurrentMap<String, Authenticated> authenticated = new ConcurrentHashMap<>();

    private final JwtService jwtService;
    private final AlertSessionRegistry registry;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService authTimeouts = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().name("ws-auth-timeout").daemon().factory());

    private record Authenticated(String email, WebSocketSession outbound) {
    }

    public AlertWebSocketHandler(JwtService jwtService, AlertSessionRegistry registry, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        authTimeouts.schedule(() -> {
            if (!authenticated.containsKey(session.getId())) {
                closeQuietly(session, UNAUTHORIZED);
            }
        }, AUTH_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        if (authenticated.containsKey(session.getId())) {
            return; // the server only pushes; later client messages are ignored
        }
        Optional<String> email = parseAuthMessage(message.getPayload());
        if (email.isEmpty()) {
            closeQuietly(session, UNAUTHORIZED);
            return;
        }
        // Alerts are sent from other threads; the decorator serializes sends,
        // which a raw WebSocketSession does not allow concurrently.
        WebSocketSession outbound = new ConcurrentWebSocketSessionDecorator(
                session, SEND_TIME_LIMIT_MS, SEND_BUFFER_LIMIT_BYTES);
        authenticated.put(session.getId(), new Authenticated(email.get(), outbound));
        registry.register(email.get(), outbound);
        outbound.sendMessage(new TextMessage(AUTH_OK));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Authenticated auth = authenticated.remove(session.getId());
        if (auth != null) {
            registry.unregister(auth.email(), auth.outbound());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        closeQuietly(session, CloseStatus.SERVER_ERROR);
    }

    @Override
    public void destroy() {
        authTimeouts.shutdownNow();
    }

    private Optional<String> parseAuthMessage(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            if (node == null || !"auth".equals(node.path("type").asText())) {
                return Optional.empty();
            }
            JsonNode token = node.get("token");
            return token != null && token.isTextual() ? jwtService.verify(token.asText()) : Optional.empty();
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    private static void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            if (session.isOpen()) session.close(status);
        } catch (IOException ignored) {
            // already gone
        }
    }
}
