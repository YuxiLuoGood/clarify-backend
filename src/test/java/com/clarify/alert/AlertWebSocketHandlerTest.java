package com.clarify.alert;

import com.clarify.security.JwtProperties;
import com.clarify.security.JwtService;
import com.clarify.transaction.Category;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlertWebSocketHandlerTest {

    private final JwtService jwt = new JwtService(
            new JwtProperties("test-secret-key-that-is-definitely-long-enough", Duration.ofHours(1)),
            Clock.systemUTC());

    private AlertSessionRegistry registry;
    private AlertWebSocketHandler handler;
    private WebSocketSession session;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        registry = new AlertSessionRegistry(mapper);
        handler = new AlertWebSocketHandler(jwt, registry, mapper);
        session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-1");
        when(session.isOpen()).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        handler.destroy();
    }

    private void send(String payload) throws Exception {
        handler.handleMessage(session, new TextMessage(payload));
    }

    @Test
    void validTokenRegistersTheSocketAndReceivesAlerts() throws Exception {
        send("{\"type\":\"auth\",\"token\":\"" + jwt.issue("a@example.com") + "\"}");

        verify(session).sendMessage(new TextMessage(AlertWebSocketHandler.AUTH_OK));
        assertThat(registry.sessionCount("a@example.com")).isEqualTo(1);

        registry.send("a@example.com", BudgetAlert.exceeded(Category.FOOD, new BigDecimal("350"), new BigDecimal("300")));
        verify(session).sendMessage(argThat(m -> m.getPayload().toString().contains("BUDGET_EXCEEDED")));

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);
        assertThat(registry.sessionCount("a@example.com")).isZero();
    }

    @Test
    void invalidTokenClosesWith4401() throws Exception {
        send("{\"type\":\"auth\",\"token\":\"not-a-jwt\"}");

        verify(session).close(AlertWebSocketHandler.UNAUTHORIZED);
        verify(session, never()).sendMessage(any());
    }

    @Test
    void anythingButAnAuthMessageClosesWith4401() throws Exception {
        send("hello");

        verify(session).close(AlertWebSocketHandler.UNAUTHORIZED);
    }

    @Test
    void alertsOnlyReachTheirOwner() throws Exception {
        send("{\"type\":\"auth\",\"token\":\"" + jwt.issue("a@example.com") + "\"}");

        registry.send("b@example.com", BudgetAlert.exceeded(Category.FOOD, BigDecimal.TEN, BigDecimal.ONE));

        verify(session, never()).sendMessage(argThat(m -> m.getPayload().toString().contains("BUDGET_EXCEEDED")));
    }
}
