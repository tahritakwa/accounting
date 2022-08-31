package fr.sparkit.accounting.application.controllers;

import fr.sparkit.accounting.dto.*;
import fr.sparkit.accounting.entities.DocumentAccount;
import fr.sparkit.accounting.entities.DocumentAccountAttachement;
import fr.sparkit.accounting.entities.FiscalYear;
import fr.sparkit.accounting.services.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DocumentAccountControllerTest extends ITConfig {

    public static final String DOCUMENT_ACCOUNT_LINE = "Document account line";
    public static final String REFERENCE = "reference";
    public static final String LETTER = "letter";
    public static final String DOCUMENT_ACCOUNTING = "Document Accounting";
    public static final String TEST_IMPORT_EXCEL = "TEST_IMPORT_EXCEL";
    public static final String SRC_INTEGRATION_TEST_RESOURCES_FILES_TEST_IMPORT_EXCEL_XLSX = "src/integration-test/resources/files/TEST_IMPORT_EXCEL.xlsx";
    @Value("${document.account-attachement.storage-directory}")
    private Path rootLocation;
    @Value("${accounting.excel.storage-directory}")
    private Path excelStoragePath;
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

    @BeforeAll
    public void setUp() {

        FiscalYear fiscalYearDto = new FiscalYear(null, "Fiscal year", LocalDateTime.of(2020, 1, 1, 0, 0),
                LocalDateTime.of(2020, 12, 31, 0, 0), LocalDateTime.of(2020, 3, 31, 0, 0),
                LocalDateTime.of(2020, 3, 31, 0, 0), 1, false, null);

        fiscalYearService.saveAndFlush(fiscalYearDto);
        saveJournals();
        saveChartAccounts();
        saveAccounts();
        saveConfiguration();
    }

    @AfterAll
    public void destroy() {
        deleteDirectory(rootLocation);
        deleteDirectory(excelStoragePath);

    }

    @Order(1)
    @Test
    public void save() {
        DocumentAccountLineDto documentAccountLineDto1 = new DocumentAccountLineDto(null, LocalDateTime.now(),
                DOCUMENT_ACCOUNT_LINE + " 1", REFERENCE, BigDecimal.ZERO, BigDecimal.TEN, 1L, false, LETTER,
                LocalDate.of(2020, 2, 1));
        DocumentAccountLineDto documentAccountLineDto2 = new DocumentAccountLineDto(null, LocalDateTime.now(),
                DOCUMENT_ACCOUNT_LINE + " 1", REFERENCE, BigDecimal.TEN, BigDecimal.ZERO, 1L, false, LETTER,
                LocalDate.of(2020, 2, 1));

        DocumentAccountingDto documentAccountingDto = new DocumentAccountingDto(DOCUMENT_ACCOUNTING,
                LocalDateTime.of(2020, 4, 1, 0, 0), 1L, false,
                new ArrayList<>(Arrays.asList(documentAccountLineDto1, documentAccountLineDto2)));

        HttpEntity<DocumentAccountingDto> documentAccountingDtoHttpEntity = new HttpEntity<>(documentAccountingDto,
                getHttpHeaders());

        ResponseEntity<DocumentAccount> responseEntity = getTestRestTemplate().exchange(getApiUrl(), HttpMethod.POST,
                documentAccountingDtoHttpEntity, DocumentAccount.class);

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
    }

    @Order(2)
    @Test
    public void saveDocumentWithInvalidAmountCode() {
        DocumentAccountLineDto documentAccountLineDto2 = new DocumentAccountLineDto(null, LocalDateTime.now(),
                DOCUMENT_ACCOUNT_LINE + " 1", REFERENCE, BigDecimal.TEN, BigDecimal.ZERO, 1L, false, LETTER,
                LocalDate.of(2020, 3, 15));

        DocumentAccountingDto documentAccountingDto = new DocumentAccountingDto(DOCUMENT_ACCOUNTING,
                LocalDateTime.of(2020, 6, 29, 23, 59), 1L, false,
                new ArrayList<>(Arrays.asList(documentAccountLineDto2)));

        HttpEntity<DocumentAccountingDto> documentAccountingDtoHttpEntity = new HttpEntity<>(documentAccountingDto,
                getHttpHeaders());

        ResponseEntity<LinkedHashMap> responseEntity = getTestRestTemplate().exchange(getApiUrl(), HttpMethod.POST,
                documentAccountingDtoHttpEntity, LinkedHashMap.class);

        assertEquals(223, responseEntity.getStatusCodeValue());
        assertEquals(20100, responseEntity.getBody().get("errorCode"));

    }

    @Order(3)
    @Test
    public void saveDocumentWithDateInClosingPeriod() {
        DocumentAccountLineDto documentAccountLineDto2 = new DocumentAccountLineDto(null, LocalDateTime.now(),
                DOCUMENT_ACCOUNT_LINE + " 1", REFERENCE, BigDecimal.TEN, BigDecimal.ZERO, 1L, false, LETTER,
                LocalDate.of(2020, 3, 15));

        DocumentAccountingDto documentAccountingDto = new DocumentAccountingDto(DOCUMENT_ACCOUNTING,
                LocalDateTime.of(2020, 3, 29, 23, 59), 1L, false,
                new ArrayList<>(Arrays.asList(documentAccountLineDto2)));

        HttpEntity<DocumentAccountingDto> documentAccountingDtoHttpEntity = new HttpEntity<>(documentAccountingDto,
                getHttpHeaders());

        ResponseEntity<LinkedHashMap> responseEntity = getTestRestTemplate().exchange(getApiUrl(), HttpMethod.POST,
                documentAccountingDtoHttpEntity, LinkedHashMap.class);

        assertEquals(223, responseEntity.getStatusCodeValue());
        assertEquals(20105, responseEntity.getBody().get("errorCode"));

    }

    @Order(4)
    @Test
    public void getDocumentAccount() {

        HttpEntity documentAccountingDto = new HttpEntity(getHttpHeaders());
        ResponseEntity<DocumentAccountingDto> responseEntity = getTestRestTemplate().exchange(getApiUrl().concat("/1"),
                HttpMethod.GET, documentAccountingDto, DocumentAccountingDto.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
    }

    @Order(5)
    @Test
    public void getAllDocumentAccount() {

        HttpEntity documentAccountingDto = new HttpEntity(getHttpHeaders());
        ResponseEntity<List<DocumentAccount>> responseEntity = getTestRestTemplate().exchange(
                getApiUrl().concat("/documents-account"), HttpMethod.GET, documentAccountingDto,
                new ParameterizedTypeReference<List<DocumentAccount>>() {
                });

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(1, responseEntity.getBody().size());
    }

    @Order(6)
    @Test
    public void getCodeDocument() {
        HttpEntity documentAccountingDto = new HttpEntity(getHttpHeaders());
        ResponseEntity<DocumentAccountingDto> responseEntity = getTestRestTemplate().exchange(
                getApiUrl().concat("/code-documents")
                        .concat("?documentDate=" + LocalDateTime.of(2020, 3, 1, 0, 0)
                                .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"))),
                HttpMethod.GET, documentAccountingDto, DocumentAccountingDto.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getStatusCode());
    }

    @Order(7)
    @Test
    public void exportModel() {
        HttpEntity documentAccountingDto = new HttpEntity(getHttpHeaders());
        ResponseEntity<String> responseEntity = getTestRestTemplate().exchange(getApiUrl().concat("/excel-template"),
                HttpMethod.GET, documentAccountingDto, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
    }

    @Order(9)
    @Test
    public void update() {
        DocumentAccountLineDto documentAccountLineDto1 = new DocumentAccountLineDto(null, LocalDateTime.now(),
                DOCUMENT_ACCOUNT_LINE + " 1", REFERENCE, BigDecimal.ZERO, BigDecimal.TEN, 1L, false, LETTER,
                LocalDate.of(2020, 2, 1));
        DocumentAccountLineDto documentAccountLineDto2 = new DocumentAccountLineDto(null, LocalDateTime.now(),
                DOCUMENT_ACCOUNT_LINE + " 1", REFERENCE, BigDecimal.TEN, BigDecimal.ZERO, 1L, false, LETTER,
                LocalDate.of(2020, 2, 1));
        DocumentAccountingDto documentAccountingDto = new DocumentAccountingDto(1L, LocalDateTime.of(2020, 4, 1, 0, 0),
                "New Document Accounting", null, BigDecimal.TEN, null, 1L, null,
                new ArrayList<>(Arrays.asList(documentAccountLineDto1, documentAccountLineDto2)), 2020L, false, null,
                false);

        HttpEntity<DocumentAccountingDto> documentAccountingDtoHttpEntity = new HttpEntity<>(documentAccountingDto,
                getHttpHeaders());

        ResponseEntity<DocumentAccount> responseEntity = getTestRestTemplate().exchange(getApiUrl(), HttpMethod.POST,
                documentAccountingDtoHttpEntity, DocumentAccount.class);

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertEquals("New Document Accounting", responseEntity.getBody().getLabel());
    }

    @Order(11)
    @Test
    public void importDocumentAccountsFromExcelFile() {

        FileUploadDto fileUploadDto = new FileUploadDto(
                convertFileToBase64(SRC_INTEGRATION_TEST_RESOURCES_FILES_TEST_IMPORT_EXCEL_XLSX), TEST_IMPORT_EXCEL);
        HttpEntity<FileUploadDto> documentAccountingDto = new HttpEntity<>(fileUploadDto, getHttpHeaders());
        ResponseEntity<LinkedHashMap> responseEntity = getTestRestTemplate().exchange(
                getApiUrl().concat("/import-documents"), HttpMethod.POST, documentAccountingDto, LinkedHashMap.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertNotNull(responseEntity.getBody().get("name"));
        assertNotNull(responseEntity.getBody().get("base64Content"));
    }

    @Order(12)
    @Test
    public void exportDocumentsAsExcelFile() {
        HttpEntity documentAccountingDto = new HttpEntity(getHttpHeaders());
        ResponseEntity<String> responseEntity = getTestRestTemplate().exchange(getApiUrl().concat("/export-documents"),
                HttpMethod.GET, documentAccountingDto, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
    }

    @Order(13)
    @Test
    public void uploadDocumentAccountAttachements() {

        FileDto fileDto = new FileDto(null,
                convertFileToBase64(SRC_INTEGRATION_TEST_RESOURCES_FILES_TEST_IMPORT_EXCEL_XLSX), TEST_IMPORT_EXCEL,
                1L);
        List<FileDto> fileUploadDtos = new ArrayList<>();
        fileUploadDtos.add(fileDto);
        HttpEntity<List<FileDto>> documentAccountingDto = new HttpEntity<>(fileUploadDtos, getHttpHeaders());
        ResponseEntity<LinkedHashMap> responseEntity = getTestRestTemplate().exchange(
                getApiUrl().concat("/upload-document-account-attachement"), HttpMethod.POST, documentAccountingDto,
                LinkedHashMap.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Order(14)
    @Test
    public void getDocumentAccountAttachements() {

        FileDto fileDto = new FileDto(null,
                convertFileToBase64(SRC_INTEGRATION_TEST_RESOURCES_FILES_TEST_IMPORT_EXCEL_XLSX), TEST_IMPORT_EXCEL,
                1L);
        List<FileDto> fileUploadDtos = new ArrayList<>();
        fileUploadDtos.add(fileDto);
        HttpEntity<List<FileDto>> documentAccountingDto = new HttpEntity<>(fileUploadDtos, getHttpHeaders());
        ResponseEntity<List<DocumentAccountAttachement>> responseEntity = getTestRestTemplate().exchange(
                getApiUrl().concat("/document-account-attachement/1"), HttpMethod.GET, documentAccountingDto,
                new ParameterizedTypeReference<List<DocumentAccountAttachement>>() {
                });

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(1, responseEntity.getBody().size());
        assertNotNull(responseEntity.getBody().get(0).getDocumentAccount());
        assertEquals(1L, responseEntity.getBody().get(0).getDocumentAccount().getId());
    }

    private String getApiUrl() {
        return getRootUrl().concat("/api/accounting/document");
    }

    private String convertFileToBase64(String filePath) {
        File originalFile = new File(filePath);
        String encodedBase64 = null;
        try (FileInputStream fileInputStreamReader = new FileInputStream(originalFile)) {
            byte[] bytes = new byte[(int) originalFile.length()];
            fileInputStreamReader.read(bytes);
            encodedBase64 = new String(Base64.encodeBase64(bytes));
        } catch (IOException e) {
            log.error(String.valueOf(e));
        }
        return encodedBase64;

    }

    private void saveAccounts() {
        List<AccountDto> accountDtos = new ArrayList<>();
        accountDtos.add(new AccountDto(null, 25626565, "Résultat de l'exercice 1", 1L, 2, BigDecimal.ONE,
                BigDecimal.ONE, true, true, 1L, false));
        accountDtos.add(new AccountDto(null, 25626566, "Résultat de l'exercice 2", 1L, 2, BigDecimal.ONE,
                BigDecimal.ONE, true, true, 1L, false));
        accountDtos.add(new AccountDto(null, 25626567, "Résultat de l'exercice 3", 1L, 2, BigDecimal.ONE,
                BigDecimal.ONE, true, true, 1L, false));
        accountDtos.add(new AccountDto(null, 25626568, "Résultat de l'exercice 4", 1L, 2, BigDecimal.ONE,
                BigDecimal.ONE, true, true, 1L, false));
        accountDtos.forEach(d -> accountService.saveAccount(d));
    }

    private void saveJournals() {
        List<JournalDto> journalDtos = new ArrayList<>();
        journalDtos.add(new JournalDto(null, "1234", "JournalANewId", LocalDateTime.now(), true));
        journalDtos.add(new JournalDto(null, "1235", "JournalSalesIdl", LocalDateTime.now(), true));
        journalDtos.add(new JournalDto(null, "1236", "JournalPurchasesId", LocalDateTime.now(), true));
        journalDtos.add(new JournalDto(null, "1237", "JournalCofferId", LocalDateTime.now(), true));
        journalDtos.add(new JournalDto(null, "1238", "JournalBankId", LocalDateTime.now(), true));
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
        AccountingConfigurationDto accountingConfigurationDto = new AccountingConfigurationDto(null, 2L, 1L, 1L, 2L, 3L,
                4L, 5L, 5L, 6L, 7L, 0, 0, 0, 0, 0, 0, 0, 0, 3L, 4L, 1L, 1L, 2L, 3L, 4L, 5L, 7L, 0, 1L, 6L);
        accountingConfigurationDto.setJournalANewId(1L);
        accountingConfigurationDto.setFiscalYearId(1L);
        accountingConfigurationDto.setTaxStampIdAccountingAccountPurchase(1L);
        accountingConfigurationDto.setTaxStampIdAccountingAccountSales(2L);
        accountingConfigurationDto.setBalanceSheetOpeningAccount(3L);
        accountingConfigurationDto.setBalanceSheetClosingAccount(4L);

        accountingConfigurationService.saveConfiguration(accountingConfigurationDto);

    }

    private void deleteDirectory(Path path) {
        File file = new File(path.toString());
        File[] files = file.listFiles();
        List<File> fileList = Arrays.asList(files);
        if (file.isDirectory() && file.listFiles().length > 0) {
            fileList.forEach(File::delete);
            file.delete();
        }
    }
}