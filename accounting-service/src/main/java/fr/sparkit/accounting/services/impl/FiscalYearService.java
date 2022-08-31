package fr.sparkit.accounting.services.impl;

import static fr.sparkit.accounting.services.utils.AccountingServiceUtil.dateIsAfterOrEquals;
import static fr.sparkit.accounting.services.utils.AccountingServiceUtil.isDateBeforeOrEquals;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.constants.NumberConstant;
import fr.sparkit.accounting.convertor.FiscalYearConvertor;
import fr.sparkit.accounting.convertor.ReportLineConverter;
import fr.sparkit.accounting.dao.FiscalYearDao;
import fr.sparkit.accounting.dto.Filter;
import fr.sparkit.accounting.dto.FiscalYearDto;
import fr.sparkit.accounting.entities.AmortizationTable;
import fr.sparkit.accounting.entities.DocumentAccount;
import fr.sparkit.accounting.entities.FiscalYear;
import fr.sparkit.accounting.entities.ReportLine;
import fr.sparkit.accounting.enumuration.FiscalYearClosingState;
import fr.sparkit.accounting.enumuration.ReportType;
import fr.sparkit.accounting.services.IAccountingConfigurationService;
import fr.sparkit.accounting.services.IAmortizationtableService;
import fr.sparkit.accounting.services.IDocumentAccountService;
import fr.sparkit.accounting.services.IFiscalYearService;
import fr.sparkit.accounting.services.IReportLineService;
import fr.sparkit.accounting.services.utils.AccountingServiceUtil;
import fr.sparkit.accounting.services.utils.FiscalYearUtil;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.errors.ErrorsResponse;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FiscalYearService extends GenericService<FiscalYear, Long> implements IFiscalYearService {

    private final FiscalYearDao fiscalYearDao;
    private final IDocumentAccountService documentAccountService;
    private final IReportLineService reportLineService;
    private final IAmortizationtableService amortizationTableService;
    private final IAccountingConfigurationService accountingConfigurationService;

    @Autowired
    public FiscalYearService(FiscalYearDao fiscalYearDao, @Lazy DocumentAccountService documentAccountService,
            @Lazy IReportLineService reportLineService, @Lazy AmortizationTableService amortizationTableService,
            @Lazy IAccountingConfigurationService accountingConfigurationService) {
        super();
        this.fiscalYearDao = fiscalYearDao;
        this.documentAccountService = documentAccountService;
        this.reportLineService = reportLineService;
        this.amortizationTableService = amortizationTableService;
        this.accountingConfigurationService = accountingConfigurationService;
    }

    @Override
    public boolean existsById(Long fiscalYearId) {
        return fiscalYearDao.existsByIdAndIsDeletedFalse(fiscalYearId);
    }

    @Override
    @Cacheable(value = "FiscalYearCache", key = "'FiscalYearCache_'+T(fr.sparkit.accounting.util.CompanyContextHolder).getCompanyContext()+'_'"
            + "+T(java.util.Arrays).toString(#root.args)", unless = "#result==null")
    public FiscalYearDto findById(Long id) {
        return FiscalYearConvertor.modelToDto(Optional.ofNullable(fiscalYearDao.findOne(id))
                .orElseThrow(() -> new HttpCustomException(ApiErrors.Accounting.FISCAL_YEAR_INEXISTANT_FISCAL_YEAR,
                        new ErrorsResponse().error(id))));
    }

    @Override
    public Long findFiscalYearOfDate(LocalDateTime ldt) {
        return fiscalYearDao.findFiscalYearOfDate(ldt).map(FiscalYear::getId).orElse(null);
    }

    @Override
    public List<FiscalYear> findAll() {
        return fiscalYearDao.findAllByIsDeletedFalseOrderByStartDateDesc();
    }

    @Override
    public Optional<FiscalYear> findPreviousFiscalYear(Long fiscalYearId) {
        return fiscalYearDao.findPreviousFiscalYear(fiscalYearId);
    }

    @Override
    @CacheEvict(value = "FiscalYearCache", allEntries = true)
    public FiscalYearDto saveOrUpdate(FiscalYearDto fiscalYearDto) {
        FiscalYearUtil.checkValuesNotNull(fiscalYearDto);
        fiscalYearDto.setStartDate(fiscalYearDto.getStartDate().with(LocalTime.MIN));
        fiscalYearDto.setEndDate(fiscalYearDto.getEndDate().with(LocalTime.MAX).truncatedTo(ChronoUnit.MILLIS));
        FiscalYearUtil.checkDatesOrderValid(fiscalYearDto);
        if (fiscalYearDto.getId() != null) {
            FiscalYearDto fiscalYear = findById(fiscalYearDto.getId());
            if (fiscalYear.getClosingState() == FiscalYearClosingState.CONCLUDED.getValue()) {
                log.error(AccountingConstants.TRYING_TO_UPDATE_FISCAL_YEAR_THAT_IS_CONCLUDED);
                throw new HttpCustomException(ApiErrors.Accounting.UPDATING_FISCAL_YEAR_THAT_IS_CONCLUDED);
            }
            checkDateStartDateValid(fiscalYearDto, true);
            if ((!fiscalYear.getStartDate().equals(fiscalYearDto.getStartDate())
                    || !fiscalYear.getEndDate().equals(fiscalYearDto.getEndDate()))) {
                if (fiscalYear.getClosingState() != FiscalYearClosingState.OPEN.getValue()) {
                    log.error(AccountingConstants.TRYING_TO_UPDATE_FISCAL_YEAR_THAT_IS_NOT_OPENED);
                    throw new HttpCustomException(ApiErrors.Accounting.UPDATING_FISCAL_YEAR_THAT_IS_NOT_OPENED);
                }
                if (!fiscalYearDao.findAllFiscalYearsAfterDate(fiscalYear.getStartDate()).isEmpty()) {
                    log.error(AccountingConstants.TRYING_TO_UPDATE_FISCAL_YEAR_THAT_IS_NOT_THE_LAST);
                    throw new HttpCustomException(ApiErrors.Accounting.UPDATING_FISCAL_YEAR_THAT_IS_NOT_LAST);
                }
                if (!isEveryDocumentInFiscalYearStillInPeriod(fiscalYear.getId(), fiscalYearDto.getStartDate(),
                        fiscalYearDto.getEndDate())) {
                    log.error(AccountingConstants.TRYING_TO_UPDATE_FISCAL_YEAR_THAT_DOCUMENT_NOT_IN_NEW_PERIOD);
                    throw new HttpCustomException(ApiErrors.Accounting.FISCAL_YEAR_NOT_ALL_DOCUMENTS_IN_NEW_PERIOD);
                }
            }
        } else {
            checkDateStartDateValid(fiscalYearDto, false);
        }
        FiscalYear fiscalYearWithTheSameName = fiscalYearDao.findByNameAndIsDeletedFalse(fiscalYearDto.getName());
        if ((fiscalYearWithTheSameName != null) && ((fiscalYearDto.getId() == null)
                || !fiscalYearDto.getId().equals(fiscalYearWithTheSameName.getId()))) {
            log.error(AccountingConstants.TRYING_TO_SAVE_FISCAL_YEAR_WITH_EXISTING_NAME,
                    fiscalYearWithTheSameName.getName());
            throw new HttpCustomException(ApiErrors.Accounting.FISCAL_YEAR_NAME_EXISTS,
                    new ErrorsResponse().error(fiscalYearDto.getName()));
        } else {
            fiscalYearDto.setId(saveAndFlush(FiscalYearConvertor.dtoToModel(fiscalYearDto)).getId());
            log.info(AccountingConstants.LOG_ENTITY_SAVED, FiscalYearConvertor.dtoToModel(fiscalYearDto));
            if (reportLineService.findByFiscalYearIdAndIsDeletedFalseOrderById(fiscalYearDto.getId()).isEmpty()) {
                log.info(AccountingConstants.INITIALIZING_DEFAULT_REPORT_LINES_FOR_FISCAL_YEAR, fiscalYearDto.getId());
                Arrays.stream(ReportType.values()).forEach(reportType -> reportLineService
                        .initReportLinesForFiscalYear(reportType, fiscalYearDto.getId()));
            }
            return fiscalYearDto;
        }
    }

    private boolean isEveryDocumentInFiscalYearStillInPeriod(Long fiscalYearId, LocalDateTime newStartDate,
            LocalDateTime newEndDate) {
        List<DocumentAccount> documentsInFiscalYear = documentAccountService.findAllDocumentsInFiscalYear(fiscalYearId);
        if (documentsInFiscalYear.isEmpty()) {
            return true;
        } else {
            return documentsInFiscalYear.stream()
                    .filter(documentAccount -> dateIsAfterOrEquals(
                            documentAccount.getDocumentDate().truncatedTo(ChronoUnit.MILLIS), newEndDate)
                            || isDateBeforeOrEquals(documentAccount.getDocumentDate().truncatedTo(ChronoUnit.MILLIS),
                                    newStartDate))
                    .findAny().orElse(null) == null;
        }

    }

    private void checkDateStartDateValid(FiscalYearDto fiscalYearDto, boolean caseUpdate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy ");

        if (caseUpdate) {
            Optional<FiscalYear> previousFiscalYear = fiscalYearDao.findPreviousFiscalYear(fiscalYearDto.getId());
            if (previousFiscalYear.isPresent() && !previousFiscalYear.get().getEndDate().plusDays(NumberConstant.ONE)
                    .toLocalDate().equals(fiscalYearDto.getStartDate().toLocalDate())) {
                throw new HttpCustomException(ApiErrors.Accounting.START_DATE_NOT_VALID, new ErrorsResponse()
                        .error(previousFiscalYear.get().getEndDate().plusDays(NumberConstant.ONE).format(formatter)));
            }
        } else {
            FiscalYear fiscalYear = fiscalYearDao.findTopByIsDeletedFalseOrderByEndDateDesc();
            if (!fiscalYear.getEndDate().plusDays(NumberConstant.ONE).toLocalDate()
                    .equals(fiscalYearDto.getStartDate().toLocalDate())) {
                throw new HttpCustomException(ApiErrors.Accounting.START_DATE_NOT_VALID, new ErrorsResponse()
                        .error(fiscalYear.getEndDate().plusDays(NumberConstant.ONE).format(formatter)));
            }
        }

    }

    @Override
    public void checkPreviousFiscalYearsAreConcluded(FiscalYear currentFiscalYear) {
        List<Long> previousOpenedFiscalYears = fiscalYearDao
                .findAllFiscalYearsBeforeCurrentFiscalYear(currentFiscalYear.getStartDate()).stream()
                .filter(previousFiscalYear -> previousFiscalYear.getClosingState() != FiscalYearClosingState.CONCLUDED
                        .getValue())
                .map(FiscalYear::getId).collect(Collectors.toList());
        if (!previousOpenedFiscalYears.isEmpty()) {
            throw new HttpCustomException(ApiErrors.Accounting.PREVIOUS_FISCAL_YEARS_NOT_ALL_CONCLUDED);
        }
    }

    @Override
    public String getDefaultSortFieldForFiscalYear() {
        String defaultSortFieldForAccount = AccountingConstants.FIELD_NAME_START_DATE;
        if (AccountingServiceUtil.fieldExistsInEntity(defaultSortFieldForAccount, FiscalYear.class)) {
            return defaultSortFieldForAccount;
        } else {
            throw new HttpCustomException(ApiErrors.Accounting.TRYING_TO_SORT_USING_NON_EXISTENT_FIELD);
        }
    }

    @Override
    public LocalDateTime startDateFiscalYear() {
        Optional<LocalDateTime> nextFiscalYearStartDate = fiscalYearDao.startDateFiscalYear();
        return nextFiscalYearStartDate.map(localDateTime -> localDateTime.plusDays(1L)).orElseGet(LocalDateTime::now);
    }

    @Override
    @CacheEvict(value = "FiscalYearCache", allEntries = true)
    public FiscalYear closePeriod(Long fiscalYearId, LocalDateTime closingDate, String user) {
        FiscalYear fiscalYear = FiscalYearConvertor.dtoToModel(findById(fiscalYearId));
        if (closingDate != null) {
            closingDate = closingDate.with(LocalTime.MAX).truncatedTo(ChronoUnit.MILLIS);
            if (AccountingServiceUtil.isDateBeforeOrEquals(closingDate, fiscalYear.getEndDate())) {
                fiscalYear.setClosingDate(closingDate);
                fiscalYear.setClosingState(getClosingStateForFiscalYear(fiscalYear));
                log.info(AccountingConstants.CLOSING_THE_PERIOD_BETWEEN_IN_FISCAL_YEAR, fiscalYear.getStartDate(),
                        closingDate, fiscalYearId);
                fiscalYear = saveAndFlush(fiscalYear);
                log.info(AccountingConstants.LOG_ENTITY_UPDATED, fiscalYear);
                if (fiscalYear.getClosingState() == FiscalYearClosingState.CLOSED.getValue()) {
                    generateAllAccountingReports(fiscalYear.getId(), user);
                }
                return fiscalYear;
            } else {
                throw new HttpCustomException(ApiErrors.Accounting.FISCAL_YEAR_CLOSING_DATE_BEFORE_END_DATE);
            }
        } else {
            throw new HttpCustomException(ApiErrors.Accounting.FISCAL_YEAR_CLOSING_DATE_NULL);
        }
    }

    @Override
    public int getClosingStateForFiscalYear(FiscalYear fiscalYear) {
        if (fiscalYear.getClosingDate() != null) {
            if (fiscalYear.getClosingDate().equals(fiscalYear.getEndDate())) {
                return FiscalYearClosingState.CLOSED.getValue();
            } else {
                return FiscalYearClosingState.PARTIALLY_CLOSED.getValue();
            }
        } else {
            return FiscalYearClosingState.OPEN.getValue();
        }
    }

    @Override
    @CacheEvict(value = "FiscalYearCache", allEntries = true)
    public FiscalYear openFiscalYear(Long fiscalYearId) {
        FiscalYear fiscalYear = FiscalYearConvertor.dtoToModel(findById(fiscalYearId));
        List<FiscalYear> followingFiscalYears = findAllFiscalYearsAfterDate(fiscalYear.getStartDate());

        followingFiscalYears.forEach((FiscalYear followingFiscalYear) -> {
            if (hasFiscalYearsAJournalANewThatContainsLetteredLines(followingFiscalYear)) {
                throw new HttpCustomException(ApiErrors.Accounting.JOURNAL_A_NEW_TO_REMOVE_CONTAINS_LETTERED_LINES,
                        new ErrorsResponse().error(followingFiscalYear.getName()));
            }
        });
        fiscalYear.setClosingDate(null);
        fiscalYear.setConclusionDate(null);
        fiscalYear.setClosingState(FiscalYearClosingState.OPEN.getValue());
        saveAndFlush(fiscalYear);
        log.info(AccountingConstants.OPENING_FISCAL_YEAR, fiscalYearId);
        accountingConfigurationService.updateCurrentFiscalYear(fiscalYearId);
        followingFiscalYears.forEach((FiscalYear followingFiscalYear) -> {
            followingFiscalYear.setConclusionDate(null);
            followingFiscalYear.setClosingDate(null);
            followingFiscalYear.setClosingState(FiscalYearClosingState.OPEN.getValue());
            documentAccountService.deleteJournalANewDocumentForFiscalYear(followingFiscalYear.getId());
        });
        save(followingFiscalYears);
        log.info(AccountingConstants.LOG_ENTITY_UPDATED, followingFiscalYears);
        List<AmortizationTable> amortizationTables = amortizationTableService.findByFiscalYear(fiscalYearId);
        amortizationTableService.deleteList(amortizationTables);
        log.info(AccountingConstants.AMORTIZATION_TABLES_LIST_DELETED);
        return fiscalYear;
    }

    private boolean hasFiscalYearsAJournalANewThatContainsLetteredLines(FiscalYear fiscalYear) {
        DocumentAccount journalANewDocument = documentAccountService
                .findJournalANewDocumentForFiscalYear(fiscalYear.getId());
        if (journalANewDocument != null) {
            return documentAccountService.hasDocumentAccountLetteredLines(journalANewDocument);
        }
        return false;
    }

    @Override
    public boolean isDateInClosedPeriod(LocalDateTime date) {
        Optional<FiscalYear> fiscalYear = fiscalYearDao.findFiscalYearOfDate(date);
        LocalDateTime startDateOfFirstFiscalYear = fiscalYearDao.getStartDateOfFirstFiscalYear();
        return (fiscalYear.isPresent() && fiscalYear.get().getClosingDate() != null
                && date.compareTo(fiscalYear.get().getClosingDate()) <= 0
                && date.compareTo(fiscalYear.get().getStartDate()) >= 0)
                || (date.compareTo(startDateOfFirstFiscalYear) < 0);
    }

    @Override
    public boolean isDateInClosedPeriod(LocalDateTime date, Long fiscalYearId) {
        FiscalYearDto fiscalYear = findById(fiscalYearId);
        return fiscalYear.getClosingDate() != null && date.compareTo(fiscalYear.getClosingDate()) <= 0
                && date.compareTo(fiscalYear.getStartDate()) >= 0;
    }

    @Override
    public boolean isDateInFiscalYear(LocalDateTime date, Long fiscalYearId) {
        FiscalYearDto fiscalYear = findById(fiscalYearId);
        return date != null && date.compareTo(fiscalYear.getEndDate()) <= 0
                && date.compareTo(fiscalYear.getStartDate()) >= 0;
    }

    @Override
    public List<FiscalYear> findClosedFiscalYears() {
        return fiscalYearDao.findAllByClosingStateInAndIsDeletedFalse(Arrays
                .asList(FiscalYearClosingState.PARTIALLY_CLOSED.getValue(), FiscalYearClosingState.CLOSED.getValue()));
    }

    @Override
    public void generateAllAccountingReports(Long fiscalYearId, String user) {
        List<ReportLine> reportLines = new ArrayList<>();
        Arrays.stream(ReportType.values()).forEach(reportType -> reportLines.addAll(ReportLineConverter
                .dtosToModels(reportLineService.generateAnnualReport(reportType, fiscalYearId, user))));
        reportLineService.save(reportLines);
        log.info(AccountingConstants.LOG_ENTITY_CREATED, reportLines);
    }

    @Override
    @CacheEvict(value = "FiscalYearCache", allEntries = true)
    public void closeFiscalYear(Long fiscalYearId) {
        FiscalYearDto fiscalYear = findById(fiscalYearId);
        fiscalYear.setClosingState(FiscalYearClosingState.CLOSED.getValue());
        fiscalYear.setClosingDate(fiscalYear.getEndDate());
        fiscalYear.setClosingDate(null);
        saveAndFlush(FiscalYearConvertor.dtoToModel(fiscalYear));
        log.info(AccountingConstants.LOG_ENTITY_UPDATED, fiscalYear);
    }

    @Override
    public List<FiscalYear> findAllFiscalYearsAfterDate(LocalDateTime fiscalYearStartDate) {
        return fiscalYearDao.findAllFiscalYearsAfterDate(fiscalYearStartDate);
    }

    @Override
    public FiscalYear findLastFiscalYearByEndDate() {
        return fiscalYearDao.findTopByIsDeletedFalseOrderByEndDateDesc();
    }

    @Override
    public List<FiscalYearDto> findAllFiscalYearsNotClosed() {
        List<FiscalYear> openedFiscalYears = fiscalYearDao
                .findByClosingStateAndIsDeletedFalse(FiscalYearClosingState.OPEN.getValue());
        openedFiscalYears.addAll(
                fiscalYearDao.findByClosingStateAndIsDeletedFalse(FiscalYearClosingState.PARTIALLY_CLOSED.getValue()));
        return FiscalYearConvertor.modelsToDtos(openedFiscalYears);
    }

    @Override
    public FiscalYearDto firstFiscalYearNotConcluded() {
        List<FiscalYearDto> unConcludedFiscalYears = findUnConcludedFiscalYears();
        if (unConcludedFiscalYears.isEmpty()) {
            return new FiscalYearDto();
        }
        return unConcludedFiscalYears.get(0);
    }

    @Override
    public List<FiscalYearDto> findOpeningTargets() {
        List<FiscalYearDto> unConcludedFiscalYears = findUnConcludedFiscalYears();
        if (!unConcludedFiscalYears.isEmpty()) {
            unConcludedFiscalYears.remove(0);
        }
        return unConcludedFiscalYears;
    }

    private List<FiscalYearDto> findUnConcludedFiscalYears() {
        return FiscalYearConvertor.modelsToDtos(
                fiscalYearDao.findAllByClosingStateNotOrderByStartDate(FiscalYearClosingState.CONCLUDED.getValue()));
    }

    @Override
    public LocalDateTime getStartDateOfFirstFiscalYear() {
        return fiscalYearDao.getStartDateOfFirstFiscalYear();
    }

    @Override
    public Page<FiscalYearDto> filterFiscalYear(List<Filter> filters, Pageable pageable) {
        if (!pageable.getSort().get().findFirst().isPresent()) {
            pageable = AccountingServiceUtil.getPageable(pageable.getPageNumber(), pageable.getPageSize(),
                    getDefaultSortFieldForFiscalYear(), Sort.Direction.ASC.toString());
        }
        Page<FiscalYear> page = FilterService.getPageOfFilterableEntity(FiscalYear.class, fiscalYearDao, filters,
                pageable);
        return new PageImpl<>(FiscalYearConvertor.modelsToDtos(page.getContent()), pageable, page.getTotalElements());
    }

    @Override
    public Optional<FiscalYear> findNextFiscalYear(Long fiscalYearId) {
        return fiscalYearDao.findNextFiscalYear(fiscalYearId);
    }
}
