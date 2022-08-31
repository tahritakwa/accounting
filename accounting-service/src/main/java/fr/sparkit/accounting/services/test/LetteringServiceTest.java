package fr.sparkit.accounting.services.test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import fr.sparkit.accounting.services.IFiscalYearService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import fr.sparkit.accounting.constants.NumberConstant;
import fr.sparkit.accounting.dao.DocumentAccountLineDao;
import fr.sparkit.accounting.dto.AccountingConfigurationDto;
import fr.sparkit.accounting.dto.DocumentAccountingDto;
import fr.sparkit.accounting.dto.LiterableDocumentAccountLineDto;
import fr.sparkit.accounting.dto.LiterableDocumentAccountLinePageDto;
import fr.sparkit.accounting.entities.Account;
import fr.sparkit.accounting.entities.DocumentAccount;
import fr.sparkit.accounting.entities.DocumentAccountLine;
import fr.sparkit.accounting.entities.Journal;
import fr.sparkit.accounting.services.IAccountingConfigurationService;
import fr.sparkit.accounting.services.IDocumentAccountService;
import fr.sparkit.accounting.services.impl.LetteringService;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.http.HttpCustomException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LetteringServiceTest {
    @InjectMocks
    private LetteringService letteringService;
    @Mock
    private DocumentAccountLineDao documentAccountLineDao;
    @Mock
    private IAccountingConfigurationService accountingConfigurationService;
    @Mock
    private IDocumentAccountService documentAccountService;
    @Mock
    private IFiscalYearService fiscalYearService;

    private static final String CODE_DOCUMENT = "code10";
    private static final String JOURNAL_LABEL = "code10";
    private static final String ACCOUNT_LABEL = "LABEL";
    private static final boolean MUST_BE_TRUE = true;
    private static final boolean MUST_BE_FALSE = false;
    private static final String ACCOUNT_CODE_BEGIN = "10";
    private static final String ACCOUNT_CODE_END = "20";
    private static final int ACCOUNT_CODE_BEGIN_I = 10;
    private static final int ACCOUNT_CODE_END_I = 20;

    private LocalDateTime before;
    private LocalDateTime after;
    private Pageable pageable;
    private Page<Integer> page;
    private List<DocumentAccountLine> documentAccountLines;
    private DocumentAccount documentAccount;
    private Account account;
    private Page<DocumentAccountLine> documentAccountLinesPage;
    private Pageable pageableAccountLine;
    private String field;
    private String direction;

    public static final Long ID_ENTITY = 1L;
    public static final int ZERO = 0;
    public static final int ONE = 1;
    public static final int TWO = 2;

    public LetteringServiceTest() {
        super();
    }

    final void loadDocumentAccountLines() {
        documentAccountLines = new ArrayList<>();
        documentAccountLines.add(new DocumentAccountLine());
        documentAccountLines.add(new DocumentAccountLine());
        documentAccountLines.get(ZERO).setDebitAmount(BigDecimal.TEN);
        documentAccountLines.get(ZERO).setCreditAmount(BigDecimal.ZERO);
        documentAccountLines.get(ONE).setDebitAmount(BigDecimal.ZERO);
        documentAccountLines.get(ONE).setCreditAmount(BigDecimal.TEN);
    }

    final void loadData() {
        before = LocalDateTime.now();
        after = LocalDateTime.now().plusDays(1);
        field = "";
        direction = "";
        pageable = PageRequest.of(1, 1);
        page = new PageImpl<>(Arrays.asList(NumberConstant.ONE, NumberConstant.TWO, NumberConstant.THREE,
                NumberConstant.FOUR, NumberConstant.FIVE, NumberConstant.SIX, NumberConstant.SEVEN,
                NumberConstant.EIGHT, NumberConstant.NINE), pageable, NumberConstant.NINE);
        loadDocumentAccountLines();
        pageableAccountLine = PageRequest.of(ONE, ONE);
        documentAccountLinesPage = new PageImpl<>(documentAccountLines, pageableAccountLine, TWO);
        documentAccount = new DocumentAccount();
        Journal journal = new Journal();
        documentAccount.setJournal(journal);
        documentAccount.setDocumentDate(LocalDateTime.now());
        documentAccount.setCodeDocument(CODE_DOCUMENT);
        journal.setLabel(JOURNAL_LABEL);
        account = new Account();
        account.setCode(ONE);
        account.setLabel(ACCOUNT_LABEL);
        documentAccount.setId(ID_ENTITY);
    }

    @BeforeAll
    void setUp() {
        MockitoAnnotations.initMocks(this);
        loadData();
    }

    /**
     * FindDocumentAccountLinesForLiterableAccount
     **/
    @Test
    @DisplayName("Find DocumentAccountLines For Literable Account When have lettering lines false")
    public void testFindDocumentAccountLinesForLiterableAccount() {
        when(documentAccountLineDao.getLiterableAccountHavingNotLetteredLines(ACCOUNT_CODE_BEGIN_I, ACCOUNT_CODE_END_I,
                before, after, pageable)).thenReturn(page);
        when(documentAccountLineDao.findByAccountCodeAndLetterIsNull(ONE, before, after, pageableAccountLine))
                .thenReturn(documentAccountLinesPage);
        documentAccountLines.forEach((DocumentAccountLine documentAccountLine) -> {
            documentAccountLine.setDocumentAccount(documentAccount);
            documentAccountLine.setAccount(account);
        });
        LiterableDocumentAccountLinePageDto literableDocumentAccountLinePageDto = letteringService
                .findDocumentAccountLinesForLiterableAccount(ONE, ONE, ONE, MUST_BE_FALSE, ACCOUNT_CODE_BEGIN,
                        ACCOUNT_CODE_END, before, after, false, field, direction);
        assertNotNull(literableDocumentAccountLinePageDto);
        assertEquals(documentAccountLines.get(ZERO).getDebitAmount(),
                literableDocumentAccountLinePageDto.getContent().get(0).getDebit());

    }

    @Test
    @DisplayName("Find DocumentAccountLines For Literable Account When have lettering lines true")
    public void testFindDocumentAccountLinesForLiterableAccountHavingLetteredLine() {
        documentAccountLines.forEach((DocumentAccountLine documentAccountLine) -> {
            documentAccountLine.setDocumentAccount(documentAccount);
            documentAccountLine.setAccount(account);
        });

        when(documentAccountLineDao.getLiterableAccountHavingLetteredLines(ACCOUNT_CODE_BEGIN_I, ACCOUNT_CODE_END_I,
                before, after, pageable)).thenReturn(page);
        when(documentAccountLineDao.findByAccountCodeAndLetterIsNotNull(ONE, before, after, pageableAccountLine))
                .thenReturn(documentAccountLinesPage);

        LiterableDocumentAccountLinePageDto literableDocumentAccountLinePageDto = letteringService
                .findDocumentAccountLinesForLiterableAccount(ONE, ONE, ONE, MUST_BE_TRUE, ACCOUNT_CODE_BEGIN,
                        ACCOUNT_CODE_END, before, after, false, field, direction);
        assertNotNull(literableDocumentAccountLinePageDto);
        assertEquals(documentAccountLines.get(1).getCreditAmount(),
                literableDocumentAccountLinePageDto.getContent().get(1).getCredit());
    }

    @Test
    @DisplayName("The ending date must not be prior to the starting date")
    public void testFindDocumentAccountLinesForLiterableAccountInvalidDate() {
        // call method
        HttpCustomException exception = assertThrows(HttpCustomException.class,
                () -> letteringService.findDocumentAccountLinesForLiterableAccount(anyInt(), anyInt(), anyInt(),
                        anyBoolean(), ACCOUNT_CODE_BEGIN, ACCOUNT_CODE_END, after, before, false, field, direction));
        assertEquals(ApiErrors.Accounting.START_DATE_IS_AFTER_END_DATE, exception.getErrorCode());
    }

    /**
     * SaveLettersToSelectedLiterableDocumentAccountLine
     */
    @Test
    @DisplayName("save Letters To Selected Literable Document Account Line")
    public void testSaveLettersToSelectedLiterableDocumentAccountLine() {
        List<LiterableDocumentAccountLineDto> selectedLiterableDocumentAccountLine = new ArrayList<>();
        LiterableDocumentAccountLineDto literableDocumentAccountLineDto = new LiterableDocumentAccountLineDto();
        literableDocumentAccountLineDto.setId(ID_ENTITY);
        literableDocumentAccountLineDto.setLetter("001");
        literableDocumentAccountLineDto.setDocumentAccount(ID_ENTITY);
        selectedLiterableDocumentAccountLine.add(literableDocumentAccountLineDto);
        AccountingConfigurationDto accountingConfigurationDto = new AccountingConfigurationDto();
        accountingConfigurationService.updateCurrentFiscalYear(ID_ENTITY);
        DocumentAccountingDto documentAccountingDto = new DocumentAccountingDto();
        documentAccountingDto.setFiscalYearId(ID_ENTITY);
        documentAccountingDto.setDocumentDate(LocalDateTime.now());
        DocumentAccountLine documentAccountLine = new DocumentAccountLine();
        documentAccountLine.setLetter("001");

        // assert steps
        when(accountingConfigurationService.findLastConfig()).thenReturn(accountingConfigurationDto);
        when(documentAccountService.getDocumentAccount(anyLong())).thenReturn(documentAccountingDto);
        when(fiscalYearService.isDateInClosedPeriod(any(LocalDateTime.class), anyLong())).thenReturn(false);
        when(documentAccountLineDao.findByIdAndIsDeletedFalse(anyLong())).thenReturn(documentAccountLine);
        when(documentAccountLineDao.saveAll(documentAccountLines)).thenReturn(documentAccountLines);
        // call method
        List<LiterableDocumentAccountLineDto> selectedLiterableDocumentAccountLineR = letteringService
                .saveLettersToSelectedLiterableDocumentAccountLine(selectedLiterableDocumentAccountLine);
        assertNotNull(selectedLiterableDocumentAccountLine);
        assertEquals(selectedLiterableDocumentAccountLine.get(0).getLetter(),
                selectedLiterableDocumentAccountLineR.get(0).getLetter());
    }

    /**
     * removeLettersFromDeselectedDocumentAccountLine
     */
    @Test
    @DisplayName("Remove Letters From Deselected Document account Line")
    public void testRemoveLettersFromDeselectedDocumentAccountLine() {
        AccountingConfigurationDto accountingConfigurationDto = new AccountingConfigurationDto();
        LiterableDocumentAccountLineDto literableDocumentAccountLineDto = new LiterableDocumentAccountLineDto();
        literableDocumentAccountLineDto.setId(ID_ENTITY);
        literableDocumentAccountLineDto.setLetter("001");
        literableDocumentAccountLineDto.setDocumentAccount(ID_ENTITY);
        List<LiterableDocumentAccountLineDto> deselectedDocumentAccountLine = new ArrayList<>();
        deselectedDocumentAccountLine.add(literableDocumentAccountLineDto);
        DocumentAccountLine documentAccountLine = new DocumentAccountLine();
        Optional<DocumentAccountLine> optionnalDocumentAccountLines = Optional.of(documentAccountLine);
        List<DocumentAccountLine> listDocumentAccountLines = new ArrayList<>();
        DocumentAccountingDto documentAccountingDto = new DocumentAccountingDto();
        when(accountingConfigurationService.findLastConfig()).thenReturn(accountingConfigurationDto);
        when(documentAccountLineDao.findById(anyLong())).thenReturn(optionnalDocumentAccountLines);
        when(documentAccountService.getDocumentAccount(anyLong())).thenReturn(documentAccountingDto);
        when(fiscalYearService.isDateInClosedPeriod(any(LocalDateTime.class), anyLong())).thenReturn(false);
        when(documentAccountLineDao.saveAll(listDocumentAccountLines)).thenReturn(listDocumentAccountLines);
        List<LiterableDocumentAccountLineDto> deselectedDocumentAccountLineR = letteringService
                .removeLettersFromDeselectedDocumentAccountLine(deselectedDocumentAccountLine);
        assertNotNull(deselectedDocumentAccountLineR);
        assertEquals(deselectedDocumentAccountLine.get(0).getLetter(),
                deselectedDocumentAccountLineR.get(0).getLetter());
    }

}
