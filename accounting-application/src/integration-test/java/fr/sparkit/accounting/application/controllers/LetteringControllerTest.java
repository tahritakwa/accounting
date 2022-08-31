package fr.sparkit.accounting.application.controllers;

import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.dto.*;
import fr.sparkit.accounting.entities.DocumentAccountLine;
import fr.sparkit.accounting.entities.FiscalYear;
import fr.sparkit.accounting.services.*;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static fr.sparkit.accounting.util.errors.ApiErrors.Accounting.*;
import static org.junit.jupiter.api.Assertions.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class LetteringControllerTest extends ITConfig {

        public static final String DOCUMENT_ACCOUNT_LINES_FOR_LITERABLE_ACCOUNT = "/document-account-lines-for-literable-account";
        public static final String ACCOUNT_PAGE = "?accountPage=";
        public static final String LITERABLE_LINE_PAGE = "&literableLinePage=";
        public static final String LITERABLE_LINE_PAGE_SIZE = "&literableLinePageSize=";
        public static final String HAVING_LETTERED_LINES = "&havingLetteredLines=";
        public static final String BEGIN_ACCOUNT_CODE = "&beginAccountCode=";
        public static final String END_ACCOUNT_CODE = "&endAccountCode=";
        public static final String START_DATE = "&startDate=";
        public static final String END_DATE = "&endDate=";
        public static final String AUTO_GENERATE_LETTER_TO_LITERABLE_DOCUMENT_ACCOUNT_LINE = "/auto-generate-letter-to-literable-document-account-line";
        @Autowired
        private IJournalService journalService;
        @Autowired
        private IFiscalYearService fiscalYearService;
        @Autowired
        private IAccountService accountService;
        @Autowired
        private IChartAccountsService chartAccountsService;
        @Autowired
        private IAccountingConfigurationService accountingConfigurationService;
        @Autowired
        private IDocumentAccountService documentAccountService;
        @Autowired
        private IDocumentAccountLineService documentAccountLineService;
        @Autowired
        private ILetteringService letteringService;

        @BeforeAll
        void setUp() {
                FiscalYear fiscalYearDto = new FiscalYear(null, "Fiscal year", LocalDateTime.of(2020, 1, 1, 0, 0),
                        LocalDateTime.of(2020, 12, 31, 0, 0), LocalDateTime.of(2020, 3, 31, 0, 0),
                        LocalDateTime.of(2020, 3, 31, 0, 0), 1, false, null);
                fiscalYearService.saveAndFlush(fiscalYearDto);

                saveJournals();
                saveChartAccounts();
                saveAccounts();
                saveConfiguration();
                saveDocumentAccount();
        }

        @Order(1)
        @Test
        public void findDocumentAccountLinesForLiterableAccount() {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(AccountingConstants.YYYY_MM_DD_HH_MM_SS);
                int accountPage = 0;
                int literableLinePage = 0;
                int literableLinePageSize = 10;
                boolean havingLetteredLines = false;
                Long beginAccountCode = 25626565L;
                Long endAccountCode = 25626566L;
                String startDate = LocalDateTime.of(2020, 1, 1, 0, 0, 0).format(formatter);
                String endDate = LocalDateTime.of(2020, 12, 31, 23, 59, 59).format(formatter);
                HttpEntity<List<LiterableDocumentAccountLineDto>> listHttpEntity = new HttpEntity<>(getHttpHeaders());
                ResponseEntity<LiterableDocumentAccountLinePageDto> responseEntity = this.getTestRestTemplate()
                        .exchange(getApiUrl().concat(DOCUMENT_ACCOUNT_LINES_FOR_LITERABLE_ACCOUNT)
                                        .concat(ACCOUNT_PAGE + accountPage).concat(LITERABLE_LINE_PAGE + literableLinePage)
                                        .concat(LITERABLE_LINE_PAGE_SIZE + literableLinePageSize)
                                        .concat(HAVING_LETTERED_LINES + havingLetteredLines)
                                        .concat(BEGIN_ACCOUNT_CODE + beginAccountCode)
                                        .concat(END_ACCOUNT_CODE + endAccountCode).concat(START_DATE + startDate)
                                        .concat(END_DATE + endDate), HttpMethod.GET, listHttpEntity,
                                LiterableDocumentAccountLinePageDto.class);

                assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                assertEquals(2, responseEntity.getBody().getContent().size());
                assertNull(responseEntity.getBody().getContent().get(0).getLetter());
        }

        /**
         * Find document account lines for literable account when start date is after end date
         */
        @Order(2)
        @Test
        public void shouldReturnStartDateIsAfterEndDate() {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(AccountingConstants.YYYY_MM_DD_HH_MM_SS);
                int accountPage = 0;
                int literableLinePage = 0;
                int literableLinePageSize = 10;
                boolean havingLetteredLines = false;
                Long beginAccountCode = null;
                Long endAccountCode = null;
                String startDate = LocalDateTime.of(2020, 1, 1, 0, 0, 0).format(formatter);
                String endDate = LocalDateTime.of(2019, 12, 31, 23, 59, 59).format(formatter);

                HttpEntity<List<LiterableDocumentAccountLineDto>> listHttpEntity = new HttpEntity<>(getHttpHeaders());
                ResponseEntity<LinkedHashMap> responseEntity = this.getTestRestTemplate().exchange(
                        getApiUrl().concat(DOCUMENT_ACCOUNT_LINES_FOR_LITERABLE_ACCOUNT)
                                .concat(ACCOUNT_PAGE + accountPage).concat(LITERABLE_LINE_PAGE + literableLinePage)
                                .concat(LITERABLE_LINE_PAGE_SIZE + literableLinePageSize)
                                .concat(HAVING_LETTERED_LINES + havingLetteredLines)
                                .concat(BEGIN_ACCOUNT_CODE + beginAccountCode)
                                .concat(END_ACCOUNT_CODE + endAccountCode).concat(START_DATE + startDate)
                                .concat(END_DATE + endDate), HttpMethod.GET, listHttpEntity, LinkedHashMap.class);

                assertEquals(223, responseEntity.getStatusCodeValue());
                assertEquals(START_DATE_IS_AFTER_END_DATE, responseEntity.getBody().get(ERROR_CODE));
        }

        /**
         * Find document account lines for literable account when begin account code is greater than end account
         */
        @Order(3)
        @Test
        public void shouldReturnBeginAccountCodeIsGreaterThanEndAccount() {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(AccountingConstants.YYYY_MM_DD_HH_MM_SS);
                int accountPage = 0;
                int literableLinePage = 0;
                int literableLinePageSize = 10;
                boolean havingLetteredLines = false;
                Long beginAccountCode = 25626566L;
                Long endAccountCode = 25626565L;
                String startDate = LocalDateTime.of(2020, 1, 1, 0, 0, 0).format(formatter);
                String endDate = LocalDateTime.of(2020, 12, 31, 23, 59, 59).format(formatter);

                HttpEntity<List<LiterableDocumentAccountLineDto>> listHttpEntity = new HttpEntity<>(getHttpHeaders());
                ResponseEntity<LinkedHashMap> responseEntity = this.getTestRestTemplate().exchange(
                        getApiUrl().concat(DOCUMENT_ACCOUNT_LINES_FOR_LITERABLE_ACCOUNT)
                                .concat(ACCOUNT_PAGE + accountPage).concat(LITERABLE_LINE_PAGE + literableLinePage)
                                .concat(LITERABLE_LINE_PAGE_SIZE + literableLinePageSize)
                                .concat(HAVING_LETTERED_LINES + havingLetteredLines)
                                .concat(BEGIN_ACCOUNT_CODE + beginAccountCode)
                                .concat(END_ACCOUNT_CODE + endAccountCode).concat(START_DATE + startDate)
                                .concat(END_DATE + endDate), HttpMethod.GET, listHttpEntity, LinkedHashMap.class);

                assertEquals(223, responseEntity.getStatusCodeValue());
                assertEquals(BEGIN_ACCOUNT_CODE_IS_GREATER_THAN_END_ACCOUNT, responseEntity.getBody().get(ERROR_CODE));
        }

        @Order(4)
        @Test
        public void autoGenerateLetterToLiterableDocumentAccountLine() {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(AccountingConstants.YYYY_MM_DD_HH_MM_SS);
                int accountPage = 0;
                int literableLinePage = 0;
                int literableLinePageSize = 10;
                boolean havingLetteredLines = false;
                Long beginAccountCode = 25626565L;
                Long endAccountCode = 25626566L;
                String startDate = LocalDateTime.of(2020, 1, 1, 0, 0, 0).format(formatter);
                String endDate = LocalDateTime.of(2020, 12, 31, 23, 59, 59).format(formatter);

                HttpEntity<List<LiterableDocumentAccountLineDto>> listHttpEntity = new HttpEntity<>(getHttpHeaders());
                ResponseEntity<LiterableDocumentAccountLinePageDto> responseEntity = this.getTestRestTemplate()
                        .exchange(getApiUrl().concat(AUTO_GENERATE_LETTER_TO_LITERABLE_DOCUMENT_ACCOUNT_LINE)
                                        .concat(ACCOUNT_PAGE + accountPage).concat(LITERABLE_LINE_PAGE + literableLinePage)
                                        .concat(LITERABLE_LINE_PAGE_SIZE + literableLinePageSize)
                                        .concat(HAVING_LETTERED_LINES + havingLetteredLines)
                                        .concat(BEGIN_ACCOUNT_CODE + beginAccountCode)
                                        .concat(END_ACCOUNT_CODE + endAccountCode).concat(START_DATE + startDate)
                                        .concat(END_DATE + endDate), HttpMethod.GET, listHttpEntity,
                                LiterableDocumentAccountLinePageDto.class);

                assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                assertEquals(2, responseEntity.getBody().getContent().size());
                assertNotNull(responseEntity.getBody().getContent().get(0).getLetter());
                assertNotNull(responseEntity.getBody().getContent().get(1).getLetter());
                assertEquals(responseEntity.getBody().getContent().get(1).getLetter(),
                        responseEntity.getBody().getContent().get(0).getLetter());
        }

        /**
         * Auto generate letter to  literable account when begin account code is greater than end account
         */

        @Order(5)
        @Test
        public void shouldReturnBeginAccountCodeIsGreaterThanEndAccountWhenAutoGenarate() {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(AccountingConstants.YYYY_MM_DD_HH_MM_SS);
                int accountPage = 0;
                int literableLinePage = 0;
                int literableLinePageSize = 10;
                boolean havingLetteredLines = false;
                Long beginAccountCode = 25626566L;
                Long endAccountCode = 25626565L;
                String startDate = LocalDateTime.of(2020, 1, 1, 0, 0, 0).format(formatter);
                String endDate = LocalDateTime.of(2020, 12, 31, 23, 59, 59).format(formatter);

                HttpEntity<List<LiterableDocumentAccountLineDto>> listHttpEntity = new HttpEntity<>(getHttpHeaders());
                ResponseEntity<LinkedHashMap> responseEntity = this.getTestRestTemplate().exchange(
                        getApiUrl().concat(AUTO_GENERATE_LETTER_TO_LITERABLE_DOCUMENT_ACCOUNT_LINE)
                                .concat(ACCOUNT_PAGE + accountPage).concat(LITERABLE_LINE_PAGE + literableLinePage)
                                .concat(LITERABLE_LINE_PAGE_SIZE + literableLinePageSize)
                                .concat(HAVING_LETTERED_LINES + havingLetteredLines)
                                .concat(BEGIN_ACCOUNT_CODE + beginAccountCode)
                                .concat(END_ACCOUNT_CODE+ endAccountCode).concat(START_DATE + startDate)
                                .concat(END_DATE + endDate), HttpMethod.GET, listHttpEntity, LinkedHashMap.class);

                assertEquals(223, responseEntity.getStatusCodeValue());
                assertEquals(BEGIN_ACCOUNT_CODE_IS_GREATER_THAN_END_ACCOUNT, responseEntity.getBody().get(ERROR_CODE));

        }

        /**
         * Auto generate letter to  literable account when start date is after end date
         */

        @Order(6)
        @Test
        public void shouldReturnStartDateIsAfterEndDatetWhenAutoGenarate() {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(AccountingConstants.YYYY_MM_DD_HH_MM_SS);
                int accountPage = 0;
                int literableLinePage = 0;
                int literableLinePageSize = 10;
                boolean havingLetteredLines = false;
                Long beginAccountCode = 25626565L;
                Long endAccountCode = 25626566L;
                String startDate = LocalDateTime.of(2020, 1, 1, 0, 0, 0).format(formatter);
                String endDate = LocalDateTime.of(2019, 12, 31, 23, 59, 59).format(formatter);

                HttpEntity<List<LiterableDocumentAccountLineDto>> listHttpEntity = new HttpEntity<>(getHttpHeaders());
                ResponseEntity<LinkedHashMap> responseEntity = this.getTestRestTemplate().exchange(
                        getApiUrl().concat(AUTO_GENERATE_LETTER_TO_LITERABLE_DOCUMENT_ACCOUNT_LINE)
                                .concat(ACCOUNT_PAGE + accountPage).concat(LITERABLE_LINE_PAGE + literableLinePage)
                                .concat(LITERABLE_LINE_PAGE_SIZE + literableLinePageSize)
                                .concat(HAVING_LETTERED_LINES + havingLetteredLines)
                                .concat(BEGIN_ACCOUNT_CODE + beginAccountCode)
                                .concat(END_ACCOUNT_CODE+ endAccountCode).concat("&startDate=" + startDate)
                                .concat("&endDate=" + endDate), HttpMethod.GET, listHttpEntity, LinkedHashMap.class);

                assertEquals(223, responseEntity.getStatusCodeValue());
                assertEquals(START_DATE_IS_AFTER_END_DATE, responseEntity.getBody().get(ERROR_CODE));
        }

        @Order(7)
        @Test
        public void saveLettersToSelectedLiterableDocumentAccountLine() {

                List<DocumentAccountLine> documentAccountLines = documentAccountLineService.findByDocumentAccountId(1L);
                List<LiterableDocumentAccountLineDto> literableDocumentAccountLineDtoList = new ArrayList<>();
                String letter = letteringService.generateFirstUnusedLetter();
                documentAccountLines.forEach(d -> literableDocumentAccountLineDtoList
                        .add(new LiterableDocumentAccountLineDto(d.getId(), "25626565  Résultat de l'exercice 1",
                                LocalDateTime.now(), "05/326", d.getDocumentAccount().getJournal().getLabel(),
                                d.getReference(), d.getDebitAmount(), d.getCreditAmount(), BigDecimal.ZERO, letter,
                                1L)));

                HttpEntity<List<LiterableDocumentAccountLineDto>> listHttpEntity = new HttpEntity<>(
                        literableDocumentAccountLineDtoList, getHttpHeaders());
                ResponseEntity<List<LiterableDocumentAccountLineDto>> responseEntity = this.getTestRestTemplate()
                        .exchange(getApiUrl(), HttpMethod.POST, listHttpEntity,
                                new ParameterizedTypeReference<List<LiterableDocumentAccountLineDto>>() {
                                });

                assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                assertNotNull(responseEntity.getBody());
                assertEquals(2, responseEntity.getBody().size());
        }

        @Order(8)
        @Test
        public void saveLettersToSelectedLiterableDocumentAccountLineWhenCreditDifferentDebit() {

                List<DocumentAccountLine> documentAccountLines = documentAccountLineService.findByDocumentAccountId(1L);
                List<LiterableDocumentAccountLineDto> literableDocumentAccountLineDtoList = new ArrayList<>();
                literableDocumentAccountLineDtoList
                        .add(new LiterableDocumentAccountLineDto(documentAccountLines.get(0).getId(),
                                "25626566 Résultat de l'exercice 2", LocalDateTime.now(), "05/326",
                                documentAccountLines.get(0).getDocumentAccount().getJournal().getLabel(),
                                documentAccountLines.get(0).getReference(),
                                documentAccountLines.get(0).getDebitAmount(),
                                documentAccountLines.get(0).getCreditAmount(), BigDecimal.ZERO,
                                documentAccountLines.get(0).getLetter(), 1L));

                HttpEntity<List<LiterableDocumentAccountLineDto>> listHttpEntity = new HttpEntity<>(
                        literableDocumentAccountLineDtoList, getHttpHeaders());
                ResponseEntity<LinkedHashMap> responseEntity = this.getTestRestTemplate()
                        .exchange(getApiUrl(), HttpMethod.POST, listHttpEntity, LinkedHashMap.class);

                assertEquals(223, responseEntity.getStatusCodeValue());
                assertEquals(TOTAL_DEBIT_SHOULD_BE_EQUAL_TO_TOTAL_CREDIT_FOR_ACCOUNT_AND_LETTER,
                        responseEntity.getBody().get(ERROR_CODE));
        }

        @Order(9)
        @Test
        public void removeLettersFromDeselectedDocumentAccountLine() {

                List<DocumentAccountLine> documentAccountLines = documentAccountLineService.findByDocumentAccountId(1L);
                List<LiterableDocumentAccountLineDto> literableDocumentAccountLineDtoList = new ArrayList<>();
                documentAccountLines.forEach(d -> literableDocumentAccountLineDtoList
                        .add(new LiterableDocumentAccountLineDto(d.getId(), "25626565  Résultat de l'exercice 1",
                                LocalDateTime.now(), "05/326", d.getDocumentAccount().getJournal().getLabel(),
                                d.getReference(), d.getDebitAmount(), d.getCreditAmount(), BigDecimal.ZERO,
                                d.getLetter(), 1L)));

                HttpEntity<List<LiterableDocumentAccountLineDto>> listHttpEntity = new HttpEntity<>(
                        literableDocumentAccountLineDtoList, getHttpHeaders());
                ResponseEntity<List<LiterableDocumentAccountLineDto>> responseEntity = this.getTestRestTemplate()
                        .exchange(getApiUrl().concat("/remove-letter-from-deselected-document-account-line"),
                                HttpMethod.POST, listHttpEntity,
                                new ParameterizedTypeReference<List<LiterableDocumentAccountLineDto>>() {
                                });

                assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                assertNotNull(responseEntity.getBody());
                assertEquals(2, responseEntity.getBody().size());
        }

        @Order(10)
        @Test
        public void ShoulReturnDocumentAcountLineNotFoundWhenRemoveLetters() {

                List<DocumentAccountLine> documentAccountLines = documentAccountLineService.findByDocumentAccountId(1L);
                List<LiterableDocumentAccountLineDto> literableDocumentAccountLineDtoList = new ArrayList<>();
                documentAccountLines.forEach(d -> literableDocumentAccountLineDtoList
                        .add(new LiterableDocumentAccountLineDto(null, "25626565  Résultat de l'exercice 1",
                                LocalDateTime.now(), "05/326", d.getDocumentAccount().getJournal().getLabel(),
                                d.getReference(), d.getDebitAmount(), d.getCreditAmount(), BigDecimal.ZERO,
                                d.getLetter(), 1L)));

                HttpEntity<List<LiterableDocumentAccountLineDto>> listHttpEntity = new HttpEntity<>(
                        literableDocumentAccountLineDtoList, getHttpHeaders());
                ResponseEntity<LinkedHashMap> responseEntity = this.getTestRestTemplate()
                        .exchange(getApiUrl().concat("/remove-letter-from-deselected-document-account-line"),
                                HttpMethod.POST, listHttpEntity, new ParameterizedTypeReference<LinkedHashMap>() {
                                });

                assertEquals(223, responseEntity.getStatusCodeValue());
                assertNotNull(responseEntity.getBody());
                assertEquals(DOCUMENT_ACCOUNT_LINE_NOT_FOUND, responseEntity.getBody().get(ERROR_CODE));
        }

        @Order(11)
        @Test
        public void generateFirstNotUsedLetter() {

                HttpEntity<List<LiterableDocumentAccountLineDto>> listHttpEntity = new HttpEntity<>(getHttpHeaders());
                ResponseEntity<LiterableDocumentAccountLineDto> responseEntity = this.getTestRestTemplate()
                        .exchange(getApiUrl().concat("/generate-letter-code"), HttpMethod.GET, listHttpEntity,
                                LiterableDocumentAccountLineDto.class);

                assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                assertNotNull(responseEntity.getBody());
                assertNotNull(responseEntity.getBody().getLetter());
        }

        private String getApiUrl() {
                return getRootUrl().concat("/api/accounting/lettering");
        }

        private void saveAccounts() {
                List<AccountDto> accountDtos = new ArrayList<>(Arrays.asList(
                        new AccountDto(null, 25626565, "Résultat de l'exercice 1", 1L, 2, BigDecimal.ONE,
                                BigDecimal.ONE, true, true, 1L, false),
                        new AccountDto(null, 25626566, "Résultat de l'exercice 2", 1L, 2, BigDecimal.ONE,
                                BigDecimal.ONE, true, true, 1L, false),
                        new AccountDto(null, 25626567, "Résultat de l'exercice 3", 1L, 2, BigDecimal.ONE,
                                BigDecimal.ONE, true, true, 1L, false),
                        new AccountDto(null, 25626568, "Résultat de l'exercice 4", 1L, 2, BigDecimal.ONE,
                                BigDecimal.ONE, true, true, 1L, false)));
                accountDtos.forEach(d -> accountService.saveAccount(d));
        }

        private void saveJournals() {
                List<JournalDto> journalDtos = new ArrayList<>(
                        Arrays.asList(new JournalDto(null, "1234", "JournalANewId", LocalDateTime.now(), true),
                                new JournalDto(null, "1235", "JournalAccount", LocalDateTime.now(), true),
                                new JournalDto(null, "1236", "JournalPurchasesId", LocalDateTime.now(), true),
                                new JournalDto(null, "1237", "JournalCofferId", LocalDateTime.now(), true),
                                new JournalDto(null, "1238", "JournalBankId", LocalDateTime.now(), true),
                                new JournalDto(null, "1239", "JournalSalesIdl", LocalDateTime.now(), true)));

                journalDtos.forEach(d -> journalService.save(d));
        }

        private void saveChartAccounts() {
                List<ChartAccountsDto> chartAccountsDtos = new ArrayList<>();
                List<ChartAccountsDto> childrenList = new ArrayList<>();
                ChartAccountsDto children = new ChartAccountsDto(null, 1L, 1, "CofferIdAccountingAccount", null);
                childrenList.add(children);
                chartAccountsDtos.add(new ChartAccountsDto(null, null, 2, "BankIdAccountingAccount", childrenList));
                chartAccountsDtos.add(new ChartAccountsDto(null, null, 3, "CustomerAccount", childrenList));
                chartAccountsDtos.add(new ChartAccountsDto(null, null, 4, "SupplierAccount", childrenList));
                chartAccountsDtos.add(new ChartAccountsDto(null, null, 5, "TaxSalesAccount", childrenList));
                chartAccountsDtos.add(new ChartAccountsDto(null, null, 6, "HtaxSalesAccount", childrenList));
                chartAccountsDtos.add(new ChartAccountsDto(null, null, 7, "TaxPurchasesAccount", childrenList));
                chartAccountsDtos.add(new ChartAccountsDto(null, null, 8, "HtaxPurchasesAccount", childrenList));
                chartAccountsDtos.forEach(d -> chartAccountsService.save(d));
        }

        private void saveConfiguration() {
                AccountingConfigurationDto accountingConfigurationDto = new AccountingConfigurationDto(null, 2L, 1L, 1L,
                        2L, 3L, 4L, 5L, 5L, 6L, 7L, 0, 0, 0, 0, 0, 0, 0, 0, 3l, 4L, 1L, 1L, 6L, 3L, 4L, 5L, 7L, 0, 1L,
                        6L);
                accountingConfigurationDto.setJournalANewId(1L);
                accountingConfigurationDto.setFiscalYearId(1L);
                accountingConfigurationDto.setTaxStampIdAccountingAccountPurchase(1L);
                accountingConfigurationDto.setTaxStampIdAccountingAccountSales(2L);
                accountingConfigurationDto.setBalanceSheetOpeningAccount(3L);
                accountingConfigurationDto.setBalanceSheetClosingAccount(4L);
                accountingConfigurationService.saveConfiguration(accountingConfigurationDto);
        }

        private void saveDocumentAccount() {
                DocumentAccountLineDto firstDocumentAccountLineDto = new DocumentAccountLineDto(null,
                        LocalDateTime.now(), "Document account line", "AAZ", BigDecimal.ZERO, BigDecimal.TEN, 1L, false,
                        null, LocalDate.of(2020, 5, 1));
                DocumentAccountLineDto secondDocumentAccountLineDto = new DocumentAccountLineDto(null,
                        LocalDateTime.now(), "Document account line", "AAZ", BigDecimal.TEN, BigDecimal.ZERO, 1L, false,
                        null, LocalDate.of(2020, 5, 1));
                DocumentAccountingDto documentAccountingDto = new DocumentAccountingDto("Document Accounting 1",
                        LocalDateTime.of(2020, 5, 1, 0, 0), 1L, false,
                        new ArrayList<>(Arrays.asList(firstDocumentAccountLineDto, secondDocumentAccountLineDto)));
                documentAccountService.saveDocumentAccount(documentAccountingDto, false);
        }

}