package fr.sparkit.accounting.services.impl;

import static fr.sparkit.accounting.constants.AccountingConstants.AMORTIZATION_TABLE;
import static fr.sparkit.accounting.constants.AccountingConstants.AUXILIARY_JOURNAL;
import static fr.sparkit.accounting.constants.AccountingConstants.BALANCE_GENERAL;
import static fr.sparkit.accounting.constants.AccountingConstants.BALANCE_SHEET;
import static fr.sparkit.accounting.constants.AccountingConstants.BALANCE_SHEET_EQUITY_AND_LIABILITIES;
import static fr.sparkit.accounting.constants.AccountingConstants.BILAN_ANNEXE;
import static fr.sparkit.accounting.constants.AccountingConstants.CAPITTAL_COLLECTION;
import static fr.sparkit.accounting.constants.AccountingConstants.CASH_FLOW_AUTHORIZED;
import static fr.sparkit.accounting.constants.AccountingConstants.CASH_FLOW_AUTHORIZED_ANNEX;
import static fr.sparkit.accounting.constants.AccountingConstants.CASH_FLOW_OF_REFERENCE;
import static fr.sparkit.accounting.constants.AccountingConstants.CASH_FLOW_OF_REFERENCE_ANNEX;
import static fr.sparkit.accounting.constants.AccountingConstants.COLLECTION_OF_DATA;
import static fr.sparkit.accounting.constants.AccountingConstants.FISCAL_YEARNAME_PARAM;
import static fr.sparkit.accounting.constants.AccountingConstants.GENERAL_LEGEDER;
import static fr.sparkit.accounting.constants.AccountingConstants.INTERMEDIARY_BALANCE_COMMERCIAL;
import static fr.sparkit.accounting.constants.AccountingConstants.INTERMEDIARY_BALANCE_NAME;
import static fr.sparkit.accounting.constants.AccountingConstants.JRXML_EXTENSION;
import static fr.sparkit.accounting.constants.AccountingConstants.MAP_REPORT_LINES_ASSETS;
import static fr.sparkit.accounting.constants.AccountingConstants.MAP_REPORT_LINES_STATE;
import static fr.sparkit.accounting.constants.AccountingConstants.PDF;
import static fr.sparkit.accounting.constants.AccountingConstants.PREVIOUS_FISCAL_YEAR_PARAM;
import static fr.sparkit.accounting.constants.AccountingConstants.RECONCILIATION_BANK;
import static fr.sparkit.accounting.constants.AccountingConstants.RECONCILIATION_BANK_STATEMENT;
import static fr.sparkit.accounting.constants.AccountingConstants.STATE_JOURNAL;
import static fr.sparkit.accounting.constants.AccountingConstants.STATE_OF_INCOME;
import static fr.sparkit.accounting.constants.AccountingConstants.STATE_OF_INCOME_ANNEX;
import static fr.sparkit.accounting.constants.AccountingConstants.TOTAL_AMOUNTFISCAL_YEAR;
import static fr.sparkit.accounting.constants.AccountingConstants.TOTAL_AMOUNT_PREVIOUS_FISCAL_YEAR;
import static fr.sparkit.accounting.constants.NumberConstant.ZERO;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.constants.NumberConstant;
import fr.sparkit.accounting.convertor.CloseDocumentAccountLineConvertor;
import fr.sparkit.accounting.convertor.ReportLineConverter;
import fr.sparkit.accounting.dto.AccountDto;
import fr.sparkit.accounting.dto.AmortizationTableReportDto;
import fr.sparkit.accounting.dto.AnnexeReportDto;
import fr.sparkit.accounting.dto.AuxiliaryJournalLineDto;
import fr.sparkit.accounting.dto.CentralizingJournalByMonthReportLineDto;
import fr.sparkit.accounting.dto.CloseDocumentAccountLineDto;
import fr.sparkit.accounting.dto.FileUploadDto;
import fr.sparkit.accounting.dto.FiscalYearDto;
import fr.sparkit.accounting.dto.GeneralLedgerReportLineDto;
import fr.sparkit.accounting.dto.JournalStateReportLineDto;
import fr.sparkit.accounting.dto.PrintableCloseDocumentAccountLineDto;
import fr.sparkit.accounting.dto.PrintableReportLineDto;
import fr.sparkit.accounting.dto.ReportLineDto;
import fr.sparkit.accounting.dto.ReportTemplateDefaultParameters;
import fr.sparkit.accounting.dto.TrialBalanceReportLineDto;
import fr.sparkit.accounting.enumuration.ReportType;
import fr.sparkit.accounting.services.IAccountService;
import fr.sparkit.accounting.services.IBankReconciliationStatementService;
import fr.sparkit.accounting.services.IDocumentAccountLineService;
import fr.sparkit.accounting.services.IFiscalYearService;
import fr.sparkit.accounting.services.IGeneralLedgerService;
import fr.sparkit.accounting.services.IJournalService;
import fr.sparkit.accounting.services.IJournalStateService;
import fr.sparkit.accounting.services.IReportJasperService;
import fr.sparkit.accounting.services.IReportLineService;
import fr.sparkit.accounting.services.IStateOfAuxiliaryJournalService;
import fr.sparkit.accounting.services.ITrialBalanceService;
import fr.sparkit.accounting.services.utils.ReportLineUtil;
import fr.sparkit.accounting.util.CalculationUtil;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.errors.ErrorsResponse;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Service
@Slf4j
public class ReportJasperService implements IReportJasperService {

    private final IGeneralLedgerService generalLedgerService;
    private final IJournalService journalService;
    private final IStateOfAuxiliaryJournalService stateAuxilaryJournalService;
    private final ITrialBalanceService trialBalanceService;
    private final IBankReconciliationStatementService bankReconciliationStatementService;
    private final IAccountService accountService;
    private final IDocumentAccountLineService documentAccountLineService;
    private final IReportLineService reportLineService;
    private final IFiscalYearService fiscalYearService;
    private final IJournalStateService journalStateService;

    @Autowired
    public ReportJasperService(IAccountService accountService, IGeneralLedgerService generalLedgerService,
            IJournalService journalService, IStateOfAuxiliaryJournalService stateAuxilaryJournalService,
            ITrialBalanceService trialBalanceService,
            IBankReconciliationStatementService bankReconciliationStatementService,
            IDocumentAccountLineService documentAccountLineService, IReportLineService reportLineService,
            IFiscalYearService fiscalYearService, IJournalStateService journalStateService) {
        this.generalLedgerService = generalLedgerService;
        this.journalService = journalService;
        this.stateAuxilaryJournalService = stateAuxilaryJournalService;
        this.trialBalanceService = trialBalanceService;
        this.bankReconciliationStatementService = bankReconciliationStatementService;
        this.accountService = accountService;
        this.documentAccountLineService = documentAccountLineService;
        this.reportLineService = reportLineService;
        this.fiscalYearService = fiscalYearService;
        this.journalStateService = journalStateService;
    }

    @Override
    public FileUploadDto generateGeneralLedgerReport(LocalDateTime startDate, LocalDateTime endDate,
            String beginAccountCode, String endAccountCode, String beginAmount, String endAmount, String accountType,
            ReportTemplateDefaultParameters reportTemplateParams, String letteringOperationType, String field,
            String directio) {

        HashMap<String, Object> parameters = new HashMap<>();

        ReportLineUtil.fillTheMapParametersWithGeneralInfos(startDate, endDate, reportTemplateParams, parameters);

        ReportLineUtil.fillTheMapParametersWithGeneralLedgerInfos(parameters);

        List<GeneralLedgerReportLineDto> generalLedgerReportLineDtos = generalLedgerService
                .generateGeneralLedgerTelerikReport(startDate, endDate, beginAccountCode, endAccountCode, beginAmount,
                        endAmount, accountType, letteringOperationType, field, directio);

        generalLedgerReportLineDtos.add(ZERO, new GeneralLedgerReportLineDto());

        return compileAndGenerateFile(generalLedgerReportLineDtos, COLLECTION_OF_DATA, GENERAL_LEGEDER, null,
                parameters);

    }

    @Override
    public FileUploadDto generateStateOfIncome(Long fiscalYearId, List<ReportLineDto> reportLines,
            ReportTemplateDefaultParameters reportTemplateParams) {

        FiscalYearDto fiscalYearDto = fiscalYearService.findById(fiscalYearId);

        Map<String, PrintableReportLineDto> mapReportLinesState = ReportLineConverter.annualReportToMap(reportLines);

        Map<String, Object> parameters = getAnnualReportParameters(fiscalYearDto, reportLines);

        parameters.put(MAP_REPORT_LINES_STATE, mapReportLinesState);

        List<PrintableReportLineDto> printableReportLineDtos = new ArrayList<>();

        printableReportLineDtos.add(new PrintableReportLineDto());

        ReportLineUtil.fillTheMapParametersWithGeneralInfos(fiscalYearDto.getStartDate(), fiscalYearDto.getEndDate(),
                reportTemplateParams, parameters);

        ReportLineUtil.fillTheMapParametersWithStateOfIncome(parameters);

        return compileAndGenerateFile(printableReportLineDtos, COLLECTION_OF_DATA, STATE_OF_INCOME, null, parameters);
    }

    @Override
    public FileUploadDto generateDetailedAnnualReport(Long fiscalYearId, List<ReportLineDto> reportLines,
            String reportType, ReportTemplateDefaultParameters reportTemplateParams) {
        FileUploadDto fileUploadDto;
        if (reportType.equalsIgnoreCase(AccountingConstants.INTERMEDIARY_BALANCE)) {
            reportType = INTERMEDIARY_BALANCE_COMMERCIAL;
        }
        try {
            ReportType reportTypeEnum = ReportType.valueOf(reportType.toUpperCase(AccountingConstants.LANGUAGE));
            switch (reportTypeEnum) {
            case SOI:
                fileUploadDto = generateStateOfIncome(fiscalYearId, reportLines, reportTemplateParams);
                break;
            case BSAS:
                fileUploadDto = generateAssetBalanceSheet(fiscalYearId, reportLines, reportTemplateParams);
                break;
            case CIB:
                fileUploadDto = generateIntermediaryBalance(fiscalYearId, reportLines, reportTemplateParams);
                break;
            case CF:
                log.info("Modèle de référence Flux de trésorerie");
                fileUploadDto = generateCashFlowOfReference(fiscalYearId, reportLines, reportTemplateParams);
                break;
            case CFA:
                log.info("Modèle autorisé Flux de trésorerie");
                fileUploadDto = generateCashFlowAuthorized(fiscalYearId, reportLines, reportTemplateParams);
                break;
            default:
                log.error(AccountingConstants.NO_ANNEX_REPORT_SUPPORTED_FOR_REPORT_TYPE, reportTypeEnum);
                throw new HttpCustomException(ApiErrors.Accounting.NO_ANNEX_REPORT_SUPPORTED_FOR_THIS_REPORT_TYPE);
            }
        } catch (IllegalArgumentException e) {
            log.error(AccountingConstants.LOG_REPORT_TYPE_INVALID, reportType);
            throw new HttpCustomException(ApiErrors.Accounting.REPORT_TYPE_INVALID,
                    new ErrorsResponse().error(reportType));
        }
        return fileUploadDto;
    }

    @Override
    public FileUploadDto generateCentralizingJournalReport(LocalDateTime startDate, LocalDateTime endDate,
            int breakingAccount, int breakingCustomerAccount, int breakingSupplierAccount, List<Long> journalIds,
            ReportTemplateDefaultParameters reportTemplateParams) {

        HashMap<String, Object> parameters = new HashMap<>();

        ReportLineUtil.fillTheMapParametersWithGeneralInfos(startDate, endDate, reportTemplateParams, parameters);

        ReportLineUtil.fillTheMapParametersWithCentralizingJournalInfos(parameters);

        List<CentralizingJournalByMonthReportLineDto> centralizingJournalReportLineDtos = journalService
                .generateCentralizingJournalByMonthReportLines(journalIds, startDate, endDate, breakingAccount,
                        breakingCustomerAccount, breakingSupplierAccount);

        centralizingJournalReportLineDtos.add(ZERO, new CentralizingJournalByMonthReportLineDto());

        return compileAndGenerateFile(centralizingJournalReportLineDtos,
                AccountingConstants.LIST_OF_CENTRALIZATION_JOURNAL_REPORT,
                AccountingConstants.PDF_NAME_CENTRALIZING_JOURNAL, null, parameters);

    }

    public FileUploadDto compileAndGenerateFile(Collection<?> collectionData, String collectionName, String pdfName,
            Map<String, Object> tempalteParameters, Map<String, Object> parameters) {
        JRBeanCollectionDataSource beanColDataSource = ReportLineUtil.getJRBeanCollection(collectionData);
        parameters.put(collectionName, beanColDataSource);
        if (tempalteParameters != null) {
            parameters.putAll(tempalteParameters);
        }
        URL url = getClass().getResource("/jasper-reports/" + pdfName + JRXML_EXTENSION);
        try {
            JasperPrint jasperPrint = ReportLineUtil.compileJRXMLFile(parameters, beanColDataSource, url.getPath());
            byte[] pdfContents = JasperExportManager.exportReportToPdf(jasperPrint);
            return new FileUploadDto(Base64.getEncoder().encodeToString(pdfContents), pdfName + PDF);
        } catch (JRException e) {
            log.error(e.toString());
            throw new HttpCustomException(ApiErrors.Accounting.ERROR_JASPER_FILE_GENERATION,
                    new ErrorsResponse().error(e));
        }
    }

    @Override
    public FileUploadDto generateTrialBalanceJasperReport(LocalDateTime startDate, LocalDateTime endDate,
            String beginAccountCode, String endAccountCode, ReportTemplateDefaultParameters reportTemplateParams) {

        HashMap<String, Object> parameters = new HashMap<>();

        ReportLineUtil.fillTheMapParametersWithGeneralInfos(startDate, endDate, reportTemplateParams, parameters);

        ReportLineUtil.fillTheMapParametersWithTrialBalanceInfos(parameters);

        List<TrialBalanceReportLineDto> trialBalanceReportLineDtos = trialBalanceService
                .generateTrialBalanceTelerikReport(startDate, endDate, beginAccountCode, endAccountCode);

        trialBalanceReportLineDtos.add(ZERO, new TrialBalanceReportLineDto());

        return compileAndGenerateFile(trialBalanceReportLineDtos, COLLECTION_OF_DATA, BALANCE_GENERAL, null,
                parameters);

    }

    @Override
    public FileUploadDto generateAuxiliaryJournalJasperReport(LocalDateTime startDate, LocalDateTime endDate,
            List<Long> journalIds, ReportTemplateDefaultParameters reportTemplateParams) {

        HashMap<String, Object> parameters = new HashMap<>();

        ReportLineUtil.fillTheMapParametersWithGeneralInfos(startDate, endDate, reportTemplateParams, parameters);

        ReportLineUtil.fillTheMapParametersWithAuxiliaryJournalInfos(parameters);

        List<AuxiliaryJournalLineDto> auxiliaryJournalReportLineDto = stateAuxilaryJournalService
                .generateAuxiliaryJournalsTelerikReport(startDate, endDate, journalIds);

        auxiliaryJournalReportLineDto.add(ZERO, new AuxiliaryJournalLineDto());

        return compileAndGenerateFile(auxiliaryJournalReportLineDto, COLLECTION_OF_DATA, AUXILIARY_JOURNAL, null,
                parameters);
    }

    @Override
    public FileUploadDto generateBankReconciliationJasperReport(Long fiscalYearId, Long accountId, int closeMonth,
            String period, String initialAmountCredit, String finalAmountCredit, String initialAmountDebit,
            String finalAmountDebit, ReportTemplateDefaultParameters reportTemplateParams) {

        List<CloseDocumentAccountLineDto> closeDocumentAccountLineDto = bankReconciliationStatementService
                .generateBankReconciliationReport(fiscalYearId, accountId, closeMonth);

        List<PrintableCloseDocumentAccountLineDto> bankReconcialiationReportLineDtos = CloseDocumentAccountLineConvertor
                .closeDocumentAccountLinesDtoToPrintables(closeDocumentAccountLineDto);

        bankReconcialiationReportLineDtos.add(ZERO, new PrintableCloseDocumentAccountLineDto());

        AccountDto accountdto = accountService.findById(accountId);

        HashMap<String, Object> parameters = new HashMap<>();

        FiscalYearDto fiscalYearDto = fiscalYearService.findById(fiscalYearId);

        ReportLineUtil.fillTheMapParametersWithGeneralInfos(fiscalYearDto.getStartDate(), fiscalYearDto.getEndDate(),
                reportTemplateParams, parameters);

        ReportLineUtil.fillTheMapParametersWithReconciliationBankInfos(parameters, accountdto, period,
                initialAmountCredit, finalAmountCredit, initialAmountDebit, finalAmountDebit);

        return compileAndGenerateFile(bankReconcialiationReportLineDtos, COLLECTION_OF_DATA, RECONCILIATION_BANK, null,
                parameters);
    }

    @Override
    public FileUploadDto generateBankReconciliationStatementJasperReport(Long accountId, LocalDateTime startDate,
            LocalDateTime endDate, String period, ReportTemplateDefaultParameters reportTemplateParams) {

        List<CloseDocumentAccountLineDto> closeDocumentAccountLineDto = documentAccountLineService
                .getReconcilableDocumentAccountLineInBetween(accountId, startDate, endDate);

        List<PrintableCloseDocumentAccountLineDto> bankReconcialiationReportLineDtos = CloseDocumentAccountLineConvertor
                .closeDocumentAccountLinesDtoToPrintables(closeDocumentAccountLineDto);

        bankReconcialiationReportLineDtos.add(ZERO, new PrintableCloseDocumentAccountLineDto());

        AccountDto accountdto = accountService.findById(accountId);

        HashMap<String, Object> parameters = new HashMap<>();

        ReportLineUtil.fillTheMapParametersWithGeneralInfos(startDate, endDate, reportTemplateParams, parameters);

        ReportLineUtil.fillTheMapParametersWithReconciliationBankStatementInfos(parameters, accountdto, period);

        return compileAndGenerateFile(bankReconcialiationReportLineDtos, COLLECTION_OF_DATA,
                RECONCILIATION_BANK_STATEMENT, null, parameters);
    }

    @Override
    public FileUploadDto generateAnnualReportAnnexJasperReport(String reportType, Long fiscalYearId,
            ReportTemplateDefaultParameters reportTemplateParams) {
        List<AnnexeReportDto> annexeReportDtos;
        FileUploadDto fileUploadDto;
        try {
            ReportType reportTypeEnum = ReportType.valueOf(reportType.toUpperCase(AccountingConstants.LANGUAGE));
            if (reportTypeEnum.equals(ReportType.BSAN)) {
                reportTypeEnum = ReportType.BS;
            }
            annexeReportDtos = reportLineService.generateAnnualReportAnnex(reportTypeEnum, fiscalYearId);
            annexeReportDtos.add(NumberConstant.ZERO, null);
            switch (reportTypeEnum) {
            case SOI:
                fileUploadDto = generateStateOfIncomeAnnex(fiscalYearId, annexeReportDtos, reportTemplateParams);
                break;
            case BS:
                fileUploadDto = generateBalanceSheetAnnex(fiscalYearId, annexeReportDtos, reportTemplateParams);
                break;
            case CF:
                log.info("Modèle de référence Flux de trésorerie annexe");
                fileUploadDto = generateCashFlowOfReferenceAnnex(fiscalYearId, annexeReportDtos, reportTemplateParams);
                break;
            case CFA:
                log.info("Modèle autorisé Flux de trésorerie annexe");
                fileUploadDto = generateCashFlowAuthorizedAnnex(fiscalYearId, annexeReportDtos, reportTemplateParams);
                break;

            default:
                log.error(AccountingConstants.NO_ANNEX_REPORT_SUPPORTED_FOR_REPORT_TYPE, reportTypeEnum);
                throw new HttpCustomException(ApiErrors.Accounting.NO_ANNEX_REPORT_SUPPORTED_FOR_THIS_REPORT_TYPE);
            }
        } catch (IllegalArgumentException e) {
            log.error(AccountingConstants.LOG_REPORT_TYPE_INVALID, reportType);
            throw new HttpCustomException(ApiErrors.Accounting.REPORT_TYPE_INVALID,
                    new ErrorsResponse().error(reportType));
        }
        return fileUploadDto;
    }

    private FileUploadDto generateCashFlowOfReferenceAnnex(Long fiscalYearId, List<AnnexeReportDto> annexeReportDtos,
            ReportTemplateDefaultParameters reportTemplateParams) {

        HashMap<String, Object> parameters = new HashMap<>();

        FiscalYearDto fiscalYearDto = fiscalYearService.findById(fiscalYearId);

        ReportLineUtil.fillTheMapParametersWithGeneralInfos(fiscalYearDto.getStartDate(), fiscalYearDto.getEndDate(),
                reportTemplateParams, parameters);

        ReportLineUtil.fillTheMapParametersWithCashFlowOfReferenceAnnex(parameters);

        return compileAndGenerateFile(annexeReportDtos, COLLECTION_OF_DATA, CASH_FLOW_OF_REFERENCE_ANNEX, null,
                parameters);
    }

    @Override
    public FileUploadDto generateCashFlowAuthorizedAnnex(Long fiscalYearId, List<AnnexeReportDto> annexeReportDtos,
            ReportTemplateDefaultParameters reportTemplateParams) {

        HashMap<String, Object> parameters = new HashMap<>();

        FiscalYearDto fiscalYearDto = fiscalYearService.findById(fiscalYearId);

        ReportLineUtil.fillTheMapParametersWithGeneralInfos(fiscalYearDto.getStartDate(), fiscalYearDto.getEndDate(),
                reportTemplateParams, parameters);

        ReportLineUtil.fillTheMapParametersWithCashFlowAuthorizedAnnex(parameters);

        return compileAndGenerateFile(annexeReportDtos, COLLECTION_OF_DATA, CASH_FLOW_AUTHORIZED_ANNEX, null,
                parameters);
    }

    @Override
    public FileUploadDto generateAssetBalanceSheet(Long fiscalYearId, List<ReportLineDto> reportLines,
            ReportTemplateDefaultParameters reportTemplateParams) {

        FiscalYearDto fiscalYearDto = fiscalYearService.findById(fiscalYearId);

        Map<String, PrintableReportLineDto> mapReportLinesAssets = ReportLineConverter.annualReportToMap(reportLines);

        HashMap<String, Object> parameters = new HashMap<>();

        parameters.putAll(getParamCurrentAndPreviousFiscalyear(reportLines, fiscalYearDto));

        parameters.put(MAP_REPORT_LINES_ASSETS, mapReportLinesAssets);

        List<PrintableReportLineDto> printableReportLineDtos = new ArrayList<>();

        printableReportLineDtos.add(new PrintableReportLineDto());

        ReportLineUtil.fillTheMapParametersWithGeneralInfos(fiscalYearDto.getStartDate(), fiscalYearDto.getEndDate(),
                reportTemplateParams, parameters);

        ReportLineUtil.fillTheMapParametersWithBalanceSheetParts(parameters, BALANCE_SHEET);

        return compileAndGenerateFile(printableReportLineDtos, COLLECTION_OF_DATA, BALANCE_SHEET, null, parameters);
    }

    @Override
    public FileUploadDto generateBalanceSheetEquityAndLiabilities(Long fiscalYearId, List<ReportLineDto> reportLines,
            ReportTemplateDefaultParameters reportTemplateParams) {

        FiscalYearDto fiscalYearDto = fiscalYearService.findById(fiscalYearId);

        Map<String, PrintableReportLineDto> mapReportLinesAssets = ReportLineConverter.annualReportToMap(reportLines);

        HashMap<String, Object> parameters = new HashMap<>();

        parameters.putAll(getParamCurrentAndPreviousFiscalyear(reportLines, fiscalYearDto));

        parameters.put(MAP_REPORT_LINES_ASSETS, mapReportLinesAssets);

        List<PrintableReportLineDto> printableReportLineDtos = new ArrayList<>();

        printableReportLineDtos.add(new PrintableReportLineDto());

        ReportLineUtil.fillTheMapParametersWithGeneralInfos(fiscalYearDto.getStartDate(), fiscalYearDto.getEndDate(),
                reportTemplateParams, parameters);

        ReportLineUtil.fillTheMapParametersWithBalanceSheetParts(parameters, BALANCE_SHEET_EQUITY_AND_LIABILITIES);

        return compileAndGenerateFile(printableReportLineDtos, COLLECTION_OF_DATA, BALANCE_SHEET_EQUITY_AND_LIABILITIES,
                null, parameters);
    }

    @Override
    public FileUploadDto generateBalanceSheetAnnex(Long fiscalYearId, Collection<?> annexeReportDtos,
            ReportTemplateDefaultParameters reportTemplateParams) {

        HashMap<String, Object> parameters = new HashMap<>();

        FiscalYearDto fiscalYearDto = fiscalYearService.findById(fiscalYearId);

        ReportLineUtil.fillTheMapParametersWithGeneralInfos(fiscalYearDto.getStartDate(), fiscalYearDto.getEndDate(),
                reportTemplateParams, parameters);

        ReportLineUtil.fillTheMapParametersWithBilanAnnex(parameters);

        return compileAndGenerateFile(annexeReportDtos, COLLECTION_OF_DATA, BILAN_ANNEXE, null, parameters);
    }

    @Override
    public FileUploadDto generateStateOfIncomeAnnex(Long fiscalYearId, Collection<?> annexeReportDtos,
            ReportTemplateDefaultParameters reportTemplateParams) {

        HashMap<String, Object> parameters = new HashMap<>();

        FiscalYearDto fiscalYearDto = fiscalYearService.findById(fiscalYearId);

        ReportLineUtil.fillTheMapParametersWithGeneralInfos(fiscalYearDto.getStartDate(), fiscalYearDto.getEndDate(),
                reportTemplateParams, parameters);

        ReportLineUtil.fillTheMapParametersWithStateOfIncomeAnnex(parameters);

        return compileAndGenerateFile(annexeReportDtos, COLLECTION_OF_DATA, STATE_OF_INCOME_ANNEX, null, parameters);
    }

    @Override
    public FileUploadDto generateIntermediaryBalance(Long fiscalYearId, List<ReportLineDto> reportLines,
            ReportTemplateDefaultParameters reportTemplateParams) {

        FiscalYearDto fiscalYearDto = fiscalYearService.findById(fiscalYearId);

        Map<String, Object> parameters = getAnnualReportParameters(fiscalYearDto, reportLines);

        List<PrintableReportLineDto> printableReportLines = ReportLineConverter.dtosToPrintables(reportLines);
        List<PrintableReportLineDto> commercialActivity = printableReportLines.stream()
                .filter((PrintableReportLineDto line) -> ReportType.CIB.equals(line.getReportType()))
                .collect(Collectors.toList());
        List<PrintableReportLineDto> industrialActivity = printableReportLines.stream()
                .filter((PrintableReportLineDto line) -> ReportType.IIB.equals(line.getReportType()))
                .collect(Collectors.toList());
        parameters.put(CAPITTAL_COLLECTION, ReportLineUtil.getJRBeanCollection(industrialActivity));

        commercialActivity.add(ZERO, null);

        ReportLineUtil.fillTheMapParametersWithGeneralInfos(fiscalYearDto.getStartDate(), fiscalYearDto.getEndDate(),
                reportTemplateParams, parameters);

        ReportLineUtil.fillTheMapParametersWithIntermediaryBalance(parameters);

        return compileAndGenerateFile(commercialActivity, COLLECTION_OF_DATA, INTERMEDIARY_BALANCE_NAME, null,
                parameters);
    }

    @Override
    public FileUploadDto generateCashFlowOfReference(Long fiscalYearId, List<ReportLineDto> reportLines,
            ReportTemplateDefaultParameters reportTemplateParams) {

        FiscalYearDto fiscalYearDto = fiscalYearService.findById(fiscalYearId);

        Map<String, Object> parameters = getAnnualReportParameters(fiscalYearDto, reportLines);

        List<PrintableReportLineDto> printableReportLines = ReportLineConverter.dtosToPrintables(reportLines);

        printableReportLines.add(ZERO, null);

        Map<String, PrintableReportLineDto> mapReportLinesAssets = ReportLineConverter.annualReportToMap(reportLines);

        parameters.putAll(getParamCurrentAndPreviousFiscalyear(reportLines, fiscalYearDto));

        parameters.put(CAPITTAL_COLLECTION, mapReportLinesAssets);

        List<PrintableReportLineDto> printableReportLineDtos = new ArrayList<>();

        printableReportLineDtos.add(new PrintableReportLineDto());

        ReportLineUtil.fillTheMapParametersWithGeneralInfos(fiscalYearDto.getStartDate(), fiscalYearDto.getEndDate(),
                reportTemplateParams, parameters);

        ReportLineUtil.fillTheMapParametersWithCashFlowOfReference(parameters);

        return compileAndGenerateFile(printableReportLines, COLLECTION_OF_DATA, CASH_FLOW_OF_REFERENCE, null,
                parameters);

    }

    @Override
    public FileUploadDto generateCashFlowAuthorized(Long fiscalYearId, List<ReportLineDto> reportLines,
            ReportTemplateDefaultParameters reportTemplateParams) {

        FiscalYearDto fiscalYearDto = fiscalYearService.findById(fiscalYearId);

        Map<String, Object> parameters = getAnnualReportParameters(fiscalYearDto, reportLines);

        List<PrintableReportLineDto> printableReportLines = ReportLineConverter.dtosToPrintables(reportLines);

        printableReportLines.add(ZERO, null);

        Map<String, PrintableReportLineDto> mapReportLinesAssets = ReportLineConverter.annualReportToMap(reportLines);

        parameters.putAll(getParamCurrentAndPreviousFiscalyear(reportLines, fiscalYearDto));

        parameters.put(MAP_REPORT_LINES_ASSETS, mapReportLinesAssets);

        List<PrintableReportLineDto> printableReportLineDtos = new ArrayList<>();

        printableReportLineDtos.add(new PrintableReportLineDto());

        ReportLineUtil.fillTheMapParametersWithGeneralInfos(fiscalYearDto.getStartDate(), fiscalYearDto.getEndDate(),
                reportTemplateParams, parameters);

        ReportLineUtil.fillTheMapParametersWithCashFlowAuthorized(parameters);

        return compileAndGenerateFile(printableReportLines, COLLECTION_OF_DATA, CASH_FLOW_AUTHORIZED, null, parameters);

    }

    @Override
    public FileUploadDto generateJournalStateReport(LocalDateTime startDate, LocalDateTime endDate,
            ReportTemplateDefaultParameters reportTemplateParams) {

        HashMap<String, Object> parameters = new HashMap<>();

        ReportLineUtil.fillTheMapParametersWithGeneralInfos(startDate, endDate, reportTemplateParams, parameters);

        ReportLineUtil.fillTheMapParametersWithStateOfJournal(parameters);

        List<JournalStateReportLineDto> journalStateReportLineDtos = journalStateService
                .findJournalsStateReport(startDate, endDate);

        journalStateReportLineDtos.add(ZERO, new JournalStateReportLineDto());

        return compileAndGenerateFile(journalStateReportLineDtos, COLLECTION_OF_DATA, STATE_JOURNAL, null, parameters);
    }

    @Override
    public FileUploadDto generateAmortizationReport(Long fiscalYearId, String user, String contentType,
            String authorization, ReportTemplateDefaultParameters reportTemplateParams) {

        FiscalYearDto fiscalYearDto = fiscalYearService.findById(fiscalYearId);

        List<AmortizationTableReportDto> amortizationTableReportDtos = reportLineService
                .generateAmortizationReport(fiscalYearId, user, contentType, authorization);

        amortizationTableReportDtos.add(ZERO, new AmortizationTableReportDto());

        HashMap<String, Object> parameters = new HashMap<>();

        ReportLineUtil.fillTheMapParametersWithGeneralInfos(fiscalYearDto.getStartDate(), fiscalYearDto.getEndDate(),
                reportTemplateParams, parameters);

        ReportLineUtil.fillTheMapParametersWithAmortizationTable(parameters);

        return compileAndGenerateFile(amortizationTableReportDtos, COLLECTION_OF_DATA, AMORTIZATION_TABLE, null,
                parameters);

    }

    private Map<String, Object> getAnnualReportParameters(FiscalYearDto fiscalYearDto,
            List<ReportLineDto> reportLines) {
        Map<String, Object> paramOfAnnualReport = getParamCurrentAndPreviousFiscalyear(reportLines, fiscalYearDto);
        paramOfAnnualReport.put(TOTAL_AMOUNTFISCAL_YEAR, CalculationUtil
                .getFormattedBigDecimalValueOrEmptyStringIfZero(reportLines.get(reportLines.size() - 1).getAmount()));
        paramOfAnnualReport.put(TOTAL_AMOUNT_PREVIOUS_FISCAL_YEAR,
                CalculationUtil.getFormattedBigDecimalValueOrEmptyStringIfZero(
                        reportLines.get(reportLines.size() - 1).getPreviousFiscalYearAmount()));
        return paramOfAnnualReport;
    }

    public Map<String, Object> getParamCurrentAndPreviousFiscalyear(List<ReportLineDto> reportLines,
            FiscalYearDto fiscalYearDto) {
        Map<String, Object> fiscalYearParams = new HashMap<>();
        fiscalYearParams.put(FISCAL_YEARNAME_PARAM, fiscalYearDto.getName());
        fiscalYearParams.put(PREVIOUS_FISCAL_YEAR_PARAM, reportLines.get(ZERO).getPreviousFiscalYear());
        return fiscalYearParams;
    }

}
