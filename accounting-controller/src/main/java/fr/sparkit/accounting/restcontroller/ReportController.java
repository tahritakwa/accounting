package fr.sparkit.accounting.restcontroller;

import static fr.sparkit.accounting.constants.AccountingConstants.INTERMEDIARY_BALANCE;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.sparkit.accounting.auditing.HasRoles;
import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.convertor.ReportLineConverter;
import fr.sparkit.accounting.dto.AmortizationTableDto;
import fr.sparkit.accounting.dto.AmortizationTableReportDto;
import fr.sparkit.accounting.dto.AnnexeReportDto;
import fr.sparkit.accounting.dto.AuxiliaryJournalDetailsDto;
import fr.sparkit.accounting.dto.AuxiliaryJournalLineDto;
import fr.sparkit.accounting.dto.BankReconciliationPageDto;
import fr.sparkit.accounting.dto.BankReconciliationStatementDto;
import fr.sparkit.accounting.dto.CentralizingJournalDetailsByMonthDto;
import fr.sparkit.accounting.dto.CentralizingJournalDetailsDto;
import fr.sparkit.accounting.dto.CentralizingJournalDto;
import fr.sparkit.accounting.dto.CentralizingJournalReportLineDto;
import fr.sparkit.accounting.dto.CloseDocumentAccountLineDto;
import fr.sparkit.accounting.dto.DepreciationAssetsDto;
import fr.sparkit.accounting.dto.GeneralLedgerDetailsPageDto;
import fr.sparkit.accounting.dto.GeneralLedgerPageDto;
import fr.sparkit.accounting.dto.GeneralLedgerReportLineDto;
import fr.sparkit.accounting.dto.JournalStateDetailsDto;
import fr.sparkit.accounting.dto.JournalStateDto;
import fr.sparkit.accounting.dto.PrintableReportLineDto;
import fr.sparkit.accounting.dto.ReportLineDto;
import fr.sparkit.accounting.dto.StandardReportLineDto;
import fr.sparkit.accounting.dto.StateOfAuxiliaryJournalPage;
import fr.sparkit.accounting.dto.TrialBalancePageDto;
import fr.sparkit.accounting.dto.TrialBalanceReportLineDto;
import fr.sparkit.accounting.entities.BankReconciliationStatement;
import fr.sparkit.accounting.entities.DepreciationAssets;
import fr.sparkit.accounting.enumuration.ReportType;
import fr.sparkit.accounting.services.IAmortizationtableService;
import fr.sparkit.accounting.services.IBankReconciliationStatementService;
import fr.sparkit.accounting.services.IDepreciationAssetConfigurationService;
import fr.sparkit.accounting.services.IDepreciationAssetService;
import fr.sparkit.accounting.services.IDocumentAccountLineService;
import fr.sparkit.accounting.services.IGeneralLedgerService;
import fr.sparkit.accounting.services.IJournalService;
import fr.sparkit.accounting.services.IJournalStateService;
import fr.sparkit.accounting.services.IReportLineService;
import fr.sparkit.accounting.services.IStandardReportLineService;
import fr.sparkit.accounting.services.IStateOfAuxiliaryJournalService;
import fr.sparkit.accounting.services.ITrialBalanceService;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.errors.ErrorsResponse;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/accounting/reports")

public class ReportController {

    private final IGeneralLedgerService generalLedgerService;
    private final ITrialBalanceService trialBalanceService;
    private final IReportLineService reportLineService;
    private final IJournalStateService journalStateService;
    private final IBankReconciliationStatementService bankReconciliationStatementService;
    private final IAmortizationtableService amortizationTableService;
    private final IDepreciationAssetService depreciationAssetService;
    private final IStandardReportLineService standardReportLineService;
    private final IDocumentAccountLineService documentAccountLineService;
    private final IJournalService journalService;
    private final IStateOfAuxiliaryJournalService stateOfAuxiliaryJournalService;
    private final IDepreciationAssetConfigurationService depreciationAssetConfigurationService;

    @Value("${tomcat.server.url}")
    private String tomcatServerUrl;
    @Value("${dotnet.url}")
    private String dotnetRessource;

    @Autowired
    public ReportController(IGeneralLedgerService generalLedgerService, ITrialBalanceService trialBalanceService,
            IReportLineService reportLineService, IJournalStateService journalStateService,
            IBankReconciliationStatementService bankReconciliationStatementService,
            IAmortizationtableService amortizationTableService, IDepreciationAssetService depreciationAssetService,
            IStandardReportLineService standardReportLineService,
            IDocumentAccountLineService documentAccountLineService, IJournalService journalService,
            IStateOfAuxiliaryJournalService stateOfAuxiliaryJournalService,
            IDepreciationAssetConfigurationService depreciationAssetConfigurationService) {
        super();
        this.generalLedgerService = generalLedgerService;
        this.trialBalanceService = trialBalanceService;
        this.reportLineService = reportLineService;
        this.journalStateService = journalStateService;
        this.bankReconciliationStatementService = bankReconciliationStatementService;
        this.amortizationTableService = amortizationTableService;
        this.depreciationAssetService = depreciationAssetService;
        this.standardReportLineService = standardReportLineService;
        this.documentAccountLineService = documentAccountLineService;
        this.journalService = journalService;
        this.stateOfAuxiliaryJournalService = stateOfAuxiliaryJournalService;
        this.depreciationAssetConfigurationService = depreciationAssetConfigurationService;
    }

    @GetMapping(value = "/general-ledger-accounts")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_EDITIONS_REPORTS" })
    public GeneralLedgerPageDto findGeneralLedgerAccounts(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            @RequestParam String beginAccountCode, @RequestParam String endAccountCode,
            @RequestParam String accountType, @RequestParam String beginAmount, @RequestParam String endAmount,
            @RequestParam String letteringOperationType) {
        return generalLedgerService.findGeneralLedgerAccounts(page, size, startDate, endDate, beginAccountCode,
                endAccountCode, beginAmount, endAmount, accountType, letteringOperationType);
    }

    @GetMapping(value = "/general-ledger-account-details/{id}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_EDITIONS_REPORTS" })
    public GeneralLedgerDetailsPageDto findGeneralLedgerAccountDetails(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            @RequestParam String beginAmount, @RequestParam String endAmount,
            @RequestParam String letteringOperationType, @RequestParam String field, @RequestParam String direction) {
        return generalLedgerService.findGeneralLedgerAccountDetails(id, page, size, startDate, endDate, beginAmount,
                endAmount, letteringOperationType, field, direction);
    }

    @GetMapping(value = "/general-ledger-report")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_EDITIONS_REPORTS" })
    public List<GeneralLedgerReportLineDto> findGeneralLedgerReport(
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            @RequestParam String beginAccountCode, @RequestParam String endAccountCode,
            @RequestParam String beginAmount, @RequestParam String endAmount, @RequestParam String accountType,
            @RequestParam String letteringOperationType, @RequestParam String field, @RequestParam String direction) {
        return generalLedgerService.generateGeneralLedgerTelerikReport(startDate, endDate, beginAccountCode,
                endAccountCode, beginAmount, endAmount, accountType, letteringOperationType, field, direction);
    }

    @PostMapping(value = "/auxiliary-journals")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_JOURNALS_REPORTS", "PRINT_JOURNALS_REPORTS" })
    public StateOfAuxiliaryJournalPage findAuxiliaryJournals(
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            @RequestBody List<Long> journalIds, Pageable pageable) {
        return stateOfAuxiliaryJournalService.findAuxiliaryJournals(startDate, endDate, journalIds, pageable);
    }

    @GetMapping(value = "/auxiliary-journal-details/{id}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_JOURNALS_REPORTS" })
    public Page<AuxiliaryJournalDetailsDto> findAuxiliaryJournalDetails(@PathVariable Long id,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            Pageable pageable) {
        return stateOfAuxiliaryJournalService.findAuxiliaryJournalDetails(id, startDate, endDate, pageable);
    }

    @GetMapping(value = "/auxiliary-journals-report")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_JOURNALS_REPORTS", "PRINT_JOURNALS_REPORTS" })
    public List<AuxiliaryJournalLineDto> findAuxiliaryJournalsReport(
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            @RequestParam List<Long> journalIds) {
        return stateOfAuxiliaryJournalService.generateAuxiliaryJournalsTelerikReport(startDate, endDate, journalIds);
    }

    @GetMapping(value = "/trial-balance")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_EDITIONS_REPORTS" })
    public TrialBalancePageDto findTrialBalanceAccounts(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            @RequestParam String beginAccountCode, @RequestParam String endAccountCode) {
        return trialBalanceService.findTrialBalanceAccounts(page, size, startDate, endDate, beginAccountCode,
                endAccountCode);
    }

    @GetMapping(value = "/trial-balance-report")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_EDITIONS_REPORTS" })
    public List<TrialBalanceReportLineDto> findTrialBalanceReport(
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            @RequestParam String beginAccountCode, @RequestParam String endAccountCode) {
        return trialBalanceService.generateTrialBalanceTelerikReport(startDate, endDate, beginAccountCode,
                endAccountCode);
    }

    @GetMapping(value = "/standard-report/{reportType}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_ACCOUNTING_STANDARD_REPORTS", "UPDATE_ACCOUNTING_STANDARD_REPORTS_FORMULA" })
    public List<StandardReportLineDto> getStandardReport(@PathVariable("reportType") String reportType) {
        try {
            return standardReportLineService.getReportLinesForReportType(
                    ReportType.valueOf(reportType.toUpperCase(AccountingConstants.LANGUAGE)));
        } catch (IllegalArgumentException e) {
            log.error(AccountingConstants.LOG_REPORT_TYPE_INVALID, reportType);
            throw new HttpCustomException(ApiErrors.Accounting.REPORT_TYPE_INVALID,
                    new ErrorsResponse().error(reportType));
        }
    }

    @PutMapping(value = "/standard-report/{id}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "UPDATE_ACCOUNTING_STANDARD_REPORTS_FORMULA" })
    public StandardReportLineDto updateStandardReport(@RequestBody StandardReportLineDto standardReportLine) {
        return standardReportLineService.updateStandardReport(standardReportLine);
    }

    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "UPDATE_FINANCIAL_STATES_REPORTS_FORMULA" })
    @PutMapping(value = "/reset-report-line/{reportLineId}")
    public ReportLineDto resetReportLine(@PathVariable() Long reportLineId, @RequestHeader("User") String user) {
        return reportLineService.resetReportLine(reportLineId, user);
    }

    @GetMapping(value = "/report-line")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_FINANCIAL_STATES_REPORTS" })
    public List<ReportLineDto> getReportLinesForReportType(@RequestParam String reportType) {
        try {
            return reportLineService.getReportLinesForReportType(
                    ReportType.valueOf(reportType.toUpperCase(AccountingConstants.LANGUAGE)));
        } catch (IllegalArgumentException e) {
            log.error(AccountingConstants.LOG_REPORT_TYPE_INVALID, reportType);
            throw new HttpCustomException(ApiErrors.Accounting.REPORT_TYPE_INVALID,
                    new ErrorsResponse().error(reportType));
        }
    }

    @GetMapping(value = "/generate-annual-report/{fiscalYearId}/{reportType}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_FINANCIAL_STATES_REPORTS", "UPDATE_FINANCIAL_STATES_REPORTS_FORMULA" })
    public List<ReportLineDto> generateAnnualReport(@PathVariable("fiscalYearId") Long fiscalYearId,
            @PathVariable("reportType") String reportType, @RequestHeader("User") String user) {
        try {
            if (reportType.equalsIgnoreCase(INTERMEDIARY_BALANCE)) {
                List<ReportLineDto> reportLinesForBothIB = reportLineService.generateAnnualReport(ReportType.CIB,
                        fiscalYearId, user);
                reportLinesForBothIB.addAll(reportLineService.generateAnnualReport(ReportType.IIB, fiscalYearId, user));
                return reportLinesForBothIB;
            } else {
                return reportLineService.generateAnnualReport(
                        ReportType.valueOf(reportType.toUpperCase(AccountingConstants.LANGUAGE)), fiscalYearId, user);
            }
        } catch (IllegalArgumentException e) {
            log.error(AccountingConstants.LOG_REPORT_TYPE_INVALID, reportType);
            throw new HttpCustomException(ApiErrors.Accounting.REPORT_TYPE_INVALID,
                    new ErrorsResponse().error(reportType));
        }
    }

    @GetMapping(value = "/annual-report/{fiscalYearId}/{reportType}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_FINANCIAL_STATES_REPORTS" })
    public List<ReportLineDto> getAnnualReport(@PathVariable("fiscalYearId") Long fiscalYearId,
            @PathVariable("reportType") String reportType) {
        try {
            if (reportType.equalsIgnoreCase(INTERMEDIARY_BALANCE)) {
                List<ReportLineDto> reportLinesForBothIB = reportLineService.getAnnualReport(ReportType.CIB,
                        fiscalYearId);
                reportLinesForBothIB.addAll(reportLineService.getAnnualReport(ReportType.IIB, fiscalYearId));
                return reportLinesForBothIB;
            } else {
                return reportLineService.getAnnualReport(
                        ReportType.valueOf(reportType.toUpperCase(AccountingConstants.LANGUAGE)), fiscalYearId);
            }
        } catch (IllegalArgumentException e) {
            log.error(AccountingConstants.LOG_REPORT_TYPE_INVALID, reportType);
            throw new HttpCustomException(ApiErrors.Accounting.REPORT_TYPE_INVALID,
                    new ErrorsResponse().error(reportType));
        }
    }

    @GetMapping(value = "/annual-report-detailed/{fiscalYearId}/{reportType}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_FINANCIAL_STATES_REPORTS" })
    public Map<String, PrintableReportLineDto> generateDetailedAnnualReport(
            @PathVariable("fiscalYearId") Long fiscalYearId, @PathVariable("reportType") String reportType) {
        return ReportLineConverter.annualReportToMap(getAnnualReport(fiscalYearId, reportType));
    }

    @GetMapping(value = "/annual-report-annex/{fiscalYearId}/{reportType}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_FINANCIAL_STATES_REPORTS" })
    public List<AnnexeReportDto> generateAnnualReportAnnex(@PathVariable("fiscalYearId") Long fiscalYearId,
            @PathVariable("reportType") String reportType) {
        try {
            return reportLineService.generateAnnualReportAnnex(
                    ReportType.valueOf(reportType.toUpperCase(AccountingConstants.LANGUAGE)), fiscalYearId);
        } catch (IllegalArgumentException e) {
            log.error(AccountingConstants.LOG_REPORT_TYPE_INVALID, reportType);
            throw new HttpCustomException(ApiErrors.Accounting.REPORT_TYPE_INVALID,
                    new ErrorsResponse().error(reportType));
        }
    }

    @PutMapping(value = "/report-line/{reportLineId}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "UPDATE_FINANCIAL_STATES_REPORTS_FORMULA" })
    public ReportLineDto reportLine(@RequestBody ReportLineDto reportLine, @RequestHeader("User") String user) {
        return reportLineService.saveReportLine(reportLine, user);
    }

    @GetMapping(value = "/journals-state")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_JOURNALS_REPORTS", "PRINT_JOURNALS_REPORTS" })
    public Page<JournalStateDto> findJournalsState(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate) {
        return journalStateService.findJournalsState(page, size, startDate, endDate);
    }

    @GetMapping(value = "/journals-state-details/{journalId}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_JOURNALS_REPORTS" })
    public Page<JournalStateDetailsDto> findJournalStateDetails(@PathVariable Long journalId,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            Pageable pageable) {
        return journalStateService.findJournalStateDetails(journalId, startDate, endDate, pageable);
    }

    @PostMapping(value = "/bank-reconciliation-statement")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "RECONCILE_ENTRY", "UNRECONCILE_ENTRY" })
    public BankReconciliationStatement saveBankReconciliationStatement(
            @RequestBody BankReconciliationStatementDto bankReconciliationStatementdto) {
        return bankReconciliationStatementService
                .saveOrUpdateBankReconciliationStatement(bankReconciliationStatementdto);
    }

    @PostMapping(value = "/bank-reconciliation")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_RECONCILIATION_BANK", "PRINT_RECONCILIATION_BANK" })
    public BankReconciliationPageDto getBankReconciliationStatement(@RequestParam Long accountId,
            @RequestParam Long fiscalYearId, @RequestParam(defaultValue = "0") int closeMonth,
            @RequestBody List<CloseDocumentAccountLineDto> documentAccountLineReleased, Pageable pageable) {
        return bankReconciliationStatementService.getBankReconciliationStatement(accountId, fiscalYearId, closeMonth,
                documentAccountLineReleased, pageable);
    }

    @PostMapping(value = "/all-bank-reconciliation")
    public List<CloseDocumentAccountLineDto> getAllBankReconciliationStatement(@RequestParam Long accountId,
            @RequestParam Long fiscalYearId, @RequestParam(defaultValue = "0") int closeMonth,
            @RequestBody List<CloseDocumentAccountLineDto> documentAccountLineReleased) {
        return bankReconciliationStatementService.getAllBankReconciliationStatement(accountId, fiscalYearId, closeMonth,
                documentAccountLineReleased);
    }

    @GetMapping(value = "/bank-reconciliation-statement-report")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_RECONCILIATION_BANK" })
    public List<CloseDocumentAccountLineDto> generateBankReconciliaionReport(@RequestParam Long accountId,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate) {
        return documentAccountLineService.getReconcilableDocumentAccountLineInBetween(accountId, startDate, endDate);
    }

    @GetMapping(value = "/bank-reconciliation-report")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_RECONCILIATION_BANK" })
    public List<CloseDocumentAccountLineDto> generateBankReconciliaionStatementReport(@RequestParam Long fiscalYearId,
            @RequestParam Long accountId, @RequestParam int closeMonth) {
        return bankReconciliationStatementService.generateBankReconciliationReport(fiscalYearId, accountId, closeMonth);
    }

    @PostMapping(value = "/calculate-amortization")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "AMORTIZATION_OF_IMMOBILIZATIONS" })
    public AmortizationTableDto calculateAmortization(@RequestBody DepreciationAssetsDto depreciationAssetsDto) {
        return amortizationTableService.calculateAmortization(depreciationAssetsDto);
    }

    @PostMapping(value = "/depreciation-of-asset")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "AMORTIZATION_OF_IMMOBILIZATIONS", "ADD_ACTIVE", "UPDATE_ACTIVE" })
    public DepreciationAssets saveOrUpdateDepreciationAsset(
            @RequestBody @Valid DepreciationAssetsDto depreciationAssetsDto) {
        return depreciationAssetService.saveOrUpdateDepreciationAsset(depreciationAssetsDto);
    }

    @GetMapping(value = "/depreciation-of-asset/{idAssets}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "AMORTIZATION_OF_IMMOBILIZATIONS" })
    public DepreciationAssets getDepreciationAsset(@PathVariable Long idAssets) {
        return depreciationAssetService.findByIdAssets(idAssets);
    }

    @DeleteMapping(value = "/depreciation-of-asset/{idAssets}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "AMORTIZATION_OF_IMMOBILIZATIONS" })
    public DepreciationAssets deleteDepreciationAsset(@PathVariable Long idAssets) {
        return depreciationAssetService.deleteByIdAssets(idAssets);
    }

    @GetMapping(value = "/amortization-code/{planCode}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "AMORTIZATION_OF_IMMOBILIZATIONS" })
    public int generateProposedAmortizationAccountCode(@PathVariable int planCode) {
        return depreciationAssetService.generateProposedAmortizationAccount(planCode);
    }

    @GetMapping(value = "/configurationCookie")
    public List<String> setConfigurationCookie(
            @RequestHeader("User") String user, @RequestHeader("Content-Type") String contentType,
            @RequestHeader("Authorization") String authorization) {
        return reportLineService.setConfigurationCookie(user, contentType, authorization);
    }

    @GetMapping(value = "/amortization-report/{fiscalYearId}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "AMORTIZATION_OF_IMMOBILIZATIONS" })
    public List<AmortizationTableReportDto> getDepreciationAssets(@PathVariable Long fiscalYearId,
            @RequestParam(value = "contentType") String contentType,
            @RequestParam(value = "user") String user, @RequestParam(value = "authorization") String authorization) {
        return reportLineService.generateAmortizationReport(fiscalYearId, contentType, user, authorization);
    }

    @GetMapping(value = "/tomcat-server-url")
    public ObjectNode getTomcatServerUrl() {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode childNode1 = mapper.createObjectNode();
        return ((ObjectNode) childNode1).put("url", tomcatServerUrl);
    }

    @GetMapping(value = "/check-balanced-plan")
    public void checkBalancedChartAccount(@RequestParam Long fiscalYearId) {
        reportLineService.checkAccountsBalanced(fiscalYearId);
    }

    @GetMapping(value = "/unbalanced-charts")
    public List<Integer> getUnbalancedChartAccounts(@RequestParam Long fiscalYearId) {
        return reportLineService.getUnbalancedChartAccounts(fiscalYearId);
    }

    @PostMapping(value = "/centralizing-journal")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_JOURNALS_REPORTS", "PRINT_JOURNALS_REPORTS" })
    public CentralizingJournalDto findCentralizingJournal(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            @RequestBody List<Long> journalIds, @RequestParam int breakingAccount,
            @RequestParam int breakingCustomerAccount, @RequestParam int breakingSupplierAccount) {
        return journalService.findCentralizingJournalPage(page, size, startDate, endDate, journalIds, breakingAccount,
                breakingCustomerAccount, breakingSupplierAccount);
    }

    @GetMapping(value = "/centralizing-journal-details")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_JOURNALS_REPORTS" })
    public List<CentralizingJournalDetailsDto> findCentralizingJournalDetails(@RequestParam Long journalId,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            @RequestParam int breakingAccount, @RequestParam int breakingCustomerAccount,
            @RequestParam int breakingSupplierAccount) {
        return journalService.findCentralizingJournalDetails(journalId, startDate, endDate, breakingAccount,
                breakingCustomerAccount, breakingSupplierAccount);
    }

    @GetMapping(value = "/centralizing-journal-details-by-month")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_JOURNALS_REPORTS", "PRINT_JOURNALS_REPORTS" })
    public Page<CentralizingJournalDetailsByMonthDto> findCentralizingJournalDetailsByMonth(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
            @RequestParam Long journalId,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            @RequestParam int breakingAccount, @RequestParam int breakingCustomerAccount,
            @RequestParam int breakingSupplierAccount, @RequestParam String month) {
        return journalService.findCentralizingJournalDetailsByMonthPage(page, size, journalId, startDate, endDate,
                breakingAccount, breakingCustomerAccount, breakingSupplierAccount, month);
    }

    @GetMapping(value = "/centralizing-journal-report")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "VIEW_JOURNALS_REPORTS", "PRINT_JOURNALS_REPORTS" })
    public List<CentralizingJournalReportLineDto> getReport(
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            @RequestParam int breakingAccount, @RequestParam int breakingCustomerAccount,
            @RequestParam int breakingSupplierAccount, @RequestParam List<Long> journalIds) {
        return journalService.generateCentralizingJournalTelerikReportLines(journalIds, startDate, endDate,
                breakingAccount, breakingCustomerAccount, breakingSupplierAccount);
    }

    @PostMapping(value = "/account-depreciation-assets")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "AMORTIZATION_OF_IMMOBILIZATIONS" })
    public Map<Long, String> getAccountDepreciationAssets(@RequestBody List<Long> idAssets) {
        return depreciationAssetService.getAccountDepreciationAssets(idAssets);
    }

    @GetMapping(value = "/depreciation-of-asset-category-set/{idCategory}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "AMORTIZATION_OF_IMMOBILIZATIONS" })
    public boolean checkCategorySetById(@PathVariable Long idCategory) {
        return depreciationAssetConfigurationService.checkCategorySetById(idCategory);

    }
}
