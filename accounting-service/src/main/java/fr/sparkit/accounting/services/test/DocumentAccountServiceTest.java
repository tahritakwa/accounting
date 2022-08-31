package fr.sparkit.accounting.services.test;

import static fr.sparkit.accounting.util.errors.ApiErrors.Accounting.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import fr.sparkit.accounting.dao.DocumentAccountDao;
import fr.sparkit.accounting.dto.AccountingConfigurationDto;
import fr.sparkit.accounting.dto.DocumentAccountLineDto;
import fr.sparkit.accounting.dto.DocumentAccountingDto;
import fr.sparkit.accounting.dto.FiscalYearDto;
import fr.sparkit.accounting.entities.DocumentAccount;
import fr.sparkit.accounting.entities.DocumentAccountLine;
import fr.sparkit.accounting.entities.FiscalYear;
import fr.sparkit.accounting.entities.Journal;
import fr.sparkit.accounting.enumuration.DocumentAccountStatus;
import fr.sparkit.accounting.services.IAccountingConfigurationService;
import fr.sparkit.accounting.services.IDocumentAccountLineService;
import fr.sparkit.accounting.services.IDocumentAccountService;
import fr.sparkit.accounting.services.IFiscalYearService;
import fr.sparkit.accounting.services.IJournalService;
import fr.sparkit.accounting.services.impl.DocumentAccountService;
import fr.sparkit.accounting.util.http.HttpCustomException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DocumentAccountServiceTest {

    @InjectMocks
    private DocumentAccountService documentAccountService;
    @Mock
    private IFiscalYearService fiscalYearService;
    @Mock
    private IAccountingConfigurationService accountCfgService;
    @Mock
    private DocumentAccountDao documentAccountDao;
    @Mock
    private IDocumentAccountService mockDocumentAccountService;
    @Mock
    private IJournalService journalService;
    @Mock
    private IDocumentAccountLineService documentAccountLineService;

    private static final String FISCAL_YEAR_NAME = "2020";
    private static final String CODE_DOCUMENT = "code01";
    private static final String ANY_STRING = "ANY_STRING";
    private static final Long ID_ENTITY = 1L;
    private static final boolean MUST_BE_TRUE = true;
    private static final boolean MUST_BE_FALSE = false;

    private static DocumentAccountingDto documentAccountingDto;
    private static DocumentAccount documentAccount;
    private static Journal journal;
    private static FiscalYearDto fiscalYearDto;
    private static FiscalYear fiscalYear;
    private static AccountingConfigurationDto currentConfiguration;
    private static LocalDateTime now = LocalDateTime.now();
    private static Long journalId = 1L;

    public DocumentAccountServiceTest() {
        super();
    }

    static void loadDocumentAccountingData() {
        documentAccountingDto = new DocumentAccountingDto(ID_ENTITY, now, ANY_STRING, CODE_DOCUMENT, null, null,
                journalId, ANY_STRING, new ArrayList<>(), null, null,
                DocumentAccountStatus.MANUALLY_CREATED.getIndex());
        documentAccountingDto.getDocumentAccountLines()
                .add(new DocumentAccountLineDto(BigDecimal.ZERO, BigDecimal.TEN));
        documentAccountingDto.getDocumentAccountLines()
                .add(new DocumentAccountLineDto(BigDecimal.TEN, BigDecimal.ZERO));
        documentAccountingDto.getDocumentAccountLines()
                .forEach(documentAccountLineDto -> documentAccountLineDto.setDocumentLineDate(now));
    }

    static void loadData() {
        currentConfiguration = new AccountingConfigurationDto();
        currentConfiguration.setJournalANewId(ID_ENTITY);
        journal = new Journal(ID_ENTITY, "", "", null, false, false);
        fiscalYear = new FiscalYear(ID_ENTITY, FISCAL_YEAR_NAME, null, null, null, null, 0, false, null);
        fiscalYearDto = new FiscalYearDto(ID_ENTITY, "", now, now, null, null, 0);
        loadDocumentAccountingData();
        documentAccount = new DocumentAccount();
        documentAccount.setCodeDocument(CODE_DOCUMENT);
        documentAccount.setFiscalYear(fiscalYear);
    }

    @BeforeAll
    void setUp() {
        MockitoAnnotations.initMocks(this);
        loadData();
    }

    /**
     * saveDocumentAccount
     */
    @Test
    @DisplayName("Save Document Account : succes")
    public final void testSaveDocumentAccount() {
        List<DocumentAccountLine> listDocumentAccountLines = new ArrayList<>();
        // Assert Steps
        when(fiscalYearService.findFiscalYearOfDate(any(LocalDateTime.class))).thenReturn(ID_ENTITY);
        when(accountCfgService.findLastConfig()).thenReturn(currentConfiguration);
        when(journalService.findOne(anyLong())).thenReturn(journal);
        when(fiscalYearService.findOne(anyLong())).thenReturn(fiscalYear);
        Assertions.assertFalse(fiscalYearService.isDateInClosedPeriod(any(LocalDateTime.class), anyLong()));
        when(journalService.findOne(anyLong())).thenReturn(journal);
        when(accountCfgService.getCurrentFiscalYear()).thenReturn(fiscalYearDto);
        when(fiscalYearService.findById(anyLong())).thenReturn(fiscalYearDto);
        when(documentAccountDao.findOne(anyLong())).thenReturn(documentAccount);
        when(documentAccountDao.saveAndFlush(documentAccount)).thenReturn(documentAccount);
        when(documentAccountLineService.findByDocumentAccountId(anyLong())).thenReturn(listDocumentAccountLines);
        // call method
        DocumentAccount documentAccountResult = documentAccountService.saveDocumentAccount(documentAccountingDto, true);
        Assertions.assertEquals(documentAccountingDto.getCodeDocument(), documentAccountResult.getCodeDocument());
    }

    @Test
    @DisplayName("Save a DocumentAccount in a closed period")
    public void testSaveDocumentAccountWhenDocumentAccountInClosedPeriod() {
        // Assert Steps
        when(fiscalYearService.findFiscalYearOfDate(any(LocalDateTime.class))).thenReturn(ID_ENTITY);
        when(accountCfgService.findLastConfig()).thenReturn(currentConfiguration);
        when(journalService.findOne(anyLong())).thenReturn(journal);
        when(fiscalYearService.findOne(anyLong())).thenReturn(fiscalYear);
        when(fiscalYearService.isDateInClosedPeriod(any(LocalDateTime.class), anyLong())).thenReturn(true);
        // call method
        HttpCustomException exception = Assertions.assertThrows(HttpCustomException.class,
                () -> documentAccountService.saveDocumentAccount(documentAccountingDto, true));
        Assertions.assertEquals(DOCUMENT_ACCOUNT_DATE_IN_CLOSED_PERIOD, exception.getErrorCode());
    }

    @Test
    @DisplayName("Save a DocumentAccount with incorrect amount")
    public void testSaveDocumentAccountWithIncorrectAmount() {
        DocumentAccountingDto docAccDto = documentAccountingDto;
        docAccDto.getDocumentAccountLines().clear();
        // Assert Steps
        when(fiscalYearService.findFiscalYearOfDate(any(LocalDateTime.class))).thenReturn(ID_ENTITY);
        when(accountCfgService.findLastConfig()).thenReturn(currentConfiguration);
        when(journalService.findOne(anyLong())).thenReturn(journal);
        when(fiscalYearService.findOne(anyLong())).thenReturn(fiscalYear);
        Assertions.assertFalse(fiscalYearService.isDateInClosedPeriod(any(LocalDateTime.class), anyLong()));
        Assertions.assertFalse(documentAccountService.isValidDocumentAccount(docAccDto));
        // call method
        HttpCustomException exception = Assertions.assertThrows(HttpCustomException.class,
                () -> documentAccountService.saveDocumentAccount(docAccDto, true));
        Assertions.assertEquals(DOCUMENT_ACCOUNT_DATE_IN_CLOSED_PERIOD, exception.getErrorCode());
    }

    @Test
    @DisplayName("DocumentAccount not found")
    public void testSaveDocumentAccountOldIsNull() {
        // Assert Steps
        when(fiscalYearService.findFiscalYearOfDate(any(LocalDateTime.class))).thenReturn(ID_ENTITY);
        when(accountCfgService.findLastConfig()).thenReturn(currentConfiguration);
        when(journalService.findOne(anyLong())).thenReturn(journal);
        when(fiscalYearService.findOne(anyLong())).thenReturn(fiscalYear);
        Assertions.assertFalse(fiscalYearService.isDateInClosedPeriod(any(LocalDateTime.class), anyLong()));
        when(journalService.findOne(anyLong())).thenReturn(journal);
        when(accountCfgService.getCurrentFiscalYear()).thenReturn(fiscalYearDto);
        when(fiscalYearService.findById(any())).thenReturn(fiscalYearDto);
        when(documentAccountDao.findOne(documentAccountingDto.getId())).thenReturn(null);
        // call method
        HttpCustomException exception = Assertions.assertThrows(HttpCustomException.class,
                () -> documentAccountService.saveDocumentAccount(documentAccountingDto, true));
        Assertions.assertEquals(ENTITY_DOCUMENT_ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Document Account found is created from Moving To next FiscalYear")
    public void testSaveDAccountL3IsCreatedFromMovingToNextFiscalYear() {
        // preparation Data values
        documentAccount.setIndexOfStatus(DocumentAccountStatus.BY_CONCLUDING_CURRENT_FISCAL_YEAR_IS_CREATED.getIndex());
        // Assert Steps
        when(fiscalYearService.findFiscalYearOfDate(any(LocalDateTime.class))).thenReturn(ID_ENTITY);
        when(accountCfgService.findLastConfig()).thenReturn(currentConfiguration);
        when(journalService.findOne(anyLong())).thenReturn(journal);
        when(fiscalYearService.findOne(anyLong())).thenReturn(fiscalYear);
        Assertions.assertFalse(fiscalYearService.isDateInClosedPeriod(any(LocalDateTime.class), anyLong()));
        when(journalService.findOne(anyLong())).thenReturn(journal);
        when(accountCfgService.getCurrentFiscalYear()).thenReturn(fiscalYearDto);
        when(fiscalYearService.findById(any())).thenReturn(fiscalYearDto);
        when(documentAccountDao.findOne(documentAccountingDto.getId())).thenReturn(documentAccount);
        // call method
        HttpCustomException exception = Assertions.assertThrows(HttpCustomException.class,
                () -> documentAccountService.saveDocumentAccount(documentAccountingDto, false));
        Assertions.assertEquals(DOCUMENT_ACCOUNT_COMING_FROM_CLOSING_FISCAL_YEAR_CANNOT_BE_MANUALLY_UPDATED,
                exception.getErrorCode());

    }

    /**
     * isValidDocumentAccount
     */
    @Test
    @DisplayName("Document Account Valid")
    public void testIsValidDocumentAccountMustBeValid() {
        when(mockDocumentAccountService.totalDebitAmountIsEqualToTotalCreditAmount(documentAccountingDto))
                .thenReturn(MUST_BE_TRUE);
        when(mockDocumentAccountService.documentAccountHasValidLines(documentAccountingDto)).thenReturn(MUST_BE_TRUE);
        when(mockDocumentAccountService.documentAccountIsEmpty(documentAccountingDto)).thenReturn(MUST_BE_FALSE);
        // call method
        Assertions.assertTrue(documentAccountService.isValidDocumentAccount(documentAccountingDto));
    }

}
