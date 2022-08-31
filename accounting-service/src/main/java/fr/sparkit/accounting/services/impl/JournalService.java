package fr.sparkit.accounting.services.impl;

import static fr.sparkit.accounting.constants.AccountingConstants.ENTITY_NAME_JOURNAL;
import static fr.sparkit.accounting.constants.AccountingConstants.EXPORT_FILE_NAME;
import static fr.sparkit.accounting.constants.AccountingConstants.IMPORT_MODEL_FILE_NAME;
import static fr.sparkit.accounting.constants.AccountingConstants.INVALID_CODE_MAX_LEGNTH_EXCEEDED;
import static fr.sparkit.accounting.constants.AccountingConstants.INVALID_LABEL_LEGNTH;
import static fr.sparkit.accounting.constants.AccountingConstants.JOURNAL_CODE_EXIST;
import static fr.sparkit.accounting.constants.AccountingConstants.JOURNAL_LABEL_EXIST;
import static fr.sparkit.accounting.constants.AccountingConstants.LOG_ENTITY_CREATED;
import static fr.sparkit.accounting.constants.AccountingConstants.LOG_ENTITY_DELETED;
import static fr.sparkit.accounting.constants.AccountingConstants.SIMULATION_EXPORT_FILE_NAME;
import static fr.sparkit.accounting.util.CalculationUtil.getBigDecimalValueFromFormattedString;
import static fr.sparkit.accounting.util.CalculationUtil.getFormattedBigDecimalValue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import fr.sparkit.accounting.auditing.JournalExcelCell;
import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.constants.LanguageConstants;
import fr.sparkit.accounting.constants.NumberConstant;
import fr.sparkit.accounting.constants.XLSXErrors;
import fr.sparkit.accounting.convertor.JournalConverter;
import fr.sparkit.accounting.dao.JournalDao;
import fr.sparkit.accounting.dto.AccountingConfigurationDto;
import fr.sparkit.accounting.dto.CentralizingJournalByMonthReportLineDto;
import fr.sparkit.accounting.dto.CentralizingJournalDetailsByMonthDto;
import fr.sparkit.accounting.dto.CentralizingJournalDetailsDto;
import fr.sparkit.accounting.dto.CentralizingJournalDto;
import fr.sparkit.accounting.dto.CentralizingJournalReportLineDto;
import fr.sparkit.accounting.dto.ChartAccountsDto;
import fr.sparkit.accounting.dto.FileUploadDto;
import fr.sparkit.accounting.dto.Filter;
import fr.sparkit.accounting.dto.JournalDto;
import fr.sparkit.accounting.dto.excel.JournalXLSXFormatDto;
import fr.sparkit.accounting.entities.ChartAccounts;
import fr.sparkit.accounting.entities.DocumentAccountLine;
import fr.sparkit.accounting.entities.Journal;
import fr.sparkit.accounting.services.IAccountingConfigurationService;
import fr.sparkit.accounting.services.IChartAccountsService;
import fr.sparkit.accounting.services.IDocumentAccountLineService;
import fr.sparkit.accounting.services.IJournalService;
import fr.sparkit.accounting.services.utils.AccountingServiceUtil;
import fr.sparkit.accounting.services.utils.JournalServiceUtil;
import fr.sparkit.accounting.services.utils.excel.ExcelCellStyleHelper;
import fr.sparkit.accounting.services.utils.excel.GenericExcelPOIHelper;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.errors.ErrorsResponse;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JournalService extends GenericService<Journal, Long> implements IJournalService {

    private static final String CHART_ACCOUNT_CODE_SUPPLIERS = "chartAccountCodeSuppliers";
    private static final String CHART_ACCOUNT_CODE_CUSTOMERS = "chartAccountCodeCustomers";
    private static final String CHART_ACCOUNT_CODE_OUT_OF_TIERS = "chartAccountCodeOutOfTiers";
    private static final int TWO = 2;
    private static final int THREE = 3;
    private static final int TEN = 10;
    private static final DataFormatter dataFormatter = new DataFormatter();
    @Value("${accounting.excel.storage-directory}")
    private Path excelStoragePath;

    private final List<Field> excelHeaderFields;
    private List<String> acceptedHeaders;

    private final JournalDao journalDao;
    private final IDocumentAccountLineService documentAccountLineService;
    private final IChartAccountsService chartAccountService;
    private final IAccountingConfigurationService accountingConfigurationService;

    @Autowired
    public JournalService(JournalDao journalDao, IDocumentAccountLineService documentAccountLineService,
            IChartAccountsService chartAccountService,
            @Lazy IAccountingConfigurationService accountingConfigurationService) {
        this.journalDao = journalDao;
        this.documentAccountLineService = documentAccountLineService;
        this.chartAccountService = chartAccountService;
        this.accountingConfigurationService = accountingConfigurationService;
        excelHeaderFields = JournalXLSXFormatDto.getJournalExcelHeaderFields();
        acceptedHeaders = excelHeaderFields.stream()
                .map(field -> field.getAnnotation(JournalExcelCell.class).headerName()).collect(Collectors.toList());
    }

    @Override
    @Caching(evict = { @CacheEvict(value = "GeneralLedgerAccounts", allEntries = true),
            @CacheEvict(value = "GeneralLedgerAccountDetails", allEntries = true),
            @CacheEvict(value = "TrialBalanceAccounts", allEntries = true) })
    public JournalDto save(JournalDto journalDto) {
        Objects.requireNonNull(journalDto);
        checkValidCode(journalDto.getCode());
        checkValidLabelLength(journalDto.getLabel());
        checkUniqueLabel(journalDto.getLabel(), journalDto.getId());
        Journal savedJournal = saveAndFlush(JournalConverter.dtoToModel(journalDto));
        log.info(LOG_ENTITY_CREATED, savedJournal);
        return JournalConverter.modelToDto(savedJournal);
    }

    @Override
    @Caching(evict = { @CacheEvict(value = "GeneralLedgerAccounts", allEntries = true),
            @CacheEvict(value = "GeneralLedgerAccountDetails", allEntries = true),
            @CacheEvict(value = "TrialBalanceAccounts", allEntries = true) })
    public JournalDto update(JournalDto journalDto) {
        JournalDto oldJournal = findById(journalDto.getId());
        if (!oldJournal.getCode().equalsIgnoreCase(journalDto.getCode())) {
            checkValidCode(journalDto.getCode());
        }
        if (oldJournal.isReconcilable() != journalDto.isReconcilable() && oldJournal.isReconcilable()) {
            Long currentFiscalYearId = accountingConfigurationService.getCurrentFiscalYearId();
            List<DocumentAccountLine> reconcilableLinesUsingJournal = documentAccountLineService
                    .findReconcilableLinesUsingJournal(currentFiscalYearId, journalDto.getId());
            if (!reconcilableLinesUsingJournal.isEmpty()) {
                List<Long> reconcilableLinesIds = reconcilableLinesUsingJournal.stream().map(DocumentAccountLine::getId)
                        .collect(Collectors.toList());
                log.error("Trying to change journal reconcilable status while having reconcilable lines {}",
                        reconcilableLinesIds);
                throw new HttpCustomException(ApiErrors.Accounting.JOURNAL_CONTAINS_CLOSED_LINES,
                        new ErrorsResponse().error(reconcilableLinesIds));
            }
        }
        Objects.requireNonNull(journalDto);
        checkValidLabelLength(journalDto.getLabel());
        checkUniqueLabel(journalDto.getLabel(), journalDto.getId());
        Optional<Journal> journalOptional = journalDao.findByIdAndIsDeletedFalse(journalDto.getId());
        return journalOptional.flatMap(journal -> getUpdatedJournalDto(journal, journalDto))
                .orElseThrow(() -> new HttpCustomException(ApiErrors.Accounting.JOURNAL_CODE_EXISTS,
                        new ErrorsResponse().error(journalDto.getCode())));
    }

    @Override
    @Caching(evict = { @CacheEvict(value = "GeneralLedgerAccounts", allEntries = true),
            @CacheEvict(value = "GeneralLedgerAccountDetails", allEntries = true),
            @CacheEvict(value = "TrialBalanceAccounts", allEntries = true) })
    public boolean isDeleteJournal(Long id) {
        checkNull(id);
        log.info(LOG_ENTITY_DELETED, ENTITY_NAME_JOURNAL, id);
        Optional<Journal> journalToDelete = Optional.ofNullable(journalDao.findOne(id));
        return journalToDelete.filter(journal -> isDynamicSoftDelete(id, Journal.class.getName(),
                String.valueOf(journal.getLabel()), "MESSAGE_JOURNAL_TO_DELETE")).isPresent();
    }

    private Optional<JournalDto> getUpdatedJournalDto(Journal journal, JournalDto journalDto) {
        journal.setLabel(journalDto.getLabel());
        journal.setReconcilable(journalDto.isReconcilable());
        journal.setCashFlow(journalDto.isCashFlow());
        if (journal.getCode().equalsIgnoreCase(journalDto.getCode())) {
            return Optional.of(JournalConverter.modelToDto(journalDao.saveAndFlush(journal)));
        }
        if (!journalDao.existsByCodeAndIsDeletedFalse(journalDto.getCode())) {
            journal.setCode(journalDto.getCode());
            return Optional.of(JournalConverter.modelToDto(journalDao.saveAndFlush(journal)));
        }
        return Optional.empty();
    }

    private void checkUniqueLabel(String label, Long id) {
        Journal journalWithTheSameLabel = journalDao.findByLabelAndIsDeletedFalseOrderByIdDesc(label);
        if (journalWithTheSameLabel != null && !journalWithTheSameLabel.getId().equals(id)) {
            log.error(JOURNAL_LABEL_EXIST, label);
            throw new HttpCustomException(ApiErrors.Accounting.JOURNAL_LABEL_EXISTS, new ErrorsResponse().error(label));
        }
    }

    private void checkValidLabelLength(String label) {
        if (label.length() < THREE) {
            log.error(INVALID_LABEL_LEGNTH);
            throw new HttpCustomException(ApiErrors.Accounting.LABEL_MIN_LENGTH, new ErrorsResponse().error(THREE));
        }
    }

    private void checkValidCode(String code) {
        if (journalDao.existsByCodeAndIsDeletedFalse(code)) {
            log.error(JOURNAL_CODE_EXIST, code);
            throw new HttpCustomException(ApiErrors.Accounting.JOURNAL_CODE_EXISTS, new ErrorsResponse().error(code));
        }
        if (code.length() > TEN) {
            log.error(INVALID_CODE_MAX_LEGNTH_EXCEEDED);
            throw new HttpCustomException(ApiErrors.Accounting.JOURNAL_CODE_LENGTH, new ErrorsResponse().error(code));
        }
    }

    public String getDefaultSortFieldForJournal() {
        String defaultSortFieldForJournal = "code";
        if (AccountingServiceUtil.fieldExistsInEntity(defaultSortFieldForJournal, Journal.class)) {
            return defaultSortFieldForJournal;
        } else {
            log.error("Trying to sort using non existent field");
            throw new HttpCustomException(ApiErrors.Accounting.TRYING_TO_SORT_USING_NON_EXISTENT_FIELD);
        }
    }

    @Override
    public JournalDto findById(Long id) {
        return JournalConverter.modelToDto(Optional.ofNullable(journalDao.findOne(id)).orElseThrow(
                () -> new HttpCustomException(ApiErrors.Accounting.ENTITY_NOT_FOUND, new ErrorsResponse().error(id))));
    }

    private static void checkNull(Object obj) {
        if (obj == null) {
            log.error("Missing parameters from journal");
            throw new HttpCustomException(ApiErrors.Accounting.JOURNAL_MISSING_PARAMETERS);
        }
    }

    @Override
    public Journal findJournalByLabel(String label) {
        return journalDao.findByLabelAndIsDeletedFalseOrderByIdDesc(label);
    }

    @Override
    public Journal findByCode(String code) {
        return journalDao.findByCodeAndIsDeletedFalseOrderByIdDesc(code);
    }

    @Override
    public boolean existsById(Long journalId) {
        return journalDao.findOne(journalId) != null;
    }

    @Override
    public CentralizingJournalDto findCentralizingJournalPage(int page, int size, LocalDateTime startDate,
            LocalDateTime endDate, List<Long> journalIds, int breakingAccount, int breakingCustomerAccount,
            int breakingSupplierAccount) {

        AccountingServiceUtil.checkFilterOnDates(startDate, endDate);

        List<JournalDto> journalDtos = new ArrayList<>();

        Pageable pageable = PageRequest.of(page, size);

        int beginIndex = Math.toIntExact(pageable.getOffset());
        int endIndex = beginIndex + Math.toIntExact(pageable.getPageSize());

        if (beginIndex >= journalIds.size()) {
            beginIndex = 0;
            endIndex = 0;
        } else if (beginIndex + pageable.getPageSize() > journalIds.size()) {
            endIndex = journalIds.size();
        }

        Iterator<Long> journalIdsIterator = journalIds.subList(beginIndex, endIndex).iterator();

        getCentralizingJournals(startDate, endDate, journalDtos, journalIdsIterator, breakingAccount,
                breakingCustomerAccount, breakingSupplierAccount);

        BigDecimal totalAmount = getTotalCentralizingJournal(startDate, endDate, journalIds, breakingAccount,
                breakingCustomerAccount, breakingSupplierAccount);

        Page<JournalDto> journalPage = new PageImpl<>(journalDtos, pageable, journalIds.size());
        return new CentralizingJournalDto(journalPage, totalAmount, totalAmount);
    }

    @Override
    public List<CentralizingJournalDetailsDto> findCentralizingJournalDetails(Long journalId, LocalDateTime startDate,
            LocalDateTime endDate, int breakingAccount, int breakingCustomerAccount, int breakingSupplierAccount) {
        accountingConfigurationService.findLastConfig();
        List<CentralizingJournalDetailsDto> centralizingJournalDetailsDtos = new ArrayList<>();
        List<Long> journalIds = new ArrayList<>();
        journalIds.add(journalId);
        for (Month month : Month.values()) {
            if (month.compareTo(startDate.getMonth()) >= NumberConstant.ZERO
                    && month.compareTo(endDate.getMonth()) <= NumberConstant.ZERO) {
                LocalDateTime startDateMonth = startDate;
                LocalDateTime endDateMonth = endDate;
                if (month.compareTo(startDate.getMonth()) > NumberConstant.ZERO) {
                    startDateMonth = LocalDateTime.of(startDate.getYear(), month, NumberConstant.ONE,
                            NumberConstant.ZERO, NumberConstant.ZERO, NumberConstant.ZERO);
                }
                if (month.compareTo(endDate.getMonth()) < NumberConstant.ZERO) {
                    endDateMonth = LocalDateTime.of(endDate.getYear(), month.getValue(), NumberConstant.ONE,
                            NumberConstant.ZERO, NumberConstant.ZERO).with(TemporalAdjusters.lastDayOfMonth());
                }
                BigDecimal totalAmount = getTotalCentralizingJournal(startDateMonth, endDateMonth, journalIds,
                        breakingAccount, breakingCustomerAccount, breakingSupplierAccount);
                centralizingJournalDetailsDtos.add(new CentralizingJournalDetailsDto(
                        Month.of(month.getValue()).toString(), totalAmount, totalAmount));
            }
        }
        return centralizingJournalDetailsDtos;
    }

    @Override
    public Page<CentralizingJournalDetailsByMonthDto> findCentralizingJournalDetailsByMonthPage(int page, int size,
            Long journalId, LocalDateTime startDate, LocalDateTime endDate, int breakingAccount,
            int breakingCustomerAccount, int breakingSupplierAccount, String month) {

        Pageable pageable = PageRequest.of(page, size);
        Long start = pageable.getOffset();
        Long end;

        LocalDateTime startDateMonth = startDate;
        LocalDateTime endDateMonth = endDate;
        Month selectedMonth = Month.valueOf(month);
        if (selectedMonth.getValue() > startDate.getMonth().getValue()) {
            startDateMonth = LocalDateTime.of(startDate.getYear(), selectedMonth.getValue(), NumberConstant.ONE,
                    NumberConstant.ZERO, NumberConstant.ZERO, NumberConstant.ZERO);
        }
        if (selectedMonth.getValue() < endDate.getMonth().getValue()) {
            endDateMonth = LocalDateTime.of(endDate.getYear(), selectedMonth.getValue(), NumberConstant.ONE,
                    NumberConstant.ZERO, NumberConstant.ZERO).with(TemporalAdjusters.lastDayOfMonth());
        }
        AccountingConfigurationDto accountingConfigurationDto = accountingConfigurationService.findLastConfig();
        List<CentralizingJournalDetailsByMonthDto> centralizingJournalDetailsByMonthDtos = new ArrayList<>();
        centralizingJournalDetailsByMonthDtos = getCentralizingJournalDetails(startDateMonth, endDateMonth,
                breakingAccount, breakingCustomerAccount, breakingSupplierAccount,
                centralizingJournalDetailsByMonthDtos, accountingConfigurationDto, journalId);

        end = JournalServiceUtil.calculateEndPage(centralizingJournalDetailsByMonthDtos.size(), pageable, start);
        Collections.sort(centralizingJournalDetailsByMonthDtos);
        return new PageImpl<>(centralizingJournalDetailsByMonthDtos.subList(start.intValue(), end.intValue()), pageable,
                centralizingJournalDetailsByMonthDtos.size());
    }

    @Override
    public List<CentralizingJournalReportLineDto> generateCentralizingJournalTelerikReportLines(List<Long> journalIds,
            LocalDateTime startDate, LocalDateTime endDate, int breakingAccount, int breakingCustomerAccount,
            int breakingSupplierAccount) {
        List<CentralizingJournalReportLineDto> centralizingJournalReportDtos = new ArrayList<>();
        Iterator<Long> journalIdsIterator = journalIds.iterator();
        AccountingConfigurationDto accountingConfigurationDto = accountingConfigurationService.findLastConfig();
        HashMap<String, List<ChartAccounts>> chartAccountHash = new HashMap<>();
        putChartAccountCodes(breakingAccount, breakingCustomerAccount, breakingSupplierAccount,
                accountingConfigurationDto, chartAccountHash);
        List<JournalDto> journalDtos = new ArrayList<>();
        while (journalIdsIterator.hasNext()) {
            List<CentralizingJournalDetailsByMonthDto> centralizingJournalDetailsByMonthDtos = new ArrayList<>();
            Long journalId = journalIdsIterator.next();
            JournalDto journalDto = findById(journalId);

            centralizingJournalDetailsByMonthDtos = getCentralizingJournalDetails(startDate, endDate, breakingAccount,
                    breakingCustomerAccount, breakingSupplierAccount, centralizingJournalDetailsByMonthDtos,
                    accountingConfigurationDto, journalId);

            BigDecimal journalDebitAmount = JournalServiceUtil
                    .calculateJournalDebitAmount(centralizingJournalDetailsByMonthDtos);
            BigDecimal journalCreditAmount = JournalServiceUtil
                    .calculateJournalCreditAmount(centralizingJournalDetailsByMonthDtos);
            journalDto = new JournalDto(journalDto.getId(), journalDto.getCode(), journalDto.getLabel(),
                    journalDebitAmount, journalCreditAmount);
            journalDtos.add(journalDto);
            JournalServiceUtil.addCentralizingJournalReportLines(centralizingJournalDetailsByMonthDtos,
                    centralizingJournalReportDtos, journalDto);
        }
        BigDecimal totalDebitAmount = journalDtos.stream().map(JournalDto::getJournalDebitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCreditAmount = journalDtos.stream().map(JournalDto::getJournalCreditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        JournalServiceUtil.addCentralizingJournalReportTotalAmountLine(centralizingJournalReportDtos, totalDebitAmount,
                totalCreditAmount);
        return centralizingJournalReportDtos;
    }

    @Override
    public List<CentralizingJournalByMonthReportLineDto> generateCentralizingJournalByMonthReportLines(
            List<Long> journalIds, LocalDateTime startDate, LocalDateTime endDate, int breakingAccount,
            int breakingCustomerAccount, int breakingSupplierAccount) {
        final String MONTH_FORMAT = LanguageConstants.MONTH + " | %d/" + startDate.getYear();
        final String JOURNAL_FORMAT = LanguageConstants.JOURNAL + " | %s";
        final String TOTAL_MONTH_FORMAT = LanguageConstants.TOTAL_MONTH + " | %d/" + startDate.getYear();
        final String TOTAL_JOURNAL_FORMAT = LanguageConstants.TOTAL_JOURNAL + " | %s";

        List<CentralizingJournalByMonthReportLineDto> centralizingJournalByMonthReportLineDtos = new ArrayList<>();
        List<JournalDto> journals = JournalConverter.modelsToDtos(findByIds(journalIds));
        journals.sort(Comparator.comparing(JournalDto::getCode));
        Iterator<JournalDto> journalsIterator = journals.iterator();
        AccountingConfigurationDto accountingConfigurationDto = accountingConfigurationService.findLastConfig();
        HashMap<String, List<ChartAccounts>> chartAccountHash = new HashMap<>();
        putChartAccountCodes(breakingAccount, breakingCustomerAccount, breakingSupplierAccount,
                accountingConfigurationDto, chartAccountHash);
        Map<JournalDto, Map<Month, Map<Integer, List<DocumentAccountLine>>>> linesByJournalAndMonth = new HashMap<>();
        while (journalsIterator.hasNext()) {
            Map<Month, Map<Integer, List<DocumentAccountLine>>> linesByMonthAndChartAccount = new TreeMap<>();
            JournalDto journal = journalsIterator.next();
            List<DocumentAccountLine> lines = documentAccountLineService
                    .findDocumentAccountLineByJournalAndDocumentDateBetween(journal.getId(), startDate, endDate);
            for (Month month : Month.values()) {
                Map<Integer, List<DocumentAccountLine>> linesByChartAccount = new HashMap<>();
                List<DocumentAccountLine> linesForMonth = lines.stream()
                        .filter((DocumentAccountLine documentAccountLine) -> month
                                .equals(documentAccountLine.getDocumentAccount().getDocumentDate().getMonth()))
                        .collect(Collectors.toList());
                final int customerAccountCode = accountingConfigurationDto.getCustomerAccountCode();
                final int supplierAccountCode = accountingConfigurationDto.getSupplierAccountCode();
                List<DocumentAccountLine> customerLines = linesForMonth.stream()
                        .filter((DocumentAccountLine documentAccountLine) -> String
                                .valueOf(documentAccountLine.getAccount().getCode())
                                .startsWith(String.valueOf(customerAccountCode)))
                        .collect(Collectors.toList());
                setLinesToBeFiltered(breakingCustomerAccount, linesByChartAccount, customerLines);
                List<DocumentAccountLine> supplierLines = linesForMonth.stream()
                        .filter((DocumentAccountLine documentAccountLine) -> String
                                .valueOf(documentAccountLine.getAccount().getCode())
                                .startsWith(String.valueOf(supplierAccountCode)))
                        .collect(Collectors.toList());
                setLinesToBeFiltered(breakingSupplierAccount, linesByChartAccount, supplierLines);
                List<DocumentAccountLine> otherLines = linesForMonth.stream()
                        .filter((DocumentAccountLine documentAccountLine) -> !String
                                .valueOf(documentAccountLine.getAccount().getCode())
                                .startsWith(String.valueOf(customerAccountCode))
                                && !String.valueOf(documentAccountLine.getAccount().getCode())
                                        .startsWith(String.valueOf(supplierAccountCode)))
                        .collect(Collectors.toList());
                setLinesToBeFiltered(breakingAccount, linesByChartAccount, otherLines);
                linesByMonthAndChartAccount.put(month, linesByChartAccount);
                linesByJournalAndMonth.put(journal, linesByMonthAndChartAccount);
            }

        }
        BigDecimal totalDebitForReport = BigDecimal.ZERO;
        BigDecimal totalCreditForReport = BigDecimal.ZERO;
        for (JournalDto journal : journals) {
            Map<Month, Map<Integer, List<DocumentAccountLine>>> linesByMonth = linesByJournalAndMonth.get(journal);
            centralizingJournalByMonthReportLineDtos.add(new CentralizingJournalByMonthReportLineDto("",
                    String.format(JOURNAL_FORMAT, journal.getCode()), "", "", journal.getLabel(), "", ""));
            BigDecimal totalDebitForJournal = BigDecimal.ZERO;
            BigDecimal totalCreditForJournal = BigDecimal.ZERO;
            for (Map.Entry<Month, Map<Integer, List<DocumentAccountLine>>> lineByMonth : linesByMonth.entrySet()) {
                Month month = lineByMonth.getKey();
                if (month.getValue() <= endDate.getMonthValue()) {
                    Map<Integer, List<DocumentAccountLine>> linesByChartAccount = lineByMonth.getValue();
                    List<Integer> chartAccounts = new ArrayList<>(linesByChartAccount.keySet());
                    Collections.sort(chartAccounts);
                    centralizingJournalByMonthReportLineDtos.add(new CentralizingJournalByMonthReportLineDto("",
                            String.format(MONTH_FORMAT, month.getValue()), "", "", "", "", ""));
                    BigDecimal totalDebitForMonth = BigDecimal.ZERO;
                    BigDecimal totalCreditForMonth = BigDecimal.ZERO;
                    Map<Integer, CentralizingJournalByMonthReportLineDto> accountsUsedMap = new HashMap<>();
                    for (Map.Entry<Integer, List<DocumentAccountLine>> lineByChartAccount : linesByChartAccount
                            .entrySet()) {
                        List<DocumentAccountLine> documentAccountLines = lineByChartAccount.getValue();
                        for (DocumentAccountLine documentAccountLine : documentAccountLines) {
                            if (accountsUsedMap.containsKey(lineByChartAccount.getKey())) {
                                CentralizingJournalByMonthReportLineDto reportLine = accountsUsedMap
                                        .get(lineByChartAccount.getKey());
                                reportLine.setTotalCreditAmount(getFormattedBigDecimalValue(
                                        getBigDecimalValueFromFormattedString(reportLine.getTotalCreditAmount())
                                                .add(documentAccountLine.getCreditAmount())));
                                reportLine.setTotalDebitAmount(getFormattedBigDecimalValue(
                                        getBigDecimalValueFromFormattedString(reportLine.getTotalDebitAmount())
                                                .add(documentAccountLine.getDebitAmount())));
                            } else {
                                accountsUsedMap.put(lineByChartAccount.getKey(),
                                        new CentralizingJournalByMonthReportLineDto(journal.getCode(), "",
                                                String.valueOf(month.getValue()),
                                                String.valueOf(lineByChartAccount.getKey()),
                                                documentAccountLine.getAccount().getLabel(),
                                                getFormattedBigDecimalValue(documentAccountLine.getDebitAmount()),
                                                getFormattedBigDecimalValue(documentAccountLine.getCreditAmount())));
                            }
                            totalDebitForMonth = totalDebitForMonth.add(documentAccountLine.getDebitAmount());
                            totalCreditForMonth = totalCreditForMonth.add(documentAccountLine.getCreditAmount());
                        }
                    }
                    centralizingJournalByMonthReportLineDtos.addAll(accountsUsedMap.values().stream()
                            .sorted(Comparator.comparing(CentralizingJournalByMonthReportLineDto::getAccountCode))
                            .collect(Collectors.toList()));
                    centralizingJournalByMonthReportLineDtos.add(new CentralizingJournalByMonthReportLineDto("",
                            String.format(TOTAL_MONTH_FORMAT, month.getValue()), "", "", "",
                            getFormattedBigDecimalValue(totalDebitForMonth),
                            getFormattedBigDecimalValue(totalCreditForMonth)));
                    totalDebitForJournal = totalDebitForJournal.add(totalDebitForMonth);
                    totalCreditForJournal = totalCreditForJournal.add(totalCreditForMonth);
                }
            }
            centralizingJournalByMonthReportLineDtos.add(new CentralizingJournalByMonthReportLineDto("",
                    String.format(TOTAL_JOURNAL_FORMAT, journal.getCode()), "", "", "",
                    getFormattedBigDecimalValue(totalDebitForJournal),
                    getFormattedBigDecimalValue(totalCreditForJournal)));
            totalDebitForReport = totalDebitForReport.add(totalDebitForJournal);
            totalCreditForReport = totalCreditForReport.add(totalCreditForJournal);
        }
        centralizingJournalByMonthReportLineDtos.add(new CentralizingJournalByMonthReportLineDto("",
                LanguageConstants.GENERAL_TOTAL, "", "", "", getFormattedBigDecimalValue(totalDebitForReport),
                getFormattedBigDecimalValue(totalCreditForReport)));
        return centralizingJournalByMonthReportLineDtos;
    }

    private void setLinesToBeFiltered(int breakingAccount, Map<Integer, List<DocumentAccountLine>> linesByChartAccount,
            List<DocumentAccountLine> linesToBeFiltered) {
        linesToBeFiltered.forEach((DocumentAccountLine documentAccountLine) -> {
            String accountCode = String.valueOf(documentAccountLine.getAccount().getCode());
            int chartCode = Integer.parseInt(accountCode.substring(0, breakingAccount));
            if (linesByChartAccount.containsKey(chartCode)) {
                linesByChartAccount.get(chartCode).add(documentAccountLine);
            } else {
                List<DocumentAccountLine> linesToBeAdded = new ArrayList<>();
                linesToBeAdded.add(documentAccountLine);
                linesByChartAccount.put(chartCode, linesToBeAdded);
            }
        });
    }

    public void getCentralizingJournals(LocalDateTime startDate, LocalDateTime endDate,
            Collection<JournalDto> journalDtos, Iterator<Long> journalIdsIterator, int breakingAccount,
            int breakingCustomerAccount, int breakingSupplierAccount) {
        while (journalIdsIterator.hasNext()) {
            JournalDto journalDto = findById(journalIdsIterator.next());
            addJournalDtoToList(startDate, endDate, journalDtos, journalDto, breakingAccount, breakingCustomerAccount,
                    breakingSupplierAccount);
        }
    }

    public JournalDto addJournalDtoToList(LocalDateTime startDate, LocalDateTime endDate,
            Collection<JournalDto> journalDtos, JournalDto journalDto, int breakingAccount, int breakingCustomerAccount,
            int breakingSupplierAccount) {
        List<CentralizingJournalDetailsByMonthDto> centralizingJournalDetailsByMonthDtos = new ArrayList<>();
        AccountingConfigurationDto accountingConfigurationDto = accountingConfigurationService.findLastConfig();
        centralizingJournalDetailsByMonthDtos = getCentralizingJournalDetails(startDate, endDate, breakingAccount,
                breakingCustomerAccount, breakingSupplierAccount, centralizingJournalDetailsByMonthDtos,
                accountingConfigurationDto, journalDto.getId());
        BigDecimal journalDebitAmount = JournalServiceUtil
                .calculateJournalDebitAmount(centralizingJournalDetailsByMonthDtos);
        BigDecimal journalCreditAmount = JournalServiceUtil
                .calculateJournalCreditAmount(centralizingJournalDetailsByMonthDtos);

        JournalDto journalDtoToAdd = new JournalDto(journalDto.getId(), journalDto.getCode(), journalDto.getLabel(),
                journalDebitAmount, journalCreditAmount);
        journalDtos.add(journalDtoToAdd);
        return journalDtoToAdd;
    }

    public void getCentralizingJournalOutOfTiersDetails(Long journalId, LocalDateTime startDate, LocalDateTime endDate,
            int breakingAccount, AccountingConfigurationDto accountingConfigurationDto,
            Collection<CentralizingJournalDetailsByMonthDto> centralizingJournalDetailsByMonthDtos) {
        int breakingCode = JournalServiceUtil.getDivisionCode(breakingAccount);
        List<CentralizingJournalDetailsByMonthDto> centralizingJournalDetailsDtosToAdd = documentAccountLineService
                .getCentralizingJournalDetailsDto(journalId, startDate, endDate, breakingCode,
                        String.valueOf(accountingConfigurationDto.getCustomerAccountCode()),
                        String.valueOf(accountingConfigurationDto.getSupplierAccountCode()));
        centralizingJournalDetailsByMonthDtos.addAll(centralizingJournalDetailsDtosToAdd);
    }

    public void getCentralizingJournalTiersDetails(Long journalId, LocalDateTime startDate, LocalDateTime endDate,
            int breakingAccount, int tierAccountCode,
            Collection<CentralizingJournalDetailsByMonthDto> centralizingJournalDetailsByMonthDtos) {
        int breakingCode = JournalServiceUtil.getDivisionCode(breakingAccount);
        List<CentralizingJournalDetailsByMonthDto> centralizingJournalDetailsDtosToAdd = documentAccountLineService
                .getCentralizingJournalTiersDetailsDto(journalId, startDate, endDate, breakingCode,
                        String.valueOf(tierAccountCode));
        centralizingJournalDetailsByMonthDtos.addAll(centralizingJournalDetailsDtosToAdd);

    }

    public List<ChartAccounts> getChartAccountOfJournal(int breakingAccount,
            AccountingConfigurationDto accountingConfigurationDto) {
        return chartAccountService.findChartAccountByLength(breakingAccount,
                accountingConfigurationDto.getCustomerAccountCode(),
                accountingConfigurationDto.getSupplierAccountCode());
    }

    public List<ChartAccounts> getChartAccountCustomersOfJournal(int breakingCustomerAccount,
            AccountingConfigurationDto accountingConfigurationDto) {
        return chartAccountService.findChartAccountTierByLength(breakingCustomerAccount,
                accountingConfigurationDto.getCustomerAccountCode());
    }

    public List<ChartAccounts> getChartAccountSuppliersOfJournal(int breakingSupplierAccount,
            AccountingConfigurationDto accountingConfigurationDto) {
        return chartAccountService.findChartAccountTierByLength(breakingSupplierAccount,
                accountingConfigurationDto.getSupplierAccountCode());
    }

    public void putChartAccountCodes(int breakingAccount, int breakingCustomerAccount, int breakingSupplierAccount,
            AccountingConfigurationDto accountingConfigurationDto, Map<String, List<ChartAccounts>> chartAccountHash) {
        chartAccountHash.put(CHART_ACCOUNT_CODE_OUT_OF_TIERS,
                getChartAccountOfJournal(breakingAccount, accountingConfigurationDto));
        chartAccountHash.put(CHART_ACCOUNT_CODE_CUSTOMERS,
                getChartAccountCustomersOfJournal(breakingCustomerAccount, accountingConfigurationDto));
        chartAccountHash.put(CHART_ACCOUNT_CODE_SUPPLIERS,
                getChartAccountSuppliersOfJournal(breakingSupplierAccount, accountingConfigurationDto));
    }

    public List<CentralizingJournalDetailsByMonthDto> getCentralizingJournalDetails(LocalDateTime startDate,
            LocalDateTime endDate, int breakingAccount, int breakingCustomerAccount, int breakingSupplierAccount,
            List<CentralizingJournalDetailsByMonthDto> centralizingJournalAccountsDtos,
            AccountingConfigurationDto accountingConfigurationDto, Long journalId) {
        getCentralizingJournalOutOfTiersDetails(journalId, startDate, endDate, breakingAccount,
                accountingConfigurationDto, centralizingJournalAccountsDtos);
        getCentralizingJournalTiersDetails(journalId, startDate, endDate, breakingCustomerAccount,
                accountingConfigurationDto.getCustomerAccountCode(), centralizingJournalAccountsDtos);
        getCentralizingJournalTiersDetails(journalId, startDate, endDate, breakingSupplierAccount,
                accountingConfigurationDto.getSupplierAccountCode(), centralizingJournalAccountsDtos);
        Map<Integer, List<CentralizingJournalDetailsByMonthDto>> centralizingJournalDetailsDtosMap = centralizingJournalAccountsDtos
                .stream().collect(Collectors.groupingBy(CentralizingJournalDetailsByMonthDto::getPlanCode));
        List<CentralizingJournalDetailsByMonthDto> centralizingJournalDetailsByMonthDtos = new ArrayList<>();
        for (Map.Entry<Integer, List<CentralizingJournalDetailsByMonthDto>> centralizingJournalDetailsByMonthDto : centralizingJournalDetailsDtosMap
                .entrySet()) {
            BigDecimal creditAmount = centralizingJournalDetailsByMonthDto.getValue().stream()
                    .map(CentralizingJournalDetailsByMonthDto::getCreditAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal debitAmount = centralizingJournalDetailsByMonthDto.getValue().stream()
                    .map(CentralizingJournalDetailsByMonthDto::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            ChartAccountsDto chartAccountsDto;
            int chartAccountCode = centralizingJournalDetailsByMonthDto.getKey();
            do {
                chartAccountsDto = chartAccountService.findByCode(chartAccountCode);
                if (chartAccountCode > NumberConstant.NINE) {
                    chartAccountCode /= NumberConstant.TEN;
                }
            } while (chartAccountsDto.getCode() == NumberConstant.ZERO);
            centralizingJournalDetailsByMonthDtos
                    .add(new CentralizingJournalDetailsByMonthDto(centralizingJournalDetailsByMonthDto.getKey(),
                            chartAccountsDto.getLabel(), debitAmount, creditAmount));
        }
        return centralizingJournalDetailsByMonthDtos;

    }

    @Override
    public List<Journal> findByIds(List<Long> journalIds) {
        return journalDao.findByIds(journalIds);
    }

    @Override
    public Page<JournalDto> filterJournal(List<Filter> filters, Pageable pageable) {

        if (!pageable.getSort().get().findFirst().isPresent()) {
            pageable = AccountingServiceUtil.getPageable(pageable.getPageNumber(), pageable.getPageSize(),
                    getDefaultSortFieldForJournal(), Sort.Direction.ASC.toString());
        }

        Page<Journal> page = FilterService.getPageOfFilterableEntity(Journal.class, journalDao, filters, pageable);

        return new PageImpl<>(JournalConverter.modelsToDtos(page.getContent()), pageable, page.getTotalElements());
    }

    public BigDecimal getTotalCentralizingJournal(LocalDateTime startDate, LocalDateTime endDate,
            Iterable<Long> journalIds, int breakingAccount, int breakingCustomerAccount, int breakingSupplierAccount) {
        List<JournalDto> journalDtos = new ArrayList<>();
        getCentralizingJournals(startDate, endDate, journalDtos, journalIds.iterator(), breakingAccount,
                breakingCustomerAccount, breakingSupplierAccount);
        return journalDtos.stream().map(JournalDto::getJournalDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public byte[] exportJournalsExcelModel() {
        File file = GenericExcelPOIHelper.generateXLSXFileFromData(new ArrayList<>(),
                String.format(IMPORT_MODEL_FILE_NAME, LanguageConstants.JOURNAL_SHEET_NAME), excelStoragePath.toFile(),
                acceptedHeaders, excelHeaderFields, LanguageConstants.JOURNAL_SHEET_NAME);
        return GenericExcelPOIHelper.convertFileToByteArray(file);
    }

    @Caching(evict = { @CacheEvict(value = "GeneralLedgerAccounts", allEntries = true),
            @CacheEvict(value = "GeneralLedgerAccountDetails", allEntries = true),
            @CacheEvict(value = "TrialBalanceAccounts", allEntries = true) })
    @Override
    public FileUploadDto loadJournalsExcelData(FileUploadDto fileUploadDto) {
        List<JournalDto> journals = new ArrayList<>();
        boolean allSheetsAreEmpty;
        boolean journalsAreValid = true;
        ExcelCellStyleHelper.resetStyles();
        try (Workbook workbook = GenericExcelPOIHelper
                .createWorkBookFromBase64String(fileUploadDto.getBase64Content())) {
            allSheetsAreEmpty = true;
            if (workbook.getNumberOfSheets() == 0) {
                throw new HttpCustomException(ApiErrors.Accounting.EXCEL_EMPTY_FILE);
            }
            GenericExcelPOIHelper.validateWorkbookSheetsHeaders(workbook, acceptedHeaders);
            List<String> previousJournalCodes = new ArrayList<>();
            List<String> previousJournalLabels = new ArrayList<>();
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                log.info("Parsing sheet #{}", sheetIndex + 1);
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                boolean isSheetEmpty = GenericExcelPOIHelper.isSheetEmpty(sheet);
                allSheetsAreEmpty &= isSheetEmpty;
                if (isSheetEmpty) {
                    continue;
                }
                journalsAreValid &= isJournalValuesAddedToSheet(journals, journalsAreValid, sheet, previousJournalCodes,
                        previousJournalLabels);
            }
            if (allSheetsAreEmpty) {
                log.error("Trying to import empty document");
                throw new HttpCustomException(ApiErrors.Accounting.EXCEL_EMPTY_FILE);
            } else if (journalsAreValid) {
                log.info("Saving journals");
                saveJournalsComingFromExcel(journals);
                return new FileUploadDto();
            } else {
                return GenericExcelPOIHelper.getFileUploadDtoFromWorkbook(workbook, excelStoragePath.toFile(),
                        String.format(SIMULATION_EXPORT_FILE_NAME, LanguageConstants.JOURNAL_SHEET_NAME));
            }
        } catch (IOException e) {
            log.error(AccountingConstants.ERROR_CREATING_FILE, e);
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_FILE_CREATION_FAIL);
        }
    }

    private boolean isJournalValuesAddedToSheet(List<JournalDto> journals, boolean journalsAreValid, Sheet sheet,
            List<String> previousJournalCodes, List<String> previousJournalLabels) {
        JournalDto journal;
        for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            log.info("Parsing row #{} in sheet {}", rowIndex, sheet.getSheetName());
            Row row = sheet.getRow(rowIndex);
            if (GenericExcelPOIHelper.isRowNotEmpty(row)) {
                GenericExcelPOIHelper.validateNumberOfCellsInRowAgainstHeaders(row, acceptedHeaders.size());
                journal = new JournalDto();
                journals.add(journal);
                journalsAreValid &= journalValuesAddedToRow(journal, row, excelHeaderFields, previousJournalCodes,
                        previousJournalLabels);
            }
        }
        return journalsAreValid;
    }

    private boolean journalValuesAddedToRow(JournalDto journal, Row row, List<Field> excelHeaderFields,
            List<String> previousJournalCodes, List<String> previousJournalLabels) {
        boolean isValid = true;
        for (int i = 0; i < excelHeaderFields.size(); i++) {
            Cell cell = row.getCell(i);
            if (cell == null) {
                cell = row.createCell(i);
            }
            cell.setCellComment(null);
            switch (excelHeaderFields.get(i).getAnnotation(JournalExcelCell.class).headerName()) {
            case LanguageConstants.XLSXHeaders.JOURNAL_CODE_HEADER_NAME:
                isValid &= isJournalCodeSet(cell, journal, previousJournalCodes);
                break;
            case LanguageConstants.XLSXHeaders.LABEL_HEADER_NAME:
                isValid &= isJournalLabelSet(cell, journal, previousJournalLabels);
                break;
            case LanguageConstants.XLSXHeaders.RECONCILABLE_HEADER_NAME:
                isValid &= isJournalReconcilableSet(cell, journal);
                break;
            case LanguageConstants.XLSXHeaders.CASH_FLOW_HEADER_NAME:
                isValid &= isJournalCashFlowSet(cell, journal);
                break;
            default:
                isValid = false;
            }
        }
        return isValid;
    }

    private boolean isJournalLabelSet(Cell cell, JournalDto journal, List<String> previousJournalLabels) {
        if (isJournalLabelInCellValid(cell, previousJournalLabels)) {
            setJournalLabelFromCell(cell, journal, previousJournalLabels);
            return true;
        }
        return false;
    }

    private boolean isJournalLabelInCellValid(Cell cell, List<String> previousJournalLabels) {
        String journalLabel = dataFormatter.formatCellValue(cell).trim();
        if (journalLabel.length() < THREE) {
            ExcelCellStyleHelper.setInvalidCell(cell,
                    String.format(XLSXErrors.JournalXLSXErrors.JOURNAL_LABEL_LENGTH_MUST_BE_AT_LEAST, THREE));
        } else {
            Journal journalWithLabel = findJournalByLabel(journalLabel);
            if (journalWithLabel != null || previousJournalLabels.contains(journalLabel)) {
                ExcelCellStyleHelper.setInvalidCell(cell, XLSXErrors.JournalXLSXErrors.JOURNAL_WITH_LABEL_EXISTS);
            } else {
                return true;
            }
        }
        return false;
    }

    public boolean isJournalCodeSet(Cell cell, JournalDto journal, List<String> previousJournalCodes) {
        if (isJournalCodeInCellValid(cell, previousJournalCodes)) {
            setJournalCodeFromCell(cell, journal, previousJournalCodes);
            return true;
        }
        return false;
    }

    public void setJournalCodeFromCell(Cell cell, JournalDto journal, Collection<String> previousJournalCodes) {
        String journalCode = dataFormatter.formatCellValue(cell).trim();
        journal.setCode(journalCode);
        previousJournalCodes.add(journalCode);
    }

    public void setJournalLabelFromCell(Cell cell, JournalDto journal, Collection<String> previousJournalLabels) {
        String journalLabel = dataFormatter.formatCellValue(cell).trim();
        journal.setLabel(journalLabel);
        previousJournalLabels.add(journalLabel);
    }

    private boolean isJournalCodeInCellValid(Cell cell, List<String> previousJournalCodes) {
        String journalCode = dataFormatter.formatCellValue(cell).trim();
        if (journalCode.length() < TWO || journalCode.length() > TEN) {
            ExcelCellStyleHelper.setInvalidCell(cell,
                    String.format(XLSXErrors.JournalXLSXErrors.JOURNAL_CODE_LENGTH_MUST_BE_BETWEEN, TWO, TEN));
        } else {
            Journal journalWithCode = findByCode(journalCode);
            if (journalWithCode != null || previousJournalCodes.contains(journalCode)) {
                ExcelCellStyleHelper.setInvalidCell(cell, XLSXErrors.JournalXLSXErrors.JOURNAL_WITH_CODE_EXISTS);
            } else {
                return true;
            }
        }
        return false;
    }

    public boolean isJournalReconcilableSet(Cell cell, JournalDto journal) {
        String cellValue = dataFormatter.formatCellValue(cell).trim();
        CellType cellType = cell.getCellType();
        if (CellType.BOOLEAN.equals(cellType)) {
            journal.setReconcilable(cell.getBooleanCellValue());
            return true;
        } else if (CellType.STRING.equals(cellType) && (LanguageConstants.TRUE.equalsIgnoreCase(cellValue)
                || LanguageConstants.FALSE.equalsIgnoreCase(cellValue))) {
            journal.setReconcilable(LanguageConstants.TRUE.equals(cellValue));
            return true;
        } else {
            ExcelCellStyleHelper.setInvalidCell(cell, String.format(XLSXErrors.RECONCILABLE_VALUE_NOT_ALLOWED,
                    LanguageConstants.TRUE, LanguageConstants.FALSE));
        }
        return false;
    }

    public boolean isJournalCashFlowSet(Cell cell, JournalDto journal) {
        String cellValue = dataFormatter.formatCellValue(cell).trim();
        CellType cellType = cell.getCellType();
        if (CellType.BOOLEAN.equals(cellType)) {
            journal.setCashFlow(cell.getBooleanCellValue());
            return true;
        } else if (CellType.STRING.equals(cellType) && (LanguageConstants.TRUE.equalsIgnoreCase(cellValue)
                || LanguageConstants.FALSE.equalsIgnoreCase(cellValue))) {
            journal.setCashFlow(LanguageConstants.TRUE.equals(cellValue));
            return true;
        } else {
            ExcelCellStyleHelper.setInvalidCell(cell, String.format(XLSXErrors.CASH_FLOW_VALUE_NOT_ALLOWED,
                    LanguageConstants.TRUE, LanguageConstants.FALSE));
        }
        return false;
    }

    private void saveJournalsComingFromExcel(List<JournalDto> journals) {
        if (journals.isEmpty()) {
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_NO_JOURNALS_TO_BE_SAVED);
        }
        for (JournalDto journal : journals) {
            save(journal);
        }
    }

    @Override
    public byte[] exportJournalsAsExcelFile() {
        List<Journal> journals = findAll();
        journals.sort(Comparator.comparing(Journal::getCode));
        List<Object> journalsXLSXFormatDtoList = new ArrayList<>();
        for (Journal journal : journals) {
            JournalXLSXFormatDto journalXLSXFormatDto = new JournalXLSXFormatDto();
            journalXLSXFormatDto.setCode(journal.getCode());
            journalXLSXFormatDto.setLabel(journal.getLabel());
            journalXLSXFormatDto.setLabel(journal.getLabel());
            if (journal.isReconcilable()) {
                journalXLSXFormatDto.setReconcilable(LanguageConstants.TRUE);
            } else {
                journalXLSXFormatDto.setReconcilable(LanguageConstants.FALSE);
            }
            if (journal.isCashFlow()) {
                journalXLSXFormatDto.setCashFlow(LanguageConstants.TRUE);
            } else {
                journalXLSXFormatDto.setCashFlow(LanguageConstants.FALSE);
            }
            journalsXLSXFormatDtoList.add(journalXLSXFormatDto);
        }
        File file = GenericExcelPOIHelper.generateXLSXFileFromData(journalsXLSXFormatDtoList,
                String.format(EXPORT_FILE_NAME, LanguageConstants.JOURNAL_SHEET_NAME), excelStoragePath.toFile(),
                acceptedHeaders, excelHeaderFields, LanguageConstants.JOURNAL_SHEET_NAME);
        return GenericExcelPOIHelper.convertFileToByteArray(file);
    }

}
