package fr.sparkit.accounting.services.test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import fr.sparkit.accounting.dao.BaseRepository;
import fr.sparkit.accounting.dao.FiscalYearDao;
import fr.sparkit.accounting.dto.FiscalYearDto;
import fr.sparkit.accounting.entities.DocumentAccount;
import fr.sparkit.accounting.entities.FiscalYear;
import fr.sparkit.accounting.services.impl.DocumentAccountService;
import fr.sparkit.accounting.services.impl.FiscalYearService;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.http.HttpCustomException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FiscalYearServiceTest {

    @InjectMocks
    private FiscalYearService fiscalYearService;
    @Mock
    private FiscalYearDao fiscalYearDao;
    @Mock
    private DocumentAccountService documentAccountService;
    @Mock
    public BaseRepository baseRepository;

    private static final int ONE = 1;
    private static final int ZERO = 0;
    private static final Long ID_ENTITY = 1L;
    private static final Long ID_ENTITY_2 = 2L;
    private static final String YEAR_LABEL = "2020";

    private FiscalYearDto fiscalYearDto;
    private FiscalYear fiscalYear;

    public FiscalYearServiceTest() {
        super();
    }

    public final void loadData() {
        fiscalYearDto = new FiscalYearDto(ID_ENTITY, YEAR_LABEL, LocalDateTime.now(), LocalDateTime.now().plusYears(1),
                null, null, ONE);
        fiscalYear = new FiscalYear(ID_ENTITY, YEAR_LABEL, LocalDateTime.now(), LocalDateTime.now().plusYears(1), null,
                null, ONE, false, null);
    }

    @BeforeAll
    void setUp() {
        MockitoAnnotations.initMocks(this);
        loadData();
    }

    /**
     * saveOrUpdate
     */
    @Test
    @DisplayName("Fiscal Year is not opened")
    public void testSaveOrUpdateFiscalYerNotOpened() {
        when(fiscalYearDao.findOne(anyLong())).thenReturn(fiscalYear);
        HttpCustomException exception = assertThrows(HttpCustomException.class,
                () -> fiscalYearService.saveOrUpdate(fiscalYearDto));
        assertEquals(ApiErrors.Accounting.UPDATING_FISCAL_YEAR_THAT_IS_NOT_OPENED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Name Fiscal Year Already Exists")
    public void testSaveOrUpdateFiscalYearAlreadyNameExits() {
        fiscalYear.setClosingState(ZERO);
        DocumentAccount dA = new DocumentAccount();
        dA.setDocumentDate(LocalDateTime.now());
        fiscalYear.setId(ID_ENTITY_2);

        when(fiscalYearDao.findOne(anyLong())).thenReturn(fiscalYear);
        List<DocumentAccount> documentsInFiscalYear = new ArrayList<>();
        when(documentAccountService.findAllDocumentsInFiscalYear(ID_ENTITY)).thenReturn(documentsInFiscalYear);
        when(fiscalYearDao.findByNameAndIsDeletedFalse(fiscalYearDto.getName())).thenReturn(fiscalYear);
        when(baseRepository.saveAndFlush(fiscalYear)).thenReturn(fiscalYear);

        HttpCustomException exception = assertThrows(HttpCustomException.class,
                () -> fiscalYearService.saveOrUpdate(fiscalYearDto));
        assertEquals(ApiErrors.Accounting.FISCAL_YEAR_NAME_EXISTS, exception.getErrorCode());
    }

    /**
     * findById
     */
    @Test
    @DisplayName("Find fiscalYear by ID")
    public void testFindById() {
        when(fiscalYearDao.findOne(anyLong())).thenReturn(fiscalYear);
        FiscalYearDto targetFiscalYear = fiscalYearService.findById(ID_ENTITY);
        assertNotNull(targetFiscalYear);
    }

    @Test
    @DisplayName("Fiscal year doesn't exist")
    public void testFindByIdFiscalYearNotExist() {
        when(fiscalYearDao.findOne(anyLong())).thenReturn(null);
        HttpCustomException exception = assertThrows(HttpCustomException.class,
                () -> fiscalYearService.findById(ID_ENTITY));
        assertEquals(ApiErrors.Accounting.FISCAL_YEAR_INEXISTANT_FISCAL_YEAR, exception.getErrorCode());
    }

    /**
     * getDefaultSortFieldForFiscalYear
     */
    @Test
    @DisplayName("Get Default Sort Field For Fiscal Year")
    public void testGetDefaultSortFieldForFiscalYear() {
        fiscalYearService.getDefaultSortFieldForFiscalYear();
    }

}
