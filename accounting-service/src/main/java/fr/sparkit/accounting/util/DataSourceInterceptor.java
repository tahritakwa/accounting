package fr.sparkit.accounting.util;

import static fr.sparkit.accounting.constants.AccountingConstants.COMPANY_HEADER_NAME;
import static fr.sparkit.accounting.constants.AccountingConstants.USER_ID_HEADER;

import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DataSourceInterceptor extends HandlerInterceptorAdapter {
    private final DBConfig dbConfig;
    private final CacheManager cacheManager;

    @Autowired
    public DataSourceInterceptor(DBConfig dbConfig, CacheManager cacheManager) {
        this.dbConfig = dbConfig;
        this.cacheManager = cacheManager;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        for (String name : cacheManager.getCacheNames()) {
            Objects.requireNonNull(cacheManager.getCache(name)).clear();
        }
        CurrentRequestContextHolder.setCurrentHttpServletRequest(request);
        if (request.getHeader(USER_ID_HEADER) != null) {
            request.setAttribute(USER_ID_HEADER, request.getHeader(USER_ID_HEADER));
        }
        if (request.getHeader(COMPANY_HEADER_NAME) != null) {
            try {
                String companyCode = request.getHeader(COMPANY_HEADER_NAME);
                CompanyContextHolder.setCompanyContext(companyCode);
                if (CompanyContextHolder.supportedCompanies.contains(companyCode)) {
                    return super.preHandle(request, response, handler);
                } else {
                    log.error("Company provided {} not supported, trying to refresh dataSource list ...",
                            request.getHeader(COMPANY_HEADER_NAME));
                    dbConfig.dataSource();
                    if (CompanyContextHolder.supportedCompanies.contains(companyCode)) {
                        return super.preHandle(request, response, handler);
                    }
                    log.error("Company provided {} is still not supported, can't proceed to datasource",
                            request.getHeader(COMPANY_HEADER_NAME));
                    throw new HttpCustomException(
                            ApiErrors.Accounting.INVALID_COMPANY_SPECIFIED_CANT_CONNECT_TO_DATA_SOURCE);
                }
            } catch (IllegalArgumentException e) {
                log.error("Company provided not valid, can't proceed to datasource", e);
                throw new HttpCustomException(
                        ApiErrors.Accounting.INVALID_COMPANY_SPECIFIED_CANT_CONNECT_TO_DATA_SOURCE);
            }
        } else {
            log.error("No company provided, can't proceed to datasource");
            throw new HttpCustomException(ApiErrors.Accounting.NO_COMPANY_SPECIFIED_CANT_CONNECT_TO_DATA_SOURCE);
        }
    }
}
