package fr.sparkit.accounting.application.controllers;

import fr.sparkit.accounting.application.RestControlAdviser;
import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.dto.*;
import fr.sparkit.accounting.services.IAccountService;
import fr.sparkit.accounting.services.IChartAccountsService;
import fr.sparkit.accounting.services.IJournalService;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import static fr.sparkit.accounting.util.errors.ApiErrors.Accounting.*;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TemplateAccountingControllerTest extends ITConfig {
    public static final int TWO = 2;
    public static final int EIGHT = 8;
    public static final int TWENTY_FIVE = 25;
    public static final int TWO_THOUSAND_FIVE_HUNDRED_SIXTY_TWO = 2562;
    public static final int ZERO_POINT_TWENTY_SIX = 0.26;
    public static final int ZERO_POINT_THREE_TWO_THREE_SIX = 0.3236;

    private static final String LABEL = "Label";
    public static final String JOURNAL_LABEL = "Journal  Label";
    @Autowired
    private IAccountService accountService;
    @Autowired
    private IChartAccountsService chartAccountsService;
    @Autowired
    private IJournalService journalService;

    @BeforeAll
    void setUp() {

        ChartAccountsDto chartAccountsDtoChildren = new ChartAccountsDto(null, 1L, 1,
                randomUUID().toString().substring(0, EIGHT), null);

        List<ChartAccountsDto> children = new ArrayList<>();
        children.add(chartAccountsDtoChildren);

        ChartAccountsDto chartAccountsDto = new ChartAccountsDto(null, null, TWO,
                randomUUID().toString().substring(0, EIGHT), children);

        AccountDto accountDto = new AccountDto(null, TWO_THOUSAND_FIVE_HUNDRED_SIXTY_TWO, "Résultat de l'exercice", 1L,
                TWENTY_FIVE, BigDecimal.ONE, BigDecimal.ONE, true, true, 1L, false);
        JournalDto journalDto = new JournalDto(null, "1234", "JournalLabel", LocalDateTime.now(), true);
        journalService.save(journalDto);
        chartAccountsService.save(chartAccountsDto);
        accountService.saveAccount(accountDto);

    }

    @Order(1)
    @Test
    public void save() {

        TemplateAccountingDetailsDto templateAccountingDetailsDto = new TemplateAccountingDetailsDto(null, 1L,
                BigDecimal.ZERO, BigDecimal.ZERO, LABEL);
        List<TemplateAccountingDetailsDto> accountingDetailsDtos = new ArrayList<>();
        accountingDetailsDtos.add(templateAccountingDetailsDto);
        TemplateAccountingDto templateAccountingDto = new TemplateAccountingDto(null, "Template Accounting", 1L,
                JOURNAL_LABEL, accountingDetailsDtos);
        HttpEntity entity = new HttpEntity<>(templateAccountingDto, getHttpHeaders());

        ResponseEntity<TemplateAccountingDto> responseEntity = getTestRestTemplate().exchange(getApiUrl(),
                HttpMethod.POST, entity, TemplateAccountingDto.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(1, responseEntity.getBody().getId());

    }

    @Order(2)
    @Test
    public void update() {

        int id = 1;
        TemplateAccountingDetailsDto templateAccountingDetailsDto = new TemplateAccountingDetailsDto(null, 1L,
                BigDecimal.TEN, BigDecimal.ZERO, "Account detail new Label");
        List<TemplateAccountingDetailsDto> accountingDetailsDtos = new ArrayList<>();
        accountingDetailsDtos.add(templateAccountingDetailsDto);
        TemplateAccountingDto templateAccountingDto = new TemplateAccountingDto(1L, "Template Account  new label", 1L,
                UUID.randomUUID().toString(), accountingDetailsDtos);

        HttpEntity<TemplateAccountingDto> entity = new HttpEntity<>(templateAccountingDto, getHttpHeaders());
        ResponseEntity<TemplateAccountingDto> responseEntity = getTestRestTemplate()
                .exchange(getApiUrl().concat("/" + id), HttpMethod.PUT, entity, TemplateAccountingDto.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals("Template Account  new label", responseEntity.getBody().getLabel());
        assertEquals("Account detail new Label",
                responseEntity.getBody().getTemplateAccountingDetails().get(0).getLabel());

    }

    @Order(4)
    @Test
    public void allTemplates() {

        HttpEntity entity = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<List<TemplateAccountingDto>> responseEntity = getTestRestTemplate().exchange(
                getApiUrl().concat("/templates"), HttpMethod.GET, entity,
                new ParameterizedTypeReference<List<TemplateAccountingDto>>() {
                });

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(1, responseEntity.getBody().size());
        assertEquals(1, responseEntity.getBody().get(0).getId());

    }

    @Order(3)
    @Test
    public void getTemplate() {

        HttpEntity entity = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<TemplateAccountingDto> responseEntity = getTestRestTemplate().exchange(getApiUrl().concat("/1"),
                HttpMethod.GET, entity, new ParameterizedTypeReference<TemplateAccountingDto>() {
                });

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(1, responseEntity.getBody().getId());
        assertNotNull(responseEntity.getBody());

    }

    @Order(6)
    @Test
    public void allTemplateLines() {
        HttpEntity entity = new HttpEntity<>(getHttpHeaders());

        ResponseEntity<List<TemplateAccountingDto>> responseEntity = getTestRestTemplate().exchange(
                getApiUrl().concat("/template-lines"), HttpMethod.GET, entity,
                new ParameterizedTypeReference<List<TemplateAccountingDto>>() {
                });

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(1, responseEntity.getBody().size());
        assertEquals(1, responseEntity.getBody().get(0).getId());
    }

    @Order(7)
    @Test
    public void getTemplateByJournal() {

        HttpEntity entity = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<List<TemplateAccountingDto>> responseEntity = getTestRestTemplate().exchange(
                getApiUrl().concat("/searchByJournal/1"), HttpMethod.GET, entity,
                new ParameterizedTypeReference<List<TemplateAccountingDto>>() {
                });

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(1, responseEntity.getBody().size());
        assertEquals(1, responseEntity.getBody().get(0).getId());
    }

    @Order(8)
    @Test
    public void getTemplateLines() {

        HttpEntity entity = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<TemplateAccountingDto> responseEntity = getTestRestTemplate().exchange(
                getApiUrl().concat("/template-lines/1"), HttpMethod.GET, entity,
                new ParameterizedTypeReference<TemplateAccountingDto>() {
                });

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());

    }

    @Order(10)
    @Test
    public void shouldReturnTemplateAccountingLabelExistWhenSave() {

        TemplateAccountingDetailsDto templateAccountingDetailsDto = new TemplateAccountingDetailsDto(null, 1L,
                BigDecimal.ZERO, BigDecimal.ZERO, LABEL);
        List<TemplateAccountingDetailsDto> accountingDetailsDtos = new ArrayList<>();
        accountingDetailsDtos.add(templateAccountingDetailsDto);
        TemplateAccountingDto templateAccountingDto = new TemplateAccountingDto(null, "Template Accounting", 1L,
                JOURNAL_LABEL, accountingDetailsDtos);
        HttpEntity entity = new HttpEntity<>(templateAccountingDto, getHttpHeaders());

        ResponseEntity<LinkedHashMap> responseEntity = getTestRestTemplate().exchange(getApiUrl(), HttpMethod.POST,
                entity, LinkedHashMap.class);

        assertEquals(RestControlAdviser.CUSTOM_STATUS_CODE, responseEntity.getStatusCodeValue());
        assertEquals(TEMPLATE_ACCOUNTING_LABEL_EXISTS, responseEntity.getBody().get(AccountingConstants.ERROR_CODE));

    }

    @Order(11)
    @Test
    public void shouldReturnTemplateAccountingWitoutLinesCodeWhenSave() {
        List<TemplateAccountingDetailsDto> accountingDetailsDtos = new ArrayList<>();

        TemplateAccountingDto templateAccountingDto = new TemplateAccountingDto(null, "Template", 1L, JOURNAL_LABEL,
                accountingDetailsDtos);
        HttpEntity entity = new HttpEntity<>(templateAccountingDto, getHttpHeaders());

        ResponseEntity<LinkedHashMap> responseEntity = getTestRestTemplate().exchange(getApiUrl(), HttpMethod.POST,
                entity, LinkedHashMap.class);

        assertEquals(RestControlAdviser.CUSTOM_STATUS_CODE, responseEntity.getStatusCodeValue());
        assertEquals(TEMPLATE_ACCOUNTING_WITHOUT_LINES_CODE,
                responseEntity.getBody().get(AccountingConstants.ERROR_CODE));

    }

    @Order(12)
    @Test
    public void shouldReturnDocumentAccountLineWithBothDebitAndCreditWhenSave() {
        TemplateAccountingDetailsDto templateAccountingDetailsDto = new TemplateAccountingDetailsDto(null, 1L,
                BigDecimal.valueOf(ZERO_POINT_TWENTY_SIX), BigDecimal.valueOf(ZERO_POINT_THREE_TWO_THREE_SIX), LABEL);
        List<TemplateAccountingDetailsDto> accountingDetailsDtos = new ArrayList<>();
        accountingDetailsDtos.add(templateAccountingDetailsDto);
        TemplateAccountingDto templateAccountingDto = new TemplateAccountingDto(null, "Test template account details",
                1L, JOURNAL_LABEL, accountingDetailsDtos);
        HttpEntity entity = new HttpEntity<>(templateAccountingDto, getHttpHeaders());

        ResponseEntity<LinkedHashMap> responseEntity = getTestRestTemplate().exchange(getApiUrl(), HttpMethod.POST,
                entity, LinkedHashMap.class);

        assertEquals(RestControlAdviser.CUSTOM_STATUS_CODE, responseEntity.getStatusCodeValue());
        assertEquals(DOCUMENT_ACCOUNT_LINE_WITH_BOTH_DEBIT_AND_CREDIT,
                responseEntity.getBody().get(AccountingConstants.ERROR_CODE));

    }

    @Order(13)
    @Test
    public void delete() {

        int id = 1;
        HttpEntity entity = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<Boolean> responseEntity = getTestRestTemplate().exchange(getApiUrl().concat("/" + id),
                HttpMethod.DELETE, entity, new ParameterizedTypeReference<Boolean>() {
                });
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());

    }

    private String getApiUrl() {
        return getRootUrl() + "/api/accounting/template-accounting";
    }

}
