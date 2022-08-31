package fr.sparkit.accounting.services.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.constants.NumberConstant;
import fr.sparkit.accounting.convertor.ReportLineConverter;
import fr.sparkit.accounting.dao.DocumentAccountLineDao;
import fr.sparkit.accounting.dao.ReportLineDao;
import fr.sparkit.accounting.dto.AccountBalanceDto;
import fr.sparkit.accounting.dto.AccountingConfigurationDto;
import fr.sparkit.accounting.dto.AmortizationTableReportDto;
import fr.sparkit.accounting.dto.AnnexeDetailsDto;
import fr.sparkit.accounting.dto.AnnexeReportDto;
import fr.sparkit.accounting.dto.DepreciationAssetsDto;
import fr.sparkit.accounting.dto.FiscalYearDto;
import fr.sparkit.accounting.dto.ReportLineDto;
import fr.sparkit.accounting.dto.StandardReportLineDto;
import fr.sparkit.accounting.entities.Account;
import fr.sparkit.accounting.entities.ChartAccounts;
import fr.sparkit.accounting.entities.FiscalYear;
import fr.sparkit.accounting.entities.ReportLine;
import fr.sparkit.accounting.entities.StandardReportLine;
import fr.sparkit.accounting.enumuration.FiscalYearClosingState;
import fr.sparkit.accounting.enumuration.ReportFormulaSuffix;
import fr.sparkit.accounting.enumuration.ReportType;
import fr.sparkit.accounting.services.IAccountService;
import fr.sparkit.accounting.services.IAccountingConfigurationService;
import fr.sparkit.accounting.services.IChartAccountsService;
import fr.sparkit.accounting.services.IDepreciationAssetService;
import fr.sparkit.accounting.services.IFiscalYearService;
import fr.sparkit.accounting.services.IInsertIntoReportLineService;
import fr.sparkit.accounting.services.IReportLineService;
import fr.sparkit.accounting.util.CalculationUtil;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.errors.ErrorsResponse;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ReportLineService extends GenericService<ReportLine, Long> implements IReportLineService {

    private static final String LINE_INDEX_FORMAT = "(%d)";

    private final IChartAccountsService chartAccountsService;
    private final IFiscalYearService fiscalYearService;
    private final IAccountingConfigurationService accountingConfigurationService;
    private final IInsertIntoReportLineService insertAnnexeLineIntoReport;
    private final IInsertIntoReportLineService insertAccountLineIntoReport;
    private final IInsertIntoReportLineService insertTotalLineIntoReport;
    private final IDepreciationAssetService depreciationAssetService;
    private final StandardReportLineService standardReportLineService;
    private final IAccountService accountService;
    private final DocumentAccountLineDao documentAccountLineDao;
    private final ReportLineDao reportLineDao;

    @Autowired
    public ReportLineService(IChartAccountsService chartAccountsService, DocumentAccountLineDao documentAccountLineDao,
            ReportLineDao reportLineDao, IFiscalYearService fiscalYearService,
            IAccountingConfigurationService accountingConfigurationService, AccountService accountService,
            DepreciationAssetService depreciationAssetService, StandardReportLineService standardReportLineService,
            @Qualifier("annexeLine") IInsertIntoReportLineService insertAnnexeLineIntoReport,
            @Qualifier("accountLines") IInsertIntoReportLineService insertAccountLineIntoReport,
            @Qualifier("totalLine") IInsertIntoReportLineService insertTotalLineIntoReport) {
        this.chartAccountsService = chartAccountsService;
        this.documentAccountLineDao = documentAccountLineDao;
        this.reportLineDao = reportLineDao;
        this.fiscalYearService = fiscalYearService;
        this.accountingConfigurationService = accountingConfigurationService;
        this.insertAnnexeLineIntoReport = insertAnnexeLineIntoReport;
        this.insertAccountLineIntoReport = insertAccountLineIntoReport;
        this.insertTotalLineIntoReport = insertTotalLineIntoReport;
        this.depreciationAssetService = depreciationAssetService;
        this.standardReportLineService = standardReportLineService;
        this.accountService = accountService;
    }

    @Override
    public List<ReportLineDto> getReportLinesForReportType(ReportType reportType) {
        List<ReportLineDto> reportLines = ReportLineConverter
                .modelsToDtos(this.reportLineDao.findByReportTypeAndFiscalYearIdAndIsDeletedFalseOrderById(reportType,
                        accountingConfigurationService.getCurrentFiscalYearId()));
        if (!reportLines.isEmpty()) {
            return reportLines;
        } else {
            return generateAnnualReport(reportType, accountingConfigurationService.getCurrentFiscalYearId(),
                    StringUtils.EMPTY);
        }
    }

    @Override
    public synchronized List<ReportLineDto> generateAnnualReport(ReportType reportType, Long fiscalYearId,
            String user) {
        FiscalYearDto fiscalYear = fiscalYearService.findById(fiscalYearId);
        if (ReportType.BS.equals(reportType)
                && !reportLineDao.findFirstByReportTypeAndFiscalYearId(ReportType.SOI, fiscalYearId).isPresent()) {
            generateAnnualReport(ReportType.SOI, fiscalYearId, user);
        }
        List<ReportLineDto> reportLines = ReportLineConverter.modelsToDtos(
                reportLineDao.findByReportTypeAndFiscalYearIdAndIsDeletedFalseOrderById(reportType, fiscalYearId));
        if (reportLines.isEmpty()) {
            reportLines = initReportLinesForFiscalYear(reportType, fiscalYearId);
        }
        LocalDateTime generationRequestTime = LocalDateTime.now();
        if (fiscalYear.getClosingState() != FiscalYearClosingState.CLOSED.getValue()) {
            log.info(AccountingConstants.GENERATE_ANNUAL_REPORT_FISCAL_YEAR_OF_USER, reportType, fiscalYearId, user);
            Optional<FiscalYear> previousFiscalYear = fiscalYearService.findPreviousFiscalYear(fiscalYearId);

            List<ReportLine> previousFiscalYearReportLines = new ArrayList<>();
            if (previousFiscalYear.isPresent()) {
                previousFiscalYearReportLines = reportLineDao.findByReportTypeAndFiscalYearIdAndIsDeletedFalseOrderById(
                        reportType, previousFiscalYear.get().getId());
            }
            List<ChartAccounts> chartAccounts = chartAccountsService.findAll();
            List<Account> accounts = accountService.findAll();
            for (ReportLineDto reportLine : reportLines) {
                if (previousFiscalYear.isPresent()) {
                    Optional<ReportLine> reportLineForPreviousFiscalYearOpt = previousFiscalYearReportLines.stream()
                            .filter(previousReportLine -> previousReportLine.getLineIndex()
                                    .equals(reportLine.getLineIndex()))
                            .findFirst();
                    reportLine.setPreviousFiscalYear(previousFiscalYear.get().getName());
                    reportLineForPreviousFiscalYearOpt
                            .ifPresent(line -> reportLine.setPreviousFiscalYearAmount(line.getAmount()));
                } else {
                    reportLine.setPreviousFiscalYear("-");
                }
                BigDecimal currentFiscalYearValue = interpretReportLineFormula(fiscalYearId, reportLines, reportLine,
                        previousFiscalYearReportLines, chartAccounts, accounts);
                reportLine.setAmount(currentFiscalYearValue);
                reportLine.setLastUpdated(generationRequestTime);
                reportLine.setUser(user);
            }
            saveReportLines(ReportLineConverter.dtosToModels(reportLines));
            log.info(AccountingConstants.LOG_ENTITY_UPDATED, reportLines);
        }
        return reportLines;
    }

    @Override
    public void checkAccountsBalanced(Long fiscalYearId) {
        for (Integer chartAccountCode : chartAccountsService.getChartAccountsCodeToBalanced()) {
            Optional<BigDecimal> resultCharAccountBalanceOpt = documentAccountLineDao
                    .totalDifferenceBetweenCreditAndDebitByChartAccountInFiscalYear(chartAccountCode.toString(),
                            fiscalYearId);
            if (resultCharAccountBalanceOpt.isPresent()
                    && resultCharAccountBalanceOpt.get().compareTo(BigDecimal.ZERO) != 0) {
                throw new HttpCustomException(ApiErrors.Accounting.ACCOUNT_NOT_BALANCED,
                        new ErrorsResponse().error(chartAccountCode.toString()));
            }
        }
    }

    @Override
    public List<Integer> getUnbalancedChartAccounts(Long fiscalYearId) {
        List<Integer> unbalanceChartAccounts = new ArrayList<>();
        for (Integer chartAccountCode : chartAccountsService.getChartAccountsCodeToBalanced()) {
            Optional<BigDecimal> resultCharAccountBalanceOpt = documentAccountLineDao
                    .totalDifferenceBetweenCreditAndDebitByChartAccountInFiscalYear(chartAccountCode.toString(),
                            fiscalYearId);
            if (resultCharAccountBalanceOpt.isPresent()
                    && resultCharAccountBalanceOpt.get().compareTo(BigDecimal.ZERO) != 0) {
                unbalanceChartAccounts.add(chartAccountCode);
            }
        }
        return unbalanceChartAccounts;
    }

    BigDecimal interpretReportLineFormula(Long fiscalYearId, List<ReportLineDto> reportLines, ReportLineDto reportLine,
            List<ReportLine> previousFiscalYearReportLines, List<ChartAccounts> chartAccounts, List<Account> accounts) {
        BigDecimal currentFiscalYearValue;
        if (CalculationUtil.isFormulaComplex(reportLine.getFormula())) {
            currentFiscalYearValue = interpretComplexFormula(
                    CalculationUtil.complexStringFormulaToCollection(reportLine.getFormula()), reportLines);
        } else {
            boolean isCashFlowReport = ReportType.CF.equals(reportLine.getReportType())
                    || ReportType.CFA.equals(reportLine.getReportType());
            currentFiscalYearValue = interpretSimpleFormula(
                    CalculationUtil.divideSimpleStringFormulaToStringElements(reportLine.getFormula()), fiscalYearId,
                    previousFiscalYearReportLines, chartAccounts, accounts, isCashFlowReport);
        }
        return currentFiscalYearValue.multiply(new BigDecimal(getSignMultiplicationValue(reportLine)));
    }

    @Override
    public List<ReportLineDto> getAnnualReport(ReportType reportType, Long fiscalYearId) {
        if (fiscalYearService.existsById(fiscalYearId)) {
            List<ReportLineDto> reportLines = ReportLineConverter.modelsToDtos(
                    reportLineDao.findByReportTypeAndFiscalYearIdAndIsDeletedFalseOrderById(reportType, fiscalYearId));

            Optional<FiscalYear> previousFiscalYear = fiscalYearService.findPreviousFiscalYear(fiscalYearId);
            if (previousFiscalYear.isPresent()) {
                reportLines.forEach((ReportLineDto reportLine) -> {
                    reportLine.setPreviousFiscalYear(previousFiscalYear.get().getName());
                    Optional<ReportLine> previousFiscalYearReportLine = reportLineDao
                            .findByReportTypeAndLineIndexAndFiscalYearIdAndIsDeletedFalse(reportType,
                                    reportLine.getLineIndex(), previousFiscalYear.get().getId());
                    previousFiscalYearReportLine
                            .ifPresent(line -> reportLine.setPreviousFiscalYearAmount(line.getAmount()));
                });
            }
            if (reportLines.isEmpty()) {
                reportLines = generateAnnualReport(reportType, fiscalYearId, StringUtils.EMPTY);
            }
            return reportLines;
        } else {
            throw new HttpCustomException(ApiErrors.Accounting.FISCAL_YEAR_INEXISTANT_FISCAL_YEAR,
                    new ErrorsResponse().error(fiscalYearId));
        }
    }

    @Override
    public List<ReportLine> findByFiscalYearIdAndIsDeletedFalseOrderById(Long fiscalYearId) {
        return reportLineDao.findByFiscalYearIdAndIsDeletedFalseOrderById(fiscalYearId);
    }

    @Override
    public List<ReportLineDto> initReportLinesForFiscalYear(ReportType reportType, Long fiscalYearId) {
        List<StandardReportLineDto> standardReportLines = standardReportLineService
                .getReportLinesForReportType(reportType);
        FiscalYear fiscalYear = fiscalYearService.findOne(fiscalYearId);
        List<ReportLine> reportLines = new ArrayList<>();
        for (StandardReportLineDto standardReportLine : standardReportLines) {
            ReportLine reportLine = new ReportLine();
            reportLine.setId(null);
            reportLine.setAmount(BigDecimal.ZERO);
            reportLine.setFiscalYear(fiscalYear);
            reportLine.setAnnexCode(standardReportLine.getAnnexCode());
            reportLine.setLineIndex(standardReportLine.getLineIndex());
            reportLine.setReportType(standardReportLine.getReportType());
            reportLine.setFormula(standardReportLine.getFormula());
            reportLine.setLabel(standardReportLine.getLabel());
            reportLine.setNegative(standardReportLine.isNegative());
            reportLines.add(reportLine);
        }
        return ReportLineConverter.modelsToDtos(saveReportLines(reportLines));
    }

    @Override
    public void resetReportLinesForFiscalYear(Long fiscalYearId) {
        if (!fiscalYearService.existsById(fiscalYearId)) {
            throw new HttpCustomException(ApiErrors.Accounting.FISCAL_YEAR_INEXISTANT_FISCAL_YEAR,
                    new ErrorsResponse().error(fiscalYearId));
        }
        List<ReportLine> reportLines = reportLineDao.findByFiscalYearIdAndIsDeletedFalseOrderById(fiscalYearId);
        for (ReportLine reportLine : reportLines) {
            reportLine.setAmount(BigDecimal.ZERO);
        }
        saveReportLines(reportLines);
        log.info(AccountingConstants.LOG_ENTITY_UPDATED, reportLines);
    }

    @Override
    public ReportLineDto saveReportLine(ReportLineDto reportLineDto, String user) {
        int currentFiscalYearClosingState = fiscalYearService.findById(reportLineDto.getFiscalYear().getId())
                .getClosingState();
        if (currentFiscalYearClosingState == FiscalYearClosingState.OPEN.getValue()
                || currentFiscalYearClosingState == FiscalYearClosingState.PARTIALLY_CLOSED.getValue()) {
            Optional<ReportLine> reportLineToBeSaved = reportLineDao.findById(reportLineDto.getId());
            if (reportLineToBeSaved.isPresent()) {
                if (reportLineHasBeenChanged(reportLineDto, reportLineToBeSaved.get())) {
                    reportLineToBeSaved.get().setManuallyChanged(true);
                }
                setValueAnnexCode(reportLineDto, reportLineToBeSaved.get());
                reportLineToBeSaved.get().setFormula(reportLineDto.getFormula());
                reportLineToBeSaved.get().setNegative(reportLineDto.isNegative());
                reportLineToBeSaved.get().setAmount(BigDecimal.ZERO);
                reportLineToBeSaved.get().setUser(user);
                reportLineToBeSaved.get().setLastUpdated(LocalDateTime.now());
                validateFormula(reportLineToBeSaved.get());
                return ReportLineConverter.modelToDto(saveAndFlush(reportLineToBeSaved.get()));
            } else {
                throw new HttpCustomException(ApiErrors.Accounting.REPORT_LINE_INEXISTANT_REPORT_LINE,
                        new ErrorsResponse().error(reportLineDto.getId()));
            }
        } else {
            throw new HttpCustomException(ApiErrors.Accounting.REPORT_LINE_CANNOT_UPDATE_WHEN_FISCAL_YEAR_NOT_OPENED);
        }
    }

    private void setValueAnnexCode(ReportLineDto reportLineDto, ReportLine reportLineToBeSaved) {
        if (reportLineDto.getAnnexCode() != null) {
            Optional<ReportLine> reportLineWithSameAnnexCode = reportLineDao
                    .findFirstByAnnexCodeAndReportTypeAndFiscalYearId(reportLineDto.getAnnexCode(),
                            reportLineToBeSaved.getReportType(), reportLineToBeSaved.getFiscalYear().getId());
            if (reportLineWithSameAnnexCode.isPresent()
                    && !reportLineWithSameAnnexCode.get().getId().equals(reportLineToBeSaved.getId())) {
                throw new HttpCustomException(ApiErrors.Accounting.REPORT_LINE_ANNEX_ALREADY_EXISTS,
                        new ErrorsResponse().error(reportLineDto.getAnnexCode()));
            } else {
                reportLineToBeSaved.setAnnexCode(reportLineDto.getAnnexCode());
            }
        } else {
            reportLineToBeSaved.setAnnexCode(null);
        }
    }

    public boolean reportLineHasBeenChanged(ReportLineDto inputReportLine, ReportLine existingReportLine) {
        boolean hasBeenChanged = inputReportLine.isNegative() != existingReportLine.isNegative()
                || !inputReportLine.getFormula().equalsIgnoreCase(existingReportLine.getFormula());
        if (inputReportLine.getAnnexCode() != null) {
            hasBeenChanged = hasBeenChanged
                    || !inputReportLine.getAnnexCode().equalsIgnoreCase(existingReportLine.getAnnexCode());
        } else {
            hasBeenChanged = hasBeenChanged || existingReportLine.getAnnexCode() != null;
        }
        return hasBeenChanged;
    }

    @Override
    public ReportLineDto findById(Long id) {
        return ReportLineConverter.modelToDto(Optional.ofNullable(reportLineDao.findOne(id))
                .orElseThrow(() -> new HttpCustomException(ApiErrors.Accounting.REPORT_LINE_INEXISTANT_REPORT_LINE,
                        new ErrorsResponse().error(id))));
    }

    @Override
    public ReportLine getReportLineByReportTypeAndLineIndexAndFiscalYear(ReportType reportType, String index,
            Long fiscalYearId) {
        return reportLineDao
                .findByReportTypeAndLineIndexAndFiscalYearIdAndIsDeletedFalse(reportType, index, fiscalYearId)
                .orElseThrow(() -> new HttpCustomException(ApiErrors.Accounting.REPORT_LINE_INDEX_LINE_NOT_FOUND,
                        new ErrorsResponse().error(index)));
    }

    @Override
    public BigDecimal interpretSimpleFormula(Collection<String> formula, Long fiscalYearId,
            List<ReportLine> previousFiscalYearReportLines, List<ChartAccounts> chartAccounts, List<Account> accounts,
            boolean isCashFlowReport) {
        BigDecimal total = BigDecimal.ZERO;
        for (String formulaElement : formula) {
            int plan;
            if (Character.isDigit((formulaElement.charAt(formulaElement.length() - NumberConstant.ONE)))) {
                plan = Integer.parseInt(formulaElement);
                Optional<ChartAccounts> chartAccountOpt = chartAccounts.stream()
                        .filter(chartAccount -> chartAccount.getCode() == Math.abs(plan)).findFirst();
                if (chartAccountOpt.isPresent()) {
                    BigDecimal value;
                    if (!isCashFlowReport) {
                        value = documentAccountLineDao.totalDifferenceBetweenCreditAndDebitByChartAccountInFiscalYear(
                                Integer.toString(Math.abs(plan)), fiscalYearId).orElse(BigDecimal.ZERO);
                    } else {
                        value = documentAccountLineDao
                                .totalDifferenceBetweenCreditAndDebitByChartAccountInFiscalYearAndJournalIsCashFlow(
                                        Integer.toString(Math.abs(plan)), fiscalYearId)
                                .orElse(BigDecimal.ZERO);
                    }
                    if (plan > NumberConstant.ZERO) {
                        total = total.add(value);
                    } else {
                        total = total.subtract(value);
                    }

                }
            } else {
                plan = Integer.parseInt(formulaElement.substring(0, formulaElement.length() - 1));
                char symbol = formulaElement.charAt(formulaElement.length() - 1);
                List<Account> accountsHavingPlanCode = accounts.stream().filter(account -> Integer
                        .toString(account.getPlan().getCode()).startsWith(String.valueOf(Math.abs(plan))))
                        .collect(Collectors.toList());
                for (Account account : accountsHavingPlanCode) {
                    BigDecimal totalAmountForAccount;
                    if (!isCashFlowReport) {
                        totalAmountForAccount = documentAccountLineDao
                                .totalDifferenceBetweenCreditAndDebitByAccountInFiscalYear(account.getId(),
                                        fiscalYearId)
                                .orElse(BigDecimal.ZERO);
                    } else {
                        totalAmountForAccount = documentAccountLineDao
                                .totalDifferenceBetweenCreditAndDebitByAccountInFiscalYearAndJournalIsCashFlow(
                                        account.getId(), fiscalYearId)
                                .orElse(BigDecimal.ZERO);
                    }
                    if ((symbol == CalculationUtil.DEBIT_SYMBOL && totalAmountForAccount.compareTo(BigDecimal.ZERO) < 0)
                            || (symbol == CalculationUtil.CREDIT_SYMBOL
                                    && totalAmountForAccount.compareTo(BigDecimal.ZERO) > 0)) {
                        if (plan > NumberConstant.ZERO) {
                            total = total.add(totalAmountForAccount);
                        } else {
                            total = total.subtract(totalAmountForAccount);
                        }
                    }
                }
                total = addToTotal(fiscalYearId, total, plan, symbol, isCashFlowReport);
            }
        }
        return total;
    }

    private BigDecimal addToTotal(Long fiscalYearId, BigDecimal total, int plan, char symbol,
            boolean isCashFlowReport) {
        if (symbol == CalculationUtil.RESULT_SYMBOL) {
            Optional<ReportLine> resultReportLine = reportLineDao
                    .findByReportTypeAndLineIndexAndFiscalYearIdAndIsDeletedFalse(ReportType.SOI,
                            String.format(LINE_INDEX_FORMAT, Math.abs(plan)), fiscalYearId);
            if (resultReportLine.isPresent()) {
                if (plan > NumberConstant.ZERO) {
                    total = total.add(resultReportLine.get().getAmount());
                } else {
                    total = total.subtract(resultReportLine.get().getAmount());
                }
            }
        }
        if (symbol == CalculationUtil.BILAN_SYMBOL) {
            Optional<ReportLine> balanceReportLine = reportLineDao
                    .findByReportTypeAndLineIndexAndFiscalYearIdAndIsDeletedFalse(ReportType.BS,
                            String.format(LINE_INDEX_FORMAT, Math.abs(plan)), fiscalYearId);
            if (balanceReportLine.isPresent()) {
                if (plan > NumberConstant.ZERO) {
                    total = total.add(balanceReportLine.get().getAmount());
                } else {
                    total = total.subtract(balanceReportLine.get().getAmount());
                }
            }
        }
        Optional<FiscalYear> previousFiscalYear = fiscalYearService.findPreviousFiscalYear(fiscalYearId);
        if (symbol == CalculationUtil.VARIATION_SYMBOL) {
            if (fiscalYearService.findPreviousFiscalYear(fiscalYearId).isPresent()) {
                BigDecimal amountForCurrentFiscalYear;
                if (!isCashFlowReport) {
                    amountForCurrentFiscalYear = documentAccountLineDao
                            .totalDifferenceBetweenCreditAndDebitByChartAccountInFiscalYear(
                                    Integer.toString(Math.abs(plan)), fiscalYearId)
                            .orElse(BigDecimal.ZERO);
                } else {
                    amountForCurrentFiscalYear = documentAccountLineDao
                            .totalDifferenceBetweenCreditAndDebitByChartAccountInFiscalYearAndJournalIsCashFlow(
                                    Integer.toString(Math.abs(plan)), fiscalYearId)
                            .orElse(BigDecimal.ZERO);
                }
                BigDecimal amountForPreviousFiscalYear = BigDecimal.ZERO;
                if (previousFiscalYear.isPresent()) {
                    if (!isCashFlowReport) {
                        amountForPreviousFiscalYear = documentAccountLineDao
                                .totalDifferenceBetweenCreditAndDebitByChartAccountInFiscalYear(
                                        Integer.toString(Math.abs(plan)), previousFiscalYear.get().getId())
                                .orElse(BigDecimal.ZERO);
                    } else {
                        amountForPreviousFiscalYear = documentAccountLineDao
                                .totalDifferenceBetweenCreditAndDebitByChartAccountInFiscalYearAndJournalIsCashFlow(
                                        Integer.toString(Math.abs(plan)), previousFiscalYear.get().getId())
                                .orElse(BigDecimal.ZERO);
                    }
                }
                if (plan > NumberConstant.ZERO) {
                    total = total.add(amountForCurrentFiscalYear.subtract(amountForPreviousFiscalYear));
                } else {
                    total = total.subtract(amountForCurrentFiscalYear.subtract(amountForPreviousFiscalYear));
                }
            } else {
                BigDecimal amountForCurrentFiscalYear;
                if (!isCashFlowReport) {
                    amountForCurrentFiscalYear = documentAccountLineDao
                            .totalDifferenceBetweenCreditAndDebitByChartAccountInFiscalYear(
                                    Integer.toString(Math.abs(plan)), fiscalYearId)
                            .orElse(BigDecimal.ZERO);
                } else {
                    amountForCurrentFiscalYear = documentAccountLineDao
                            .totalDifferenceBetweenCreditAndDebitByChartAccountInFiscalYearAndJournalIsCashFlow(
                                    Integer.toString(Math.abs(plan)), fiscalYearId)
                            .orElse(BigDecimal.ZERO);
                }
                if (plan > NumberConstant.ZERO) {
                    total = total.add(amountForCurrentFiscalYear);
                } else {
                    total = total.subtract(amountForCurrentFiscalYear);
                }
            }
        }
        if (symbol == CalculationUtil.EVOLUTION_BILAN_SYMBOL) {
            Optional<ReportLine> balanceReportLine = reportLineDao
                    .findByReportTypeAndLineIndexAndFiscalYearIdAndIsDeletedFalse(ReportType.BS,
                            String.format(LINE_INDEX_FORMAT, plan), fiscalYearId);
            if (previousFiscalYear.isPresent()) {
                Optional<ReportLine> previousFiscalYearBalanceReportLine = reportLineDao
                        .findByReportTypeAndLineIndexAndFiscalYearIdAndIsDeletedFalse(ReportType.BS,
                                String.format(LINE_INDEX_FORMAT, plan), previousFiscalYear.get().getId());
                if (balanceReportLine.isPresent() && previousFiscalYearBalanceReportLine.isPresent()) {
                    total = total.add(balanceReportLine.get().getAmount()
                            .subtract(previousFiscalYearBalanceReportLine.get().getAmount()));
                }

            } else if (balanceReportLine.isPresent()) {
                total = total.add(balanceReportLine.get().getAmount());
            } else {
                total = BigDecimal.ZERO;
            }
        }
        return total;
    }

    @Override
    public BigDecimal interpretComplexFormula(Collection<Integer> formula, List<ReportLineDto> reportLines) {
        BigDecimal total = BigDecimal.ZERO;
        for (Integer indexNumber : formula) {
            Optional<ReportLineDto> reportLineDto = reportLines.stream().filter(reportLine -> reportLine.getLineIndex()
                    .equals(String.format(LINE_INDEX_FORMAT, Math.abs(indexNumber)))).findFirst();
            if (reportLineDto.isPresent()) {
                if (indexNumber < 0) {
                    total = total.subtract(reportLineDto.get().getAmount());
                } else {
                    total = total.add(reportLineDto.get().getAmount());
                }
            }
        }
        return total;
    }

    @Override
    public void validateFormula(ReportLine reportLine) {
        log.info(AccountingConstants.VALID_REPORT_LINE_FORMULA, reportLine.getFormula());
        if (!CalculationUtil.isFormulaComplex(reportLine.getFormula())) {
            Collection<String> formulaElementsElements = CalculationUtil
                    .divideSimpleStringFormulaToStringElements(reportLine.getFormula());
            checkForRepetitionsInFormula(Collections.singleton(formulaElementsElements), reportLine.getFormula());
        } else {
            Collection<Integer> lineIndexes = CalculationUtil.complexStringFormulaToCollection(reportLine.getFormula());
            for (Integer lineIndex : lineIndexes) {
                ReportLine reportLineWithIndex = getReportLineByReportTypeAndLineIndexAndFiscalYear(
                        reportLine.getReportType(), String.format(LINE_INDEX_FORMAT, Math.abs(lineIndex)),
                        reportLine.getFiscalYear().getId());
                if (reportLineWithIndex.getId() > reportLine.getId()) {
                    log.error(AccountingConstants.REPORT_LINE_INDEX_ORDER_INVALID);
                    throw new HttpCustomException(ApiErrors.Accounting.REPORT_LINE_INDEX_LINE_ORDER_INVALID,
                            new ErrorsResponse().error(String.format(LINE_INDEX_FORMAT, Math.abs(lineIndex))));
                }
            }
            checkForRepetitionsInFormula(Collections.singleton(lineIndexes), reportLine.getFormula());
        }
    }

    @Override
    public void checkForRepetitionsInFormula(Collection<Object> formulaElements, String formula) {
        if (formulaElements.size() != new HashSet<>(formulaElements).size()) {
            log.error(AccountingConstants.FORMULA_CONTAINS_DUPLICATE_ELEMENT, formula);
            throw new HttpCustomException(ApiErrors.Accounting.REPORT_LINE_FORMULA_CONTAINS_REPETITION,
                    new ErrorsResponse().error(formula));
        }
    }

    @Override
    public List<AnnexeDetailsDto> findAnnexDetails(ReportType reportType, Long fiscalYearId) {
        List<ReportLineDto> reportLinesWithAnnex = generateAnnualReport(reportType, fiscalYearId, StringUtils.EMPTY);
        reportLinesWithAnnex = reportLinesWithAnnex.stream().filter(rld -> rld.getAnnexCode() != null)
                .collect(Collectors.toList());
        List<AnnexeDetailsDto> annexeDetailsList = new ArrayList<>();
        for (ReportLineDto reportLine : reportLinesWithAnnex) {
            List<AccountBalanceDto> currentTrialBalanceList = new ArrayList<>();

            if (CalculationUtil.isFormulaComplex(reportLine.getFormula())) {
                for (Integer indexNumber : CalculationUtil.complexStringFormulaToCollection(reportLine.getFormula())) {
                    reportLinesWithAnnex.stream()
                            .filter((ReportLineDto tempReportLine) -> tempReportLine.getLineIndex()
                                    .equals(String.format(LINE_INDEX_FORMAT, Math.abs(indexNumber))))
                            .findAny().ifPresent((ReportLineDto reportLineWithAnnex) -> {
                                currentTrialBalanceList.addAll(initAccountBalance(reportLine));
                                annexeDetailsList.add(new AnnexeDetailsDto(currentTrialBalanceList,
                                        reportLineWithAnnex.getLabel(), reportLineWithAnnex.getAnnexCode(),
                                        reportLineWithAnnex.getAmount().multiply(
                                                new BigDecimal(getSignMultiplicationValue(reportLineWithAnnex)))));
                            });
                }
            } else {
                currentTrialBalanceList.addAll(initAccountBalance(reportLine));
                annexeDetailsList.add(new AnnexeDetailsDto(currentTrialBalanceList, reportLine.getLabel(),
                        reportLine.getAnnexCode(),
                        reportLine.getAmount().multiply(new BigDecimal(getSignMultiplicationValue(reportLine)))));
            }
        }
        return annexeDetailsList;
    }

    int getSignMultiplicationValue(ReportLineDto reportLine) {
        if (reportLine.isNegative()) {
            return -1;
        }
        return 1;
    }

    private List<AccountBalanceDto> initAccountBalance(ReportLineDto reportLine) {
        List<AccountBalanceDto> accountBalanceList = new ArrayList<>();
        Collection<String> formula = CalculationUtil.divideSimpleStringFormulaToStringElements(reportLine.getFormula());
        for (String formulaElement : formula) {
            int planCode;
            boolean isDebitOnly = false;
            boolean isCreditOnly = false;
            Character elementSuffixCharacter = Character.MIN_VALUE;
            if (Character.isDigit((formulaElement.charAt(formulaElement.length() - 1)))) {
                planCode = Integer.parseInt(formulaElement);
            } else {
                planCode = Integer.parseInt(formulaElement.substring(0, formulaElement.length() - 1));
                elementSuffixCharacter = formulaElement.charAt(formulaElement.length() - 1);
                isDebitOnly = elementSuffixCharacter.equals(ReportFormulaSuffix.DEBTOR.getValue());
                isCreditOnly = elementSuffixCharacter.equals(ReportFormulaSuffix.CREDITOR.getValue());
            }
            List<AccountBalanceDto> accountBalanceForFormulaElement;
            if (elementSuffixCharacter.equals(ReportFormulaSuffix.RESULT.getValue())) {
                AccountingConfigurationDto currentConfiguration = accountingConfigurationService.findLastConfig();
                if (currentConfiguration.getResultAccount() != null) {
                    int resultAccountCode = chartAccountsService.findById(currentConfiguration.getResultAccount())
                            .getCode();
                    accountBalanceForFormulaElement = documentAccountLineDao.getAccountAnnexe(
                            Integer.toString(Math.abs(resultAccountCode)), reportLine.getFiscalYear().getStartDate(),
                            reportLine.getFiscalYear().getEndDate());
                    accountBalanceList.addAll(accountBalanceForFormulaElement);
                } else {
                    log.error(AccountingConstants.RESULT_ACCOUNT_NOT_NOT_FOUND);
                    throw new HttpCustomException(
                            ApiErrors.Accounting.ACCOUNTING_CONFIGURATION_RESULT_ACCOUNT_NOT_NOT_FOUND);
                }
            } else {
                accountBalanceForFormulaElement = documentAccountLineDao.getAccountAnnexe(
                        Integer.toString(Math.abs(planCode)), reportLine.getFiscalYear().getStartDate(),
                        reportLine.getFiscalYear().getEndDate());
                for (AccountBalanceDto accountBalanceDto : accountBalanceForFormulaElement) {
                    boolean shouldAccountBalanceBeAdded = !ReportFormulaSuffix.contains(elementSuffixCharacter);
                    shouldAccountBalanceBeAdded = shouldAccountBalanceBeAdded || (isDebitOnly && accountBalanceDto
                            .getTotalCurrentDebit().compareTo(accountBalanceDto.getTotalCurrentCredit()) >= 0);
                    shouldAccountBalanceBeAdded = shouldAccountBalanceBeAdded || (isCreditOnly && accountBalanceDto
                            .getTotalCurrentDebit().compareTo(accountBalanceDto.getTotalCurrentCredit()) <= 0);
                    if (shouldAccountBalanceBeAdded) {
                        accountBalanceList.add(accountBalanceDto);
                    }
                }
            }
        }
        return accountBalanceList;
    }

    @Override
    public List<AnnexeReportDto> generateAnnualReportAnnex(ReportType reportType, Long fiscalYearId) {
        log.info(AccountingConstants.GENERATE_ANNUAL_REPORT_FISCAL_YEAR, reportType, fiscalYearId);
        List<AnnexeReportDto> annexeReportList = new ArrayList<>();
        List<AnnexeDetailsDto> annexeReportDetailsList = findAnnexDetails(reportType, fiscalYearId);
        List<IInsertIntoReportLineService> iInsertIntoReportLineServices = new ArrayList<>();

        iInsertIntoReportLineServices.add(insertAnnexeLineIntoReport);
        iInsertIntoReportLineServices.add(insertAccountLineIntoReport);
        iInsertIntoReportLineServices.add(insertTotalLineIntoReport);

        if (!annexeReportDetailsList.isEmpty()) {
            for (AnnexeDetailsDto annexeDetailsDto : annexeReportDetailsList) {
                iInsertIntoReportLineServices.forEach((IInsertIntoReportLineService iirl) -> iirl
                        .insertIntoReport(annexeDetailsDto, annexeReportList));
            }
        }
        return annexeReportList;
    }

    @Override
    public List<String> setConfigurationCookie(String user, String contentType, String authorization) {
        List<String> configurationList = new ArrayList<>();
        configurationList.add(contentType);
        configurationList.add(user);
        configurationList.add(authorization);
        return configurationList;
    }

    @Override
    public List<AmortizationTableReportDto> generateAmortizationReport(Long fiscalYearId,
            String contentType, String user, String authorization) {
        List<DepreciationAssetsDto> depreciationAssetsDtos = depreciationAssetService.getAllDepreciations(user,
                contentType, authorization);
        return depreciationAssetService.getDistinctAmortizationAccount(depreciationAssetsDtos, fiscalYearId);
    }

    @Override
    public ReportLineDto resetReportLine(Long reportLineId, String user) {
        if (!existsById(reportLineId)) {
            log.error(AccountingConstants.REPORT_LINE_NO_EXIST);
            throw new HttpCustomException(ApiErrors.Accounting.REPORT_LINE_INEXISTANT_REPORT_LINE,
                    new ErrorsResponse().error(reportLineId));
        }
        ReportLine reportLine = findOne(reportLineId);
        int currentFiscalYearClosingState = fiscalYearService.findById(reportLine.getFiscalYear().getId())
                .getClosingState();
        if (currentFiscalYearClosingState == FiscalYearClosingState.OPEN.getValue()
                || currentFiscalYearClosingState == FiscalYearClosingState.PARTIALLY_CLOSED.getValue()) {
            StandardReportLine standardReportLine = standardReportLineService
                    .findByReportTypeAndLineIndexAndIsDeletedFalse(reportLine.getReportType(),
                            reportLine.getLineIndex());
            if (standardReportLine != null) {
                reportLine.setFormula(standardReportLine.getFormula());
                reportLine.setNegative(standardReportLine.isNegative());
                reportLine.setManuallyChanged(false);
                reportLine.setAnnexCode(standardReportLine.getAnnexCode());
                return ReportLineConverter.modelToDto(saveAndFlush(reportLine));
            } else {
                log.error(AccountingConstants.NO_STANDARD_CONFIG_WITH_INDEX_OF_REPORT, reportLine.getLineIndex());
                throw new HttpCustomException(
                        ApiErrors.Accounting.REPORT_LINE_NO_DEFAULT_REPORT_CONFIGURATION_WITH_INDEX_FOR_THIS_REPORT_TYPE,
                        new ErrorsResponse().error(reportLine.getLineIndex()));
            }
        } else {
            throw new HttpCustomException(ApiErrors.Accounting.REPORT_LINE_CANNOT_UPDATE_WHEN_FISCAL_YEAR_NOT_OPENED);
        }
    }

    @Override
    public boolean existsById(Long reportLineId) {
        return reportLineDao.existsByIdAndIsDeletedFalse(reportLineId);
    }

    private List<ReportLine> saveReportLines(List<ReportLine> reportLines) {
        return save(reportLines);
    }
}
