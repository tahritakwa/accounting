package fr.sparkit.accounting.services.utils;

import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;

import fr.sparkit.accounting.auditing.AuthorizationImpl;
import fr.sparkit.accounting.auditing.HasRoles;
import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.util.CurrentRequestContextHolder;
import fr.sparkit.accounting.util.http.HttpCustomException;

@Aspect
@Configuration
public class AuthAspect {
    @Autowired
    AuthorizationImpl authBean;

    @Before("@annotation(fr.sparkit.accounting.auditing.HasRoles)")
    public void before(JoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = CurrentRequestContextHolder.getCurrentHttpServletRequest();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = joinPoint.getTarget().getClass().getMethod(signature.getMethod().getName(),
                signature.getMethod().getParameterTypes());
        HasRoles hasRoles = method.getAnnotation(HasRoles.class);
        int haspermission = authBean.authorize(request.getHeader("Authorization"), hasRoles.permissions());
        if (haspermission == AccountingConstants.UNAUTHORIZED_CODE) {
            throw new AccessDeniedException("User doesn't have the proper permissions to access this API");
        } else if (haspermission != AccountingConstants.AUTHORIZED_CODE) {
            throw new HttpCustomException(haspermission);
        }
    }
}
