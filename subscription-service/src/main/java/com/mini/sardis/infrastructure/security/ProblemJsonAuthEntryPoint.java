package com.mini.sardis.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class ProblemJsonAuthEntryPoint implements AuthenticationEntryPoint {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/problem+json");

        Map<String, Object> body = Map.of(
                "type", "https://api.sardis.com/errors/unauthorized",
                "title", "Unauthorized",
                "status", 401,
                "detail", "Missing or invalid Bearer token",
                "instance", request.getRequestURI()
        );

        MAPPER.writeValue(response.getWriter(), body);
    }
}
