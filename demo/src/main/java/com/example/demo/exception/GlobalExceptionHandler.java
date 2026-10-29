package com.example.demo.exception;

import com.example.demo.dto.authorize.LoginFormDTO;
import com.example.demo.dto.authorize.RegisterFormDTO;
import com.example.demo.dto.badges.BadgeCountsDTO;
import com.example.demo.dto.movie.tmdb.TMDBCreateRequestDTO;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.user.PasswordChangingDTO;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.service.BadgeService;
import com.example.demo.util.MessageUtils;
import com.example.demo.web.security.CurrentUserProvider;
import com.example.demo.web.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
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
import java.util.List;
import java.util.Map;

@Slf4j
@ControllerAdvice
@AllArgsConstructor
public class GlobalExceptionHandler {

    private static final Map<String, String> FORM_VIEW_BY_REQUEST_URI = Map.of(
            "/register", "auth/register",
            "/login", "auth/login",
            "/settings/change/pwd", "account/change-password",
            "/movies/tmdb/create", "catalog/create-movie"
    );

    private final MessageUtils messageUtils;
    private final CurrentUserProvider currentUserProvider;
    private final BadgeService badgeService;

    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbortException(ClientAbortException clientAbortException) {
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public Object handleInsufficientAuthenticationException(
            HttpServletRequest httpServletRequest) {
        String errorMessage = messageUtils.getError(ErrorCode.NOT_AUTHORIZED);
        if (RequestUtils.isAjaxRequest(httpServletRequest)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(ErrorCode.NOT_AUTHORIZED, errorMessage));
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
        Object data = extractAjaxData(errorCode, businessException.getArgs());
        return buildErrorResponse(httpServletRequest, model, redirectAttributes,
                errorCode, errorMessage, httpStatus, data);
    }

    @ExceptionHandler(IllegalStateException.class)
    public Object handleIllegalStateException(HttpServletRequest httpServletRequest,
                                              Model model,
                                              RedirectAttributes redirectAttributes) {
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        String errorMessage = messageUtils.getError(errorCode);
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        return buildErrorResponse(httpServletRequest, model, redirectAttributes,
                errorCode, errorMessage, httpStatus, null);
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
    public Object handleNoResourceFoundException(HttpServletRequest httpServletRequest,
                                                 Model model) {
        if (RequestUtils.isAjaxRequest(httpServletRequest)) {
            return ResponseEntity.notFound().build();
        }

        addCommonAttributes(model);
        model.addAttribute("status", 404);
        model.addAttribute("error", messageUtils.getError(ErrorCode.ENTITY_NOT_FOUND));
        model.addAttribute("message", messageUtils.getError(ErrorCode.ENTITY_NOT_FOUND));
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public Object handleGenericException(Exception exception,
                                         HttpServletRequest httpServletRequest,
                                         Model model) {
        log.error("Необработанное исключение на URI: {}",
                httpServletRequest.getRequestURI(), exception);

        String errorMessage = messageUtils.getError(ErrorCode.INTERNAL_ERROR);

        if (RequestUtils.isAjaxRequest(httpServletRequest)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR, errorMessage));
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
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR, validationMessage));
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

        return buildErrorResponse(httpServletRequest, model, redirectAttributes,
                ErrorCode.VALIDATION_ERROR, validationMessage, HttpStatus.BAD_REQUEST, null);
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
                                      HttpStatus httpStatus,
                                      Object data) {
        if (RequestUtils.isAjaxRequest(httpServletRequest)) {
            return ResponseEntity.status(httpStatus)
                    .body(ApiResponse.errorWithDto(errorCode, errorMessage, data));
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
        return "redirect:" + resolveFallbackLocalPath(httpServletRequest);
    }

    private Object extractAjaxData(ErrorCode errorCode, Object[] args) {
        if (errorCode != ErrorCode.TMDB_MOVIE_ALREADY_EXISTS) {
            return null;
        }
        if (args == null || args.length == 0) {
            return null;
        }
        return args[0];
    }

    private void addCommonAttributes(Model model) {
        SessionUser sessionUser = currentUserProvider.getSessionUser();
        if (sessionUser == null) {
            sessionUser = SessionUser.ANONYMOUS;
        }
        model.addAttribute("sessionUser", sessionUser);

        if (!sessionUser.isAuthenticated()) {
            model.addAttribute("badges", BadgeCountsDTO.builder().build());
            return;
        }
        model.addAttribute("badges", badgeService.getBadgeCounts(sessionUser.id()));
    }

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
        if ("auth/register".equals(formViewName) && !model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", RegisterFormDTO.getForm());
        }
        if ("auth/login".equals(formViewName) && !model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", LoginFormDTO.builder().build());
        }
        if ("account/change-password".equals(formViewName)
                && !model.containsAttribute("passwordChangingDTO")) {
            model.addAttribute("passwordChangingDTO", PasswordChangingDTO.builder().build());
        }
        if ("catalog/create-movie".equals(formViewName)
                && !model.containsAttribute("tmdbCreateRequestDTO")) {
            model.addAttribute("tmdbCreateRequestDTO", TMDBCreateRequestDTO.builder()
                    .moviePriceItems(List.of())
                    .build());
        }
    }

    private boolean canUseFlashAttributes(HttpServletRequest httpServletRequest) {
        return httpServletRequest.getSession(false) != null;
    }

    private HttpStatus mapErrorCodeToHttpStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case USER_NOT_FOUND, ENTITY_NOT_FOUND, TMDB_SEARCH_EMPTY -> HttpStatus.NOT_FOUND;
            case PASSWORD_INVALID, NOT_AUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case INSUFFICIENT_FUNDS -> HttpStatus.PAYMENT_REQUIRED;
            case ALREADY_REGISTERED, ALREADY_TAKEN, DATA_COINCIDENCE, MOVIE_ALREADY_EXISTS, TMDB_MOVIE_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case TMDB_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}