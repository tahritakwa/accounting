package fr.sparkit.accounting.util;

import static fr.sparkit.accounting.constants.AccountingConstants.USER_EMAIL_HEADER_NAME;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;

//TODO: in the new version that uses AuthV2 this wil be replaced by the token interceptor
@Component
@Slf4j
public class UserInterceptorService extends HandlerInterceptorAdapter {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (request.getHeader(USER_EMAIL_HEADER_NAME) != null) {
            try {
                UserContextHolder.setUserContext(request.getHeader(USER_EMAIL_HEADER_NAME));
                return super.preHandle(request, response, handler);
            } catch (IllegalArgumentException e) {
                log.error("No user is specified", e);
                throw new HttpCustomException(ApiErrors.Accounting.NO_USER_IS_SPECIFIED);
            }
        } else {
            log.error("No user is specified");
            throw new HttpCustomException(ApiErrors.Accounting.NO_USER_IS_SPECIFIED);
        }
    }
}
