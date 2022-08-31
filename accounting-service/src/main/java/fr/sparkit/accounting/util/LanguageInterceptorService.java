package fr.sparkit.accounting.util;

import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.http.HttpCustomException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import static fr.sparkit.accounting.constants.AccountingConstants.LANGUAGE_HEADER_NAME;

@Component
@Slf4j
public class LanguageInterceptorService extends HandlerInterceptorAdapter {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (request.getHeader(LANGUAGE_HEADER_NAME) != null) {
            try {
                LanguageContextHolderService.setLanguageContext(request.getHeader(LANGUAGE_HEADER_NAME));
                return super.preHandle(request, response, handler);
            } catch (IllegalArgumentException e) {
                log.error("No language is available", e);
                throw new HttpCustomException(ApiErrors.Accounting.NO_LANGUAGE_IS_SPECIFIED);
            }
        } else {
            log.error("No language is available");
            throw new HttpCustomException(ApiErrors.Accounting.NO_LANGUAGE_IS_SPECIFIED);
        }
    }
}
