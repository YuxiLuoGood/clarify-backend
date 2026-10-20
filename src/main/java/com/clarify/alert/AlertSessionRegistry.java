package com.clarify.alert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Open alert sockets grouped by user email. In-memory, like Spring's simple
 * broker was: fine for one instance; use a shared broker (e.g. Redis pub/sub)
 * before running several.
 */
@Component
public class AlertSessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(AlertSessionRegistry.class);

    private final ConcurrentMap<String, Set<WebSocketSession>> sessionsByEmail = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public AlertSessionRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(String email, WebSocketSession session) {
        sessionsByEmail.computeIfAbsent(email, e -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(String email, WebSocketSession session) {
        sessionsByEmail.computeIfPresent(email, (e, sessions) -> {
            sessions.remove(session);
            return sessions.isEmpty() ? null : sessions;
        });
    }

    public int sessionCount(String email) {
        Set<WebSocketSession> sessions = sessionsByEmail.get(email);
        return sessions == null ? 0 : sessions.size();
    }

    /** Sends {@code payload} as JSON to every open socket of that user; dead sockets are dropped. */
    public void send(String email, Object payload) {
        Set<WebSocketSession> sessions = sessionsByEmail.get(email);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        TextMessage message;
        try {
            message = new TextMessage(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Payload is not serializable", e);
        }
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(message);
                } else {
                    unregister(email, session);
                }
            } catch (IOException | IllegalStateException e) {
                log.debug("Dropping alert socket for {}: {}", email, e.toString());
                unregister(email, session);
            }
        }
    }
}
