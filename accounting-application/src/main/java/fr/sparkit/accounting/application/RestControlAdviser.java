package fr.sparkit.accounting.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.constants.NumberConstant;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.errors.ErrorsResponse;
import fr.sparkit.accounting.util.http.HttpCustomException;
import fr.sparkit.accounting.util.http.HttpErrorResponse;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice()
@Slf4j
public class RestControlAdviser {

    public static final int CUSTOM_STATUS_CODE = 223;
    private static final String FIELD_NAME = "fieldName";
    private static final String MIN_LENGTH = "minLength";
    private static final String MAX_LENGTH = "maxLength";
    private static final String METHOD_ARGUMENT_METHOD_NOT_VALID_EXCEPTION = "Field {} of object {} value not valid";
    private static final String INVALID_FORMAT_EXCEPTION = "Invalid Format Exception : {}";
    private static final String HTTP_MESSAGE_NOT_READBLE_EXCEPTION = "Http Message Not Readable Exception : {}";
    private static final String ACCES_DENEID = "Acces : {}";

    @ExceptionHandler({ HttpCustomException.class })
    public HttpErrorResponse handleCustomerException(HttpCustomException exception, HttpServletResponse response) {
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(exception.getErrorCode(),
                exception.getErrorsResponse());
        response.setStatus(CUSTOM_STATUS_CODE);
        log.error("HttpCustomException => error code : {} , details : {}", exception.getErrorCode(),
                exception.getErrorsResponse().getErrors());
        return httpErrorResponse;
    }

    @ExceptionHandler({ MethodArgumentNotValidException.class })
    public HttpErrorResponse handleMethodArgumentException(MethodArgumentNotValidException exception,
            HttpServletResponse response) {
        HashMap<String, String> errors = new HashMap<>();
        BindingResult bindingResult = exception.getBindingResult();
        FieldError fieldError = bindingResult.getFieldError();
        Object[] arguments = Objects.requireNonNull(fieldError).getArguments();
        int errorCode = ApiErrors.Accounting.ENTITY_FIELD_NOT_VALID;

        errors.put(FIELD_NAME, Objects.requireNonNull(fieldError.getField()));
        if ("NotNullField".equals(Objects.requireNonNull(fieldError.getCodes())[fieldError.getCodes().length - 1])) {
            errorCode = (int) arguments[NumberConstant.ONE];
            errors.put(AccountingConstants.FIELD_NOT_NULL, StringUtils.EMPTY);
            log.error(METHOD_ARGUMENT_METHOD_NOT_VALID_EXCEPTION, bindingResult.getFieldError().getField(),
                    bindingResult.getObjectName());
        }

        if ("Size".equals(fieldError.getCodes()[fieldError.getCodes().length - NumberConstant.ONE])) {
            errors.put(MIN_LENGTH, Objects.requireNonNull(fieldError.getArguments())[NumberConstant.TWO].toString());
            errors.put(MAX_LENGTH, Objects.requireNonNull(fieldError.getArguments())[NumberConstant.ONE].toString());
            log.error(METHOD_ARGUMENT_METHOD_NOT_VALID_EXCEPTION, fieldError.getField(), bindingResult.getObjectName());
        }

        if ("Min".equals(fieldError.getCodes()[fieldError.getCodes().length - NumberConstant.ONE])) {
            errors.put(MIN_LENGTH, Objects.requireNonNull(fieldError.getArguments())[NumberConstant.ONE].toString());
            log.error(METHOD_ARGUMENT_METHOD_NOT_VALID_EXCEPTION, fieldError.getField(), bindingResult.getObjectName());
        }

        if ("Max".equals(fieldError.getCodes()[fieldError.getCodes().length - NumberConstant.ONE])) {
            errors.put(MAX_LENGTH,
                    String.valueOf(Objects.requireNonNull(fieldError.getArguments())[NumberConstant.ONE]));
            log.error(METHOD_ARGUMENT_METHOD_NOT_VALID_EXCEPTION, fieldError.getField(), bindingResult.getObjectName());
        }

        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(errorCode, new ErrorsResponse().error(errors));

        response.setStatus(CUSTOM_STATUS_CODE);
        return httpErrorResponse;
    }

    @ExceptionHandler({ InvalidFormatException.class })
    public HttpErrorResponse handleInvalidFormatException(Throwable exception, HttpServletResponse response) {
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(ApiErrors.Accounting.INVALID_FORMAT_EXCEPTION);
        response.setStatus(CUSTOM_STATUS_CODE);
        log.error(INVALID_FORMAT_EXCEPTION, exception.getMessage());
        return httpErrorResponse;
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class })
    public HttpErrorResponse handleHttpMessageNotReadableException(Throwable exception, HttpServletResponse response) {

        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(
                ApiErrors.Accounting.HTTP_MESSAGE_NOT_READABLE_EXCEPTION);
        log.error(HTTP_MESSAGE_NOT_READBLE_EXCEPTION, exception.getMessage());

        response.setStatus(CUSTOM_STATUS_CODE);
        return httpErrorResponse;
    }

    @ExceptionHandler({ AccessDeniedException.class })
    public HttpErrorResponse accessDeniedException(Throwable exception, HttpServletResponse response) {
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(HttpServletResponse.SC_UNAUTHORIZED);
        log.error(ACCES_DENEID, exception.getMessage());
        List<Object> errosMsg = new ArrayList<>();
        errosMsg.add(exception.getMessage());
        httpErrorResponse.setErrors(errosMsg);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return httpErrorResponse;
    }

}
