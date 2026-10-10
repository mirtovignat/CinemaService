package com.example.demo.config;

import com.example.demo.web.util.RequestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AjaxAwareEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest httpServletRequest,
                         HttpServletResponse httpServletResponse,
                         AuthenticationException authenticationException) throws IOException {
        if (RequestUtils.isAjaxRequest(httpServletRequest)) {
            httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpServletResponse.setContentType("application/json;charset=UTF-8");
            objectMapper.writeValue(
                    httpServletResponse.getWriter(),
                    Map.of("code", "NOT_AUTHORIZED")
            );
        } else {
            httpServletResponse.sendRedirect("/login");
        }
    }
}