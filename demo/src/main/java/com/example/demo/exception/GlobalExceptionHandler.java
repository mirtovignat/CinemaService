package com.example.demo.exception;

import com.example.demo.dto.authorize.LoginFormDTO;
import com.example.demo.dto.authorize.RegisterFormDTO;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.user.PasswordChangingDTO;
import com.example.demo.util.MessageUtils;
import com.example.demo.web.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final Map<String, String> FORM_VIEWS = Map.of(
            "/register", "register",
            "/login", "login",
            "/settings/change/pwd", "change-password"
    );

    private final MessageUtils messageUtils;

    private String fallbackPath(HttpServletRequest httpServletRequest) {
        String referer = httpServletRequest.getHeader("Referer");
        return (referer != null && !referer.isBlank()) ? referer : "/";
    }

    private String resolveFormView(HttpServletRequest httpServletRequest) {
        String uri = httpServletRequest.getRequestURI();
        return FORM_VIEWS.get(uri);
    }

    private void restoreFormModel(String view, Model model) {
        if ("register".equals(view) && !model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", RegisterFormDTO.getForm());
        }
        if ("login".equals(view) && !model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", LoginFormDTO.builder().build());
        }
        if ("change-password".equals(view) && !model.containsAttribute("passwordChangingDTO")) {
            model.addAttribute("passwordChangingDTO", PasswordChangingDTO.builder().build());
        }
    }

    private boolean canUseFlash(HttpServletRequest httpServletRequest) {
        return httpServletRequest.getSession(false) != null;
    }

    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbort(ClientAbortException clientAbortException) {
    }

    @ExceptionHandler(BusinessException.class)
    public Object handleBusinessException(BusinessException businessException,
                                          HttpServletRequest httpServletRequest,
                                          Model model,
                                          RedirectAttributes redirectAttributes) {
        ErrorCode errorCode = businessException.getCode();
        String message = messageUtils.getError(errorCode, businessException.getArgs());
        HttpStatus status = mapErrorCodeToHttpStatus(errorCode);

        if (RequestUtils.isAjaxRequest(httpServletRequest)) {
            return ResponseEntity.status(status).body(ApiResponse.error(errorCode, message));
        }

        String formView = resolveFormView(httpServletRequest);
        if (formView != null) {
            restoreFormModel(formView, model);
            model.addAttribute("errorMessage", message);
            return formView;
        }

        if (canUseFlash(httpServletRequest)) {
            redirectAttributes.addFlashAttribute("errorMessage", message);
        }
        return "redirect:" + fallbackPath(httpServletRequest);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidation(MethodArgumentNotValidException methodArgumentNotValidException,
                                   HttpServletRequest httpServletRequest,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        BindingResult bindingResult = methodArgumentNotValidException.getBindingResult();

        if (RequestUtils.isAjaxRequest(httpServletRequest)) {
            String message = bindingResult.getFieldErrors().stream()
                    .findFirst()
                    .map(FieldError::getDefaultMessage)
                    .orElse("Ошибка валидации");
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR, message));
        }

        String formView = resolveFormView(httpServletRequest);
        if (formView != null) {
            String objectName = bindingResult.getObjectName();
            if (!model.containsAttribute(objectName)) {
                model.addAttribute(objectName, bindingResult.getTarget());
            }
            model.addAttribute(BindingResult.MODEL_KEY_PREFIX + objectName, bindingResult);
            return formView;
        }

        String message = bindingResult.getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Ошибка валидации");
        if (canUseFlash(httpServletRequest)) {
            redirectAttributes.addFlashAttribute("errorMessage", message);
        }
        return "redirect:" + fallbackPath(httpServletRequest);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleNoResource(NoResourceFoundException noResourceFoundException,
                                   HttpServletRequest httpServletRequest,
                                   Model model) {
        if (RequestUtils.isAjaxRequest(httpServletRequest)) {
            return ResponseEntity.notFound().build();
        }

        model.addAttribute("status", 404);
        model.addAttribute("error", "Страница не найдена");
        model.addAttribute("message", "Возможно, страница была удалена или вы ошиблись в адресе.");
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneric(Exception exception,
                                HttpServletRequest httpServletRequest,
                                Model model) {
        log.error("Необработанное исключение на URI: {}", httpServletRequest.getRequestURI(), exception);

        if (RequestUtils.isAjaxRequest(httpServletRequest)) {
            String message = messageUtils.getError(ErrorCode.INTERNAL_ERROR);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR, message));
        }

        model.addAttribute("status", 500);
        model.addAttribute("error", "Внутренняя ошибка");
        model.addAttribute("message", "Что-то пошло не так. Мы уже работаем над этим.");
        return "error";
    }

    private HttpStatus mapErrorCodeToHttpStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case USER_NOT_FOUND, ENTITY_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case PASSWORD_INVALID, NOT_AUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case INSUFFICIENT_FUNDS -> HttpStatus.PAYMENT_REQUIRED;
            case ALREADY_REGISTERED, ALREADY_TAKEN, DATA_COINCIDENCE -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}