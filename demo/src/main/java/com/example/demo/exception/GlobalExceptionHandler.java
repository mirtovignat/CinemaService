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
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.Map;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final Map<String, String> FORM_VIEW_BY_REQUEST_URI = Map.of(
            "/register", "register",
            "/login", "login",
            "/settings/change/pwd", "change-password"
    );

    private final MessageUtils messageUtils;

    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbortException(ClientAbortException clientAbortException) {
    }

    @ExceptionHandler(BusinessException.class)
    public Object handleBusinessException(BusinessException businessException,
                                          HttpServletRequest httpServletRequest,
                                          Model model,
                                          RedirectAttributes redirectAttributes) {
        ErrorCode errorCode = businessException.getCode();
        String errorMessage = messageUtils.getError(errorCode, businessException.getArgs());
        HttpStatus httpStatus = mapErrorCodeToHttpStatus(errorCode);
        return buildErrorResponse(httpServletRequest, model, redirectAttributes,
                errorCode, errorMessage, httpStatus);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleMethodArgumentNotValidException(MethodArgumentNotValidException methodArgumentNotValidException,
                                                        HttpServletRequest httpServletRequest,
                                                        Model model,
                                                        RedirectAttributes redirectAttributes) {
        return handleBindingResult(methodArgumentNotValidException.getBindingResult(),
                httpServletRequest, model, redirectAttributes);
    }

    @ExceptionHandler(BindException.class)
    public Object handleBindException(BindException bindException,
                                      HttpServletRequest httpServletRequest,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        return handleBindingResult(bindException.getBindingResult(),
                httpServletRequest, model, redirectAttributes);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleNoResourceFoundException(NoResourceFoundException noResourceFoundException,
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
    public Object handleGenericException(Exception exception,
                                         HttpServletRequest httpServletRequest,
                                         Model model) {
        log.error("Необработанное исключение на URI: {}",
                httpServletRequest.getRequestURI(), exception);

        if (RequestUtils.isAjaxRequest(httpServletRequest)) {
            String errorMessage = messageUtils.getError(ErrorCode.INTERNAL_ERROR);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR, errorMessage));
        }

        model.addAttribute("status", 500);
        model.addAttribute("error", "Внутренняя ошибка");
        model.addAttribute("message", "Что-то пошло не так. Мы уже работаем над этим.");
        return "error";
    }

    private Object handleBindingResult(BindingResult bindingResult,
                                       HttpServletRequest httpServletRequest,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        if (RequestUtils.isAjaxRequest(httpServletRequest)) {
            String validationMessage = resolveFirstFieldErrorMessage(bindingResult);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR, validationMessage));
        }

        String formViewName = resolveFormViewName(httpServletRequest);
        if (formViewName != null) {
            String bindingObjectName = bindingResult.getObjectName();
            if (!model.containsAttribute(bindingObjectName)) {
                model.addAttribute(bindingObjectName, bindingResult.getTarget());
            }
            model.addAttribute(BindingResult.MODEL_KEY_PREFIX + bindingObjectName, bindingResult);
            return formViewName;
        }

        String validationMessage = resolveFirstFieldErrorMessage(bindingResult);
        return buildErrorResponse(httpServletRequest, model, redirectAttributes,
                ErrorCode.VALIDATION_ERROR, validationMessage, HttpStatus.BAD_REQUEST);
    }

    private String resolveFirstFieldErrorMessage(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Ошибка валидации");
    }

    private Object buildErrorResponse(HttpServletRequest httpServletRequest,
                                      Model model,
                                      RedirectAttributes redirectAttributes,
                                      ErrorCode errorCode,
                                      String errorMessage,
                                      HttpStatus httpStatus) {
        if (RequestUtils.isAjaxRequest(httpServletRequest)) {
            return ResponseEntity.status(httpStatus)
                    .body(ApiResponse.error(errorCode, errorMessage));
        }

        String formViewName = resolveFormViewName(httpServletRequest);
        if (formViewName != null) {
            restoreFormModel(formViewName, model);
            model.addAttribute("errorMessage", errorMessage);
            return formViewName;
        }

        if (canUseFlashAttributes(httpServletRequest)) {
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
        }
        return "redirect:" + resolveFallbackLocalPath(httpServletRequest);
    }

    /**
     * Всегда возвращаем локальный путь. Referer из внешнего домена игнорируем.
     */
    private String resolveFallbackLocalPath(HttpServletRequest httpServletRequest) {
        String refererHeader = httpServletRequest.getHeader("Referer");
        if (refererHeader == null || refererHeader.isBlank()) {
            return "/";
        }
        String localPath = extractLocalPathFromReferer(refererHeader, httpServletRequest);
        return localPath != null ? localPath : "/";
    }

    private String extractLocalPathFromReferer(String refererHeader,
                                               HttpServletRequest httpServletRequest) {
        try {
            URI refererUri = URI.create(refererHeader);
            String refererHost = refererUri.getHost();
            if (refererHost == null
                    || !refererHost.equalsIgnoreCase(httpServletRequest.getServerName())) {
                return null;
            }
            int refererPort = refererUri.getPort();
            int serverPort = httpServletRequest.getServerPort();
            if (refererPort != -1 && refererPort != serverPort) {
                return null;
            }
            String refererPath = refererUri.getRawPath();
            if (refererPath == null
                    || refererPath.isBlank()
                    || !refererPath.startsWith("/")
                    || refererPath.startsWith("//")) {
                return null;
            }
            String refererQuery = refererUri.getRawQuery();
            return (refererQuery == null || refererQuery.isBlank())
                    ? refererPath
                    : refererPath + "?" + refererQuery;
        } catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }
    }

    private String resolveFormViewName(HttpServletRequest httpServletRequest) {
        return FORM_VIEW_BY_REQUEST_URI.get(httpServletRequest.getRequestURI());
    }

    private void restoreFormModel(String formViewName, Model model) {
        if ("register".equals(formViewName) && !model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", RegisterFormDTO.getForm());
        }
        if ("login".equals(formViewName) && !model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", LoginFormDTO.builder().build());
        }
        if ("change-password".equals(formViewName)
                && !model.containsAttribute("passwordChangingDTO")) {
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
            case ALREADY_REGISTERED, ALREADY_TAKEN, DATA_COINCIDENCE -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}