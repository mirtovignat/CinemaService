package com.example.demo.exception;

import com.example.demo.dto.authorize.LoginFormDTO;
import com.example.demo.dto.authorize.RegisterFormDTO;
import com.example.demo.dto.user.PasswordChangingDTO;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.util.MessageUtils;
import com.example.demo.web.security.CurrentUserService;
import com.example.demo.web.util.RefererUtils;
import com.example.demo.web.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

@ControllerAdvice
@AllArgsConstructor
public class GlobalExceptionHandler {

    private static final Map<String, String> FORM_VIEW_BY_REQUEST_URI = Map.of(
            "/register", "auth/register",
            "/login", "auth/login",
            "/settings/change/pwd", "account/change-password"
    );

    private final MessageUtils messageUtils;
    private final CurrentUserService currentUserService;

    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbortException(ClientAbortException clientAbortException) {
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public Object handleInsufficientAuthenticationException(HttpServletRequest httpServletRequest) {
        String errorMessage = messageUtils.getError(ErrorCode.NOT_AUTHORIZED);
        if (RequestUtils.isAjaxRequest(httpServletRequest)) {
            return buildProblemDetail(HttpStatus.UNAUTHORIZED, ErrorCode.NOT_AUTHORIZED, errorMessage);
        }
        return "redirect:/login";
    }

    @ExceptionHandler(BusinessException.class)
    public Object handleBusinessException(BusinessException businessException,
                                          HttpServletRequest httpServletRequest,
                                          Model model,
                                          RedirectAttributes redirectAttributes) {
        ErrorCode errorCode = businessException.getErrorCode();
        String errorMessage = messageUtils.getError(errorCode, businessException.getArgs());
        HttpStatus httpStatus = mapErrorCodeToHttpStatus(errorCode);
        return buildErrorResponse(httpServletRequest, model, redirectAttributes, errorCode, errorMessage, httpStatus);
    }

    @ExceptionHandler(IllegalStateException.class)
    public Object handleIllegalStateException(HttpServletRequest httpServletRequest,
                                              Model model,
                                              RedirectAttributes redirectAttributes) {
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        String errorMessage = messageUtils.getError(errorCode);
        return buildErrorResponse(httpServletRequest, model, redirectAttributes, errorCode, errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(BindException.class)
    public Object handleBindException(BindException bindException,
                                      HttpServletRequest httpServletRequest,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        return handleBindingResult(bindException.getBindingResult(), httpServletRequest, model, redirectAttributes);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleNoResourceFoundException(HttpServletRequest httpServletRequest, Model model) {
        if (RequestUtils.isAjaxRequest(httpServletRequest)) {
            return buildProblemDetail(HttpStatus.NOT_FOUND, ErrorCode.ENTITY_NOT_FOUND, "Ресурс не найден");
        }
        addCommonAttributes(model);
        model.addAttribute("status", 404);
        String errorMessage = messageUtils.getError(ErrorCode.ENTITY_NOT_FOUND);
        model.addAttribute("error", errorMessage);
        model.addAttribute("message", errorMessage);
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public Object handleGenericException(HttpServletRequest httpServletRequest, Model model) {
        String errorMessage = messageUtils.getError(ErrorCode.INTERNAL_ERROR);
        if (RequestUtils.isAjaxRequest(httpServletRequest)) {
            return buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, errorMessage);
        }
        addCommonAttributes(model);
        model.addAttribute("status", 500);
        model.addAttribute("error", errorMessage);
        model.addAttribute("message", errorMessage);
        return "error";
    }

    private Object handleBindingResult(BindingResult bindingResult,
                                       HttpServletRequest httpServletRequest,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        String validationMessage = resolveFirstFieldErrorMessage(bindingResult);

        if (RequestUtils.isAjaxRequest(httpServletRequest)) {
            return buildProblemDetail(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, validationMessage);
        }

        String formViewName = resolveFormViewName(httpServletRequest);
        if (formViewName != null) {
            addCommonAttributes(model);
            String bindingObjectName = bindingResult.getObjectName();
            if (!model.containsAttribute(bindingObjectName)) {
                model.addAttribute(bindingObjectName, bindingResult.getTarget());
            }
            model.addAttribute(BindingResult.MODEL_KEY_PREFIX + bindingObjectName, bindingResult);
            return formViewName;
        }

        return buildErrorResponse(httpServletRequest, model, redirectAttributes, ErrorCode.VALIDATION_ERROR, validationMessage, HttpStatus.BAD_REQUEST);
    }

    private String resolveFirstFieldErrorMessage(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(messageUtils.getError(ErrorCode.VALIDATION_ERROR));
    }

    private Object buildErrorResponse(HttpServletRequest httpServletRequest,
                                      Model model,
                                      RedirectAttributes redirectAttributes,
                                      ErrorCode errorCode,
                                      String errorMessage,
                                      HttpStatus httpStatus) {
        if (RequestUtils.isAjaxRequest(httpServletRequest)) {
            return buildProblemDetail(httpStatus, errorCode, errorMessage);
        }
        String formViewName = resolveFormViewName(httpServletRequest);
        if (formViewName != null) {
            addCommonAttributes(model);
            restoreFormModel(formViewName, model);
            model.addAttribute("errorMessage", errorMessage);
            return formViewName;
        }
        if (canUseFlashAttributes(httpServletRequest)) {
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
        }
        return "redirect:" + RefererUtils.resolveFallbackLocalPath(httpServletRequest);
    }

    private void addCommonAttributes(Model model) {
        SessionUser sessionUser = currentUserService.get();
        model.addAttribute("sessionUser", sessionUser);
    }

    private String resolveFormViewName(HttpServletRequest httpServletRequest) {
        return FORM_VIEW_BY_REQUEST_URI.get(httpServletRequest.getRequestURI());
    }

    private void restoreFormModel(String formViewName, Model model) {
        if ("auth/register".equals(formViewName) && !model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", RegisterFormDTO.getForm());
        }
        if ("auth/login".equals(formViewName) && !model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", LoginFormDTO.builder().build());
        }
        if ("account/change-password".equals(formViewName) && !model.containsAttribute("passwordChangingDTO")) {
            model.addAttribute("passwordChangingDTO", PasswordChangingDTO.builder().build());
        }
    }

    private boolean canUseFlashAttributes(HttpServletRequest httpServletRequest) {
        return httpServletRequest.getSession(false) != null;
    }

    private HttpStatus mapErrorCodeToHttpStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case USER_NOT_FOUND, ENTITY_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case PASSWORD_INVALID, NOT_AUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case INSUFFICIENT_FUNDS -> HttpStatus.PAYMENT_REQUIRED;
            case ALREADY_REGISTERED, ALREADY_TAKEN, USERNAME_TAKEN, DATA_COINCIDENCE -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private ResponseEntity<ProblemDetail> buildProblemDetail(HttpStatus httpStatus,
                                                             ErrorCode errorCode,
                                                             String message) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(httpStatus, message);
        problemDetail.setTitle(errorCode.name());
        return ResponseEntity.status(httpStatus).body(problemDetail);
    }
}