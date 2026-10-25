package com.example.demo.web;

import com.example.demo.dto.response.ApiResponse;
import com.example.demo.exception.ErrorCode;
import com.example.demo.web.util.RequestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@AllArgsConstructor
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
                    ApiResponse.error(ErrorCode.NOT_AUTHORIZED, null)
            );
        } else {
            httpServletResponse.sendRedirect("/login");
        }
    }
}