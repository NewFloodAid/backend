package com.example.flood_aid.configs;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Enumeration;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtRequestFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        Enumeration<String> sourceAppHeaders = request.getHeaders("X-Source-App");
        if (sourceAppHeaders != null && sourceAppHeaders.hasMoreElements()) {
            int headerCount = 0;
            while (sourceAppHeaders.hasMoreElements()) {
                sourceAppHeaders.nextElement();
                headerCount++;
            }

            if (headerCount > 1) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("Multiple X-Source-App headers are not allowed.");
                return;
            }
        }

        Enumeration<String> authorizationHeaders = request.getHeaders("Authorization");
        if (authorizationHeaders != null && authorizationHeaders.hasMoreElements()) {
            int headerCount = 0;
            while (authorizationHeaders.hasMoreElements()) {
                authorizationHeaders.nextElement();
                headerCount++;
            }

            if (headerCount > 1) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("Multiple Authorization headers are not allowed.");
                return;
            }
        }

        String authorizationHeader = request.getHeader("Authorization");
        String sourceApp = request.getHeader("X-Source-App");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);

            try {
                String username = jwtUtil.extractUsername(token);
                String appType = jwtUtil.extractAppType(token);

                if (username != null && jwtUtil.validateToken(token, username, appType)) {
                    if ((sourceApp.equalsIgnoreCase("Web") && appType.equals("Web")) ||
                        (sourceApp.equalsIgnoreCase("LIFF") && appType.equals("LIFF"))) {
                        setAuthentication(username, appType.equals("LIFF") ? "USER" : "WEB_USER", request);
                    } else {
                        System.err.println("JWT Token appType does not match X-Source-App");
                    }
                }
            } catch (Exception e) {
                System.err.println("Token validation error: " + e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }

    private void setAuthentication(String username, String role, HttpServletRequest request) {
        UserDetails userDetails = User.withUsername(username).password("").roles(role).build();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
