package fr.sparkit.accounting.application.controllers;

import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.dto.ChartAccountsDto;
import fr.sparkit.accounting.entities.ChartAccounts;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static fr.sparkit.accounting.util.errors.ApiErrors.Accounting.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ChartAccountsControllerTest extends ITConfig {
    public static final int TWO = 2;
    public static final int FOURTEEN = 14;
    public static final int ONE_HUNDRAND_TWENNTY_THREE = 123;
    public static final int TWO_HUNDRAND_TWENNTY_THREE = 223;

    public static final int TWO_L = 2L;

    public static final String CHILD_LABEL = "child label";

    @Order(1)
    @Test
    public void save() {

        ChartAccountsDto chartAccountsDtoChildren = new ChartAccountsDto(null, 1L, FOURTEEN, "Label", null);

        List<ChartAccountsDto> children = new ArrayList<>();
        children.add(chartAccountsDtoChildren);
        ChartAccountsDto chartAccountsDto = new ChartAccountsDto(null, null, 1, "Chart Account", children);

        HttpEntity<ChartAccountsDto> accountsDto = new HttpEntity<>(chartAccountsDto, getHttpHeaders());
        ResponseEntity<ChartAccounts> responseEntity = getTestRestTemplate().exchange(getPathUrl(), HttpMethod.POST,
                accountsDto, new ParameterizedTypeReference<ChartAccounts>() {
                });

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(1, responseEntity.getBody().getId());
    }

    @Order(2)
    @Test
    public void update() {

        ChartAccountsDto chartAccountsDto = new ChartAccountsDto(1L, 1L, 1, "Chart label", null);
        HttpEntity<ChartAccountsDto> accountsDto = new HttpEntity<>(chartAccountsDto, getHttpHeaders());

        ResponseEntity<ChartAccountsDto> responseEntity = getTestRestTemplate().exchange(getPathUrl().concat("/1"),
                HttpMethod.PUT, accountsDto, new ParameterizedTypeReference<ChartAccountsDto>() {
                });

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(1, responseEntity.getBody().getId());
        assertEquals("Chart label", responseEntity.getBody().getLabel());
    }

    @Order(3)
    @Test
    public void exportChartsAsExcelFile() {
        HttpEntity<ChartAccountsDto> chartAccountsDto = new HttpEntity<>(getHttpHeaders());

        ResponseEntity<String> responseEntity = getTestRestTemplate().exchange(getPathUrl().concat("/export-charts"),
                HttpMethod.GET, chartAccountsDto, String.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
    }

    @Order(4)
    @Test
    public void getChartAccountsToBalanced() {
        HttpEntity<ChartAccountsDto> chartAccountsDto = new HttpEntity<>(getHttpHeaders());

        ResponseEntity<String> responseEntity = getTestRestTemplate().exchange(
                getPathUrl().concat("/chart-account-to-balanced"), HttpMethod.GET, chartAccountsDto, String.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
    }

    @Order(5)
    @Test
    public void exportModel() {
        HttpEntity<ChartAccountsDto> chartAccountsDto = new HttpEntity<>(getHttpHeaders());

        ResponseEntity<String> responseEntity = getTestRestTemplate().exchange(getPathUrl().concat("/excel-template"),
                HttpMethod.GET, chartAccountsDto, String.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
    }

    @Order(6)
    @Test
    public void findByCodes() {

        int code = 1;
        HttpEntity<ChartAccountsDto> chartAccountsDto = new HttpEntity<>(getHttpHeaders());

        ResponseEntity<List<ChartAccountsDto>> responseEntity = getTestRestTemplate().exchange(
                getPathUrl().concat("/search-by-codes?code=" + code), HttpMethod.GET, chartAccountsDto,
                new ParameterizedTypeReference<List<ChartAccountsDto>>() {
                });

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(1, responseEntity.getBody().size());

    }

    @Order(7)
    @Test
    public void shouldReturnMissingParametersWhenSave() {

        ChartAccountsDto chartAccountsDtoChildren = new ChartAccountsDto(null, 1L, FOURTEEN, CHILD_LABEL, null);

        List<ChartAccountsDto> children = new ArrayList<>();
        children.add(chartAccountsDtoChildren);
        ChartAccountsDto chartAccountsDto = new ChartAccountsDto(null, null, 1, null, children);

        HttpEntity<ChartAccountsDto> accountsDto = new HttpEntity<>(chartAccountsDto, getHttpHeaders());
        ResponseEntity<LinkedHashMap> responseEntity = getTestRestTemplate().exchange(getPathUrl(), HttpMethod.POST,
                accountsDto, new ParameterizedTypeReference<LinkedHashMap>() {
                });

        assertEquals(TWO_HUNDRAND_TWENNTY_THREE, responseEntity.getStatusCodeValue());
        assertEquals(CHART_ACCOUNT_MISSING_PARAMETERS, responseEntity.getBody().get(AccountingConstants.ERROR_CODE));

    }

    @Order(8)
    @Test
    public void shouldReturnCodeLenghtInvalidWhenSave() {

        ChartAccountsDto chartAccountsDtoChildren = new ChartAccountsDto(null, 1L, FOURTEEN, CHILD_LABEL, null);

        List<ChartAccountsDto> children = new ArrayList<>();
        children.add(chartAccountsDtoChildren);
        ChartAccountsDto chartAccountsDto = new ChartAccountsDto(null, null, ONE_HUNDRAND_TWENNTY_THREE, CHILD_LABEL,
                children);

        HttpEntity<ChartAccountsDto> accountsDto = new HttpEntity<>(chartAccountsDto, getHttpHeaders());
        ResponseEntity<LinkedHashMap> responseEntity = getTestRestTemplate().exchange(getPathUrl(), HttpMethod.POST,
                accountsDto, new ParameterizedTypeReference<LinkedHashMap>() {
                });

        assertEquals(TWO_HUNDRAND_TWENNTY_THREE, responseEntity.getStatusCodeValue());
        assertEquals(ACCOUNT_CODE_LENGTH_INVALID, responseEntity.getBody().get(AccountingConstants.ERROR_CODE));

    }

    @Order(9)
    @Test
    public void shouldReturnCodeDiffirentThanParentWhenSave() {

        ChartAccountsDto chartAccountsDtoChildren = new ChartAccountsDto(null, TWO_L, FOURTEEN, CHILD_LABEL, null);

        List<ChartAccountsDto> children = new ArrayList<>();
        children.add(chartAccountsDtoChildren);
        ChartAccountsDto chartAccountsDto = new ChartAccountsDto(null, 1L, TWO, "chart label", children);

        HttpEntity<ChartAccountsDto> accountsDto = new HttpEntity<>(chartAccountsDto, getHttpHeaders());
        ResponseEntity<LinkedHashMap> responseEntity = getTestRestTemplate().exchange(getPathUrl(), HttpMethod.POST,
                accountsDto, new ParameterizedTypeReference<LinkedHashMap>() {
                });

        assertEquals(TWO_HUNDRAND_TWENNTY_THREE, responseEntity.getStatusCodeValue());
        assertEquals(ACCOUNT_CODE_DIFFERENT_THAN_PARENT, responseEntity.getBody().get(AccountingConstants.ERROR_CODE));

    }

    @Order(10)
    @Test
    public void shouldReturnCodeExistWhenSave() {

        ChartAccountsDto chartAccountsDtoChildren = new ChartAccountsDto(null, 1L, FOURTEEN, CHILD_LABEL, null);

        List<ChartAccountsDto> children = new ArrayList<>();
        children.add(chartAccountsDtoChildren);
        ChartAccountsDto chartAccountsDto = new ChartAccountsDto(null, null, 1, "chart label", children);

        HttpEntity<ChartAccountsDto> accountsDto = new HttpEntity<>(chartAccountsDto, getHttpHeaders());
        ResponseEntity<LinkedHashMap> responseEntity = getTestRestTemplate().exchange(getPathUrl(), HttpMethod.POST,
                accountsDto, new ParameterizedTypeReference<LinkedHashMap>() {
                });

        assertEquals(TWO_HUNDRAND_TWENNTY_THREE, responseEntity.getStatusCodeValue());
        assertEquals(CHART_ACCOUNT_CODE_EXISTS, responseEntity.getBody().get(AccountingConstants.ERROR_CODE));

    }

    @Order(11)
    @Test
    public void delete() {

        HttpEntity<ChartAccountsDto> chartAccountsDto = new HttpEntity<>(getHttpHeaders());

        ResponseEntity<String> responseEntity = getTestRestTemplate().exchange(getPathUrl().concat("/1"),
                HttpMethod.DELETE, chartAccountsDto, new ParameterizedTypeReference<String>() {
                });

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    private String getPathUrl() {
        return getRootUrl() + "/api/accounting/chart-account";
    }

}
