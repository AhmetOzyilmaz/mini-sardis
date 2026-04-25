package com.mini.sardis.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class ProblemJsonAccessDeniedHandler implements AccessDeniedHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/problem+json");

        Map<String, Object> body = Map.of(
                "type", "https://api.sardis.com/errors/forbidden",
                "title", "Forbidden",
                "status", 403,
                "detail", "Insufficient permissions or resource ownership violation",
                "instance", request.getRequestURI()
        );

        MAPPER.writeValue(response.getWriter(), body);
    }
}
