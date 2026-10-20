package com.clarify.security;

import com.clarify.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates requests that carry {@code Authorization: Bearer <jwt>}.
 * Requests without a token pass through untouched; the security rules
 * decide whether they need one.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Request attribute read by {@link RestAuthenticationEntryPoint} to explain a 401. */
    static final String AUTH_ERROR_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".error";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository users;
    private final WebAuthenticationDetailsSource detailsSource = new WebAuthenticationDetailsSource();

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            jwtService.verify(token)
                    .flatMap(users::findByEmail)   // a token for a deleted user is no good either
                    .ifPresentOrElse(user -> {
                        var principal = new CurrentUser(user.getId(), user.getEmail());
                        var authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of());
                        authentication.setDetails(detailsSource.buildDetails(request));
                        SecurityContext context = SecurityContextHolder.createEmptyContext();
                        context.setAuthentication(authentication);
                        SecurityContextHolder.setContext(context);
                    }, () -> request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Invalid or expired token"));
        }
        chain.doFilter(request, response);
    }
}
