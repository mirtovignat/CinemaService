package com.example.demo.exception;

import com.example.demo.dto.response.ApiResponse;
import com.example.demo.util.MessageUtils;
import com.example.demo.web.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageUtils messageUtils;

    private String fallbackPath(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        return (referer != null && !referer.isBlank()) ? referer : "/";
    }

    @ExceptionHandler(BusinessException.class)
    public Object handleBusinessException(BusinessException ex,
                                          HttpServletRequest request,
                                          RedirectAttributes redirectAttributes) {
        ErrorCode code = ex.getCode();
        String message = messageUtils.getError(code, ex.getArgs());
        HttpStatus status = mapErrorCodeToHttpStatus(code);

        if (RequestUtils.isAjaxRequest(request)) {
            return ResponseEntity.status(status).body(
                    ApiResponse.error(code, message)
            );
        }

        redirectAttributes.addFlashAttribute("errorMessage", message);
        return "redirect:" + fallbackPath(request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidation(MethodArgumentNotValidException ex,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Ошибка валидации");

        if (RequestUtils.isAjaxRequest(request)) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(ErrorCode.VALIDATION_ERROR, message)
            );
        }

        redirectAttributes.addFlashAttribute("errorMessage", message);
        return "redirect:" + fallbackPath(request);
    }

    @ExceptionHandler(ServletRequestBindingException.class)
    public Object handleMissingSessionAttribute(HttpServletRequest request,
                                                RedirectAttributes redirectAttributes) {
        String message = messageUtils.getError(ErrorCode.NOT_AUTHORIZED);
        if (RequestUtils.isAjaxRequest(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponse.error(ErrorCode.NOT_AUTHORIZED, message)
            );
        }
        redirectAttributes.addFlashAttribute("errorMessage", message);
        return "redirect:/login";
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneric(Exception ex,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        log.error("Unhandled exception", ex);
        String message = messageUtils.getError(ErrorCode.INTERNAL_ERROR);

        if (RequestUtils.isAjaxRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(ErrorCode.INTERNAL_ERROR, message)
            );
        }

        String referer = request.getHeader("Referer");
        String currentUri = request.getRequestURI();
        if (referer == null || referer.isBlank() || referer.contains(currentUri)) {
            throw new RuntimeException(ex);
        }
        redirectAttributes.addFlashAttribute("errorMessage", message);
        return "redirect:" + referer;
    }

    private HttpStatus mapErrorCodeToHttpStatus(ErrorCode code) {
        return switch (code) {
            case USER_NOT_FOUND, ENTITY_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case PASSWORD_INVALID, NOT_AUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case INSUFFICIENT_FUNDS -> HttpStatus.PAYMENT_REQUIRED;
            case ALREADY_REGISTERED, ALREADY_TAKEN, DATA_COINCIDENCE -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}