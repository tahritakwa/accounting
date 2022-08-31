package fr.sparkit.accounting.application.controllers;

import fr.sparkit.accounting.application.RestControlAdviser;
import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.dto.AccountDto;
import fr.sparkit.accounting.dto.ChartAccountsDto;
import fr.sparkit.accounting.services.IChartAccountsService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static fr.sparkit.accounting.util.errors.ApiErrors.Accounting.*;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AccountControllerTest extends ITConfig {
    public static final int ONE = 1;
    public static final int TWO = 2;
    public static final int EIGHT = 8;
    public static final int TEN = 10;
    public static final int TWENTY_FIVE = 25;
    public static final int TWO_THOUSAND_FIVE_HUNDRED_SIXTY_TWO = 2562;

    public static final Long TWO_L = 2L;

    private static final String FISCAL_YEAR_RESULT = "Résultat de l'exercice";
    @Autowired
    private IChartAccountsService chartAccountsService;

    @BeforeAll
    void setUp() {

        ChartAccountsDto chartAccountsDtoChildren = new ChartAccountsDto(null, 1L, ONE,
                randomUUID().toString().substring(0, EIGHT), null);

        List<ChartAccountsDto> children = new ArrayList<>();
        children.add(chartAccountsDtoChildren);

        ChartAccountsDto chartAccountsDto = new ChartAccountsDto(null, null, TWO,
                randomUUID().toString().substring(0, EIGHT), children);

        chartAccountsService.save(chartAccountsDto);

    }

    @Order(1)
    @Test
    void save() {
        AccountDto accountDto = new AccountDto(null, TWO_THOUSAND_FIVE_HUNDRED_SIXTY_TWO, FISCAL_YEAR_RESULT, 1L,
                TWENTY_FIVE, BigDecimal.ONE, BigDecimal.ONE, true, true, 1L, false);

        HttpEntity<AccountDto> accountDtoHttpEntity = new HttpEntity<>(accountDto, getHttpHeaders());
        ResponseEntity<AccountDto> responseEntity = this.getTestRestTemplate().exchange(getPathUrl(), HttpMethod.POST,
                accountDtoHttpEntity, AccountDto.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(TWO_THOUSAND_FIVE_HUNDRED_SIXTY_TWO, responseEntity.getBody().getCode());
        assertNotNull(responseEntity.getBody().getId());
    }

    @Order(2)
    @Test
    void update() {
        AccountDto accountDto = new AccountDto(1L, TWO_THOUSAND_FIVE_HUNDRED_SIXTY_TWO,
                (randomUUID().toString()).substring(0, TEN), 1L, TWENTY_FIVE, BigDecimal.ONE, BigDecimal.TEN, true,
                true, 1L, false);
        HttpEntity<AccountDto> accountDtoHttpEntity = new HttpEntity<>(accountDto, getHttpHeaders());
        ResponseEntity<AccountDto> responseEntity = this.getTestRestTemplate().exchange(getPathUrl().concat("/1"),
                HttpMethod.PUT, accountDtoHttpEntity, new ParameterizedTypeReference<AccountDto>() {
                });

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(1L, responseEntity.getBody().getId());
    }

    @Order(3)
    @Test
    void findById() {
        HttpEntity<AccountDto> accountDto = new HttpEntity<>(getHttpHeaders());

        ResponseEntity<AccountDto> responseEntity = this.getTestRestTemplate().exchange(getPathUrl().concat("/1"),
                HttpMethod.GET, accountDto, AccountDto.class);

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(1L, responseEntity.getBody().getId());
    }

    @Order(4)
    @Test
    void getAllAccounts() {
        HttpEntity<AccountDto> accountDto = new HttpEntity<>(getHttpHeaders());

        ResponseEntity<List<AccountDto>> responseEntity = this.getTestRestTemplate().exchange(
                getPathUrl().concat("/accounts"), HttpMethod.GET, accountDto,
                new ParameterizedTypeReference<List<AccountDto>>() {
                });

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(1, responseEntity.getBody().size());
        assertEquals(1L, responseEntity.getBody().get(0).getId());
    }

    @Order(5)
    @Test
    void findByPlanCode() {
        HttpEntity<AccountDto> accountDto = new HttpEntity<>(getHttpHeaders());

        ResponseEntity<List<AccountDto>> responseEntity = this.getTestRestTemplate().exchange(
                getPathUrl().concat("/accounts/plan-code/2"), HttpMethod.GET, accountDto,
                new ParameterizedTypeReference<List<AccountDto>>() {
                });

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(ONE, responseEntity.getBody().size());
        assertEquals(TWO, responseEntity.getBody().get(0).getPlanCode());
    }

    @Order(6)
    @Test
    void getAllAmortizationAccounts() {
        HttpEntity<AccountDto> accountDto = new HttpEntity<>(getHttpHeaders());

        ResponseEntity<LinkedHashMap> responseEntity = this.getTestRestTemplate().exchange(
                getPathUrl().concat("/amortization-accounts"), HttpMethod.GET, accountDto,
                new ParameterizedTypeReference<LinkedHashMap>() {
                });

        assertEquals(RestControlAdviser.CUSTOM_STATUS_CODE, responseEntity.getStatusCodeValue());
        assertEquals(ACCOUNTING_CONFIGURATION_NO_CONFIGURATION_FOUND,
                responseEntity.getBody().get(AccountingConstants.ERROR_CODE));
    }

    @Order(7)
    @Test
    void getAllImmobilizationAccounts() {
        HttpEntity<AccountDto> accountDto = new HttpEntity<>(getHttpHeaders());

        ResponseEntity<Object> responseEntity = this.getTestRestTemplate().exchange(
                getPathUrl().concat("/immobilization-accounts"), HttpMethod.GET, accountDto,
                new ParameterizedTypeReference<Object>() {
                });

        assertEquals(RestControlAdviser.CUSTOM_STATUS_CODE, responseEntity.getStatusCodeValue());
        assertEquals(ACCOUNTING_CONFIGURATION_NO_CONFIGURATION_FOUND,
                ((LinkedHashMap) responseEntity.getBody()).get(AccountingConstants.ERROR_CODE));

    }

    @Order(9)
    @Test
    void getReconcilableAccounts() {
        HttpEntity<AccountDto> accountDto = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<List<AccountDto>> responseEntity = this.getTestRestTemplate().exchange(
                getPathUrl().concat("/reconcilable-accounts"), HttpMethod.GET, accountDto,
                new ParameterizedTypeReference<List<AccountDto>>() {
                });

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(1, responseEntity.getBody().size());
    }

    @Order(10)
    @Test
    void generateAccountCode() {
        Long planId = 1L;
        HttpEntity<AccountDto> accountDto = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<String> responseEntity = this.getTestRestTemplate().exchange(
                getPathUrl().concat("/generate-account-code?planId=" + planId), HttpMethod.GET, accountDto,
                new ParameterizedTypeReference<String>() {
                });

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
    }

    @Order(11)
    @Test
    void findByCode() {
        int code = TWO_THOUSAND_FIVE_HUNDRED_SIXTY_TWO;
        HttpEntity<AccountDto> accountDto = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<String> responseEntity = this.getTestRestTemplate().exchange(
                getPathUrl().concat("/search-by-code?code=" + code), HttpMethod.GET, accountDto,
                new ParameterizedTypeReference<String>() {
                });

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
    }

    @Order(12)
    @Test
    void findAccountByCode() {
        int code = TWO_THOUSAND_FIVE_HUNDRED_SIXTY_TWO;
        String extremum = "MIN";
        HttpEntity<AccountDto> accountDto = new HttpEntity<>(getHttpHeaders());

        ResponseEntity<AccountDto> responseEntity = this.getTestRestTemplate().exchange(
                getPathUrl().concat("/search-account?code=" + code + "&extremum=" + extremum), HttpMethod.GET,
                accountDto, new ParameterizedTypeReference<AccountDto>() {
                });

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
    }

    @Order(13)
    @Test
    void shouldReturnCodeDifferentParentWhenSave() {
        AccountDto accountDto = new AccountDto(null, TWO_THOUSAND_FIVE_HUNDRED_SIXTY_TWO, FISCAL_YEAR_RESULT, 1L,
                TWENTY_FIVE, BigDecimal.ONE, BigDecimal.ONE, true, true, 1L, false);

        HttpEntity<AccountDto> accountDtoHttpEntity = new HttpEntity<>(accountDto, getHttpHeaders());
        ResponseEntity<LinkedHashMap> responseEntity = this.getTestRestTemplate().exchange(getPathUrl(),
                HttpMethod.POST, accountDtoHttpEntity, LinkedHashMap.class);

        assertEquals(RestControlAdviser.CUSTOM_STATUS_CODE, responseEntity.getStatusCodeValue());
        assertEquals(ACCOUNT_CODE_DIFFERENT_THAN_PARENT, responseEntity.getBody().get(AccountingConstants.ERROR_CODE));

    }

    @Order(14)
    @Test
    void shouldReturnChartNotExistWhenSave() {
        AccountDto accountDto = new AccountDto(null, TWO_THOUSAND_FIVE_HUNDRED_SIXTY_TWO, FISCAL_YEAR_RESULT, TWO_L,
                TWENTY_FIVE, BigDecimal.ONE, BigDecimal.ONE, true, true, 1L, false);

        HttpEntity<AccountDto> accountDtoHttpEntity = new HttpEntity<>(accountDto, getHttpHeaders());
        ResponseEntity<LinkedHashMap> responseEntity = this.getTestRestTemplate().exchange(getPathUrl(),
                HttpMethod.POST, accountDtoHttpEntity, LinkedHashMap.class);

        assertEquals(RestControlAdviser.CUSTOM_STATUS_CODE, responseEntity.getStatusCodeValue());
        assertEquals(CHART_ACCOUNT_PARENT_CHART_ACCOUNT_DONT_EXIST,
                responseEntity.getBody().get(AccountingConstants.ERROR_CODE));

    }

    @Order(15)
    @Test
    void shouldReturnCodeExistWhenSave() {
        AccountDto accountDto = new AccountDto(null, TWO_THOUSAND_FIVE_HUNDRED_SIXTY_TWO, FISCAL_YEAR_RESULT, 1L,
                TWENTY_FIVE, BigDecimal.ONE, BigDecimal.ONE, true, true, 1L, false);

        HttpEntity<AccountDto> accountDtoHttpEntity = new HttpEntity<>(accountDto, getHttpHeaders());
        ResponseEntity<LinkedHashMap> responseEntity = this.getTestRestTemplate().exchange(getPathUrl(),
                HttpMethod.POST, accountDtoHttpEntity, LinkedHashMap.class);

        assertEquals(RestControlAdviser.CUSTOM_STATUS_CODE, responseEntity.getStatusCodeValue());
        assertEquals(ACCOUNT_CODE_EXISTS, responseEntity.getBody().get(AccountingConstants.ERROR_CODE));

    }

    @Order(16)
    @Test
    void delete() {
        HttpEntity<AccountDto> accountDtoHttpEntity = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<String> responseEntity = this.getTestRestTemplate().exchange(getPathUrl().concat("/1"),
                HttpMethod.DELETE, accountDtoHttpEntity, new ParameterizedTypeReference<String>() {
                });

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    private String getPathUrl() {
        return getRootUrl() + "/api/accounting/account";
    }
}
