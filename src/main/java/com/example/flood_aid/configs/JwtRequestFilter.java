package com.example.flood_aid.configs;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtRequestFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (hasDuplicateHeader(request, response, "X-Source-App", "Multiple X-Source-App headers are not allowed.")) {
            return;
        }
        if (hasDuplicateHeader(request, response, "Authorization", "Multiple Authorization headers are not allowed.")) {
            return;
        }

        String authorizationHeader = request.getHeader("Authorization");
        String sourceApp = request.getHeader("X-Source-App");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);

            try {
                Claims claims = jwtUtil.extractAllClaims(token);
                String username = claims.getSubject();
                String appType = claims.get("appType", String.class);
                UUID userId = extractUuidClaim(claims, "userId");

                if (username != null && jwtUtil.validateToken(token)) {
                    if (sourceApp != null && appType != null && !sourceApp.equalsIgnoreCase(appType)) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("X-Source-App header does not match token appType.");
                        return;
                    }

                    if (SecurityContextHolder.getContext().getAuthentication() == null) {
                        JwtPrincipal principal = new JwtPrincipal(username, appType, userId);
                        setAuthentication(principal, resolveRole(appType), request);
                    }
                }
            } catch (JwtException | IllegalArgumentException e) {
                log.warn("JWT validation error: {}", e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }

    private boolean hasDuplicateHeader(
            HttpServletRequest request,
            HttpServletResponse response,
            String headerName,
            String errorMessage) throws IOException {
        Enumeration<String> headers = request.getHeaders(headerName);
        if (headers == null || !headers.hasMoreElements()) {
            return false;
        }

        int headerCount = 0;
        while (headers.hasMoreElements()) {
            headers.nextElement();
            headerCount++;
            if (headerCount > 1) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(errorMessage);
                return true;
            }
        }
        return false;
    }

    private String resolveRole(String appType) {
        return "LIFF".equalsIgnoreCase(appType) ? "USER" : "WEB_USER";
    }

    private UUID extractUuidClaim(Claims claims, String claimName) {
        String value = claims.get(claimName, String.class);
        if (value == null || value.isBlank()) {
            return null;
        }
        return UUID.fromString(value);
    }

    private void setAuthentication(JwtPrincipal principal, String role, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
