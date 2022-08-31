package fr.sparkit.accounting.restcontroller;

import java.time.LocalDateTime;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import fr.sparkit.accounting.auditing.HasRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.dto.FileUploadDto;
import fr.sparkit.accounting.dto.ReportLineDto;
import fr.sparkit.accounting.dto.ReportTemplateDefaultParameters;
import fr.sparkit.accounting.enumuration.ReportType;
import fr.sparkit.accounting.services.IReportJasperService;
import fr.sparkit.accounting.services.IReportLineService;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.errors.ErrorsResponse;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;

@RestController
@CrossOrigin("*")
@Slf4j
@RequestMapping("/api/accounting/reports/jasper")
public class ReportJasperController {
    private final IReportJasperService reportJasperService;
    private final IReportLineService reportLineService;

    @Value("${tomcat.server.url}")
    private String tomcatServerUrl;
    @Value("${dotnet.url}")
    private String dotnetRessource;

    @Autowired
    public ReportJasperController(IReportJasperService reportJasperService, IReportLineService reportLineService) {
        super();
        this.reportJasperService = reportJasperService;
        this.reportLineService = reportLineService;
    }

    @PostMapping(value = "/general-ledger-report-jasper")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "PRINT_EDITIONS_REPORTS" })
    public ResponseEntity<FileUploadDto> findGeneralLedgerReportWithJasper(
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            @RequestParam String beginAccountCode, @RequestParam String endAccountCode,
            @RequestParam String beginAmount, @RequestParam String endAmount, @RequestParam String accountType,
            @RequestBody ReportTemplateDefaultParameters reportTemplateParams,
            @RequestParam String letteringOperationType, @RequestParam String field, @RequestParam String direction) {

        return new ResponseEntity(reportJasperService.generateGeneralLedgerReport(startDate, endDate, beginAccountCode,
                endAccountCode, beginAmount, endAmount, accountType, reportTemplateParams, letteringOperationType,
                field, direction), HttpStatus.OK);
    }

    @PostMapping(value = "/centralizing-journal-report")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "PRINT_JOURNALS_REPORTS" })
    public ResponseEntity<FileUploadDto> getCentralizationJasperReport(
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            @RequestParam int breakingAccount, @RequestParam int breakingCustomerAccount,
            @RequestParam int breakingSupplierAccount, @RequestParam List<Long> journalIds,
            @RequestBody ReportTemplateDefaultParameters reportTemplateParams) {
        return new ResponseEntity(
                reportJasperService.generateCentralizingJournalReport(startDate, endDate, breakingAccount,
                        breakingCustomerAccount, breakingSupplierAccount, journalIds, reportTemplateParams),
                HttpStatus.OK);
    }

    @PostMapping(value = "/auxiliary-journal-report")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "PRINT_JOURNALS_REPORTS" })
    public ResponseEntity<FileUploadDto> getAuxiliaryJasperReport(
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            @RequestParam List<Long> journalIds, @RequestBody ReportTemplateDefaultParameters reportTemplateParams) {
        return new ResponseEntity(reportJasperService.generateAuxiliaryJournalJasperReport(startDate, endDate,
                journalIds, reportTemplateParams), HttpStatus.OK);
    }

    @PostMapping(value = "/balance-Jasper-report")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "PRINT_EDITIONS_REPORTS" })
    public ResponseEntity<FileUploadDto> getBalanceTrialJasperReport(
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            @RequestParam String beginAccountCode, @RequestParam String endAccountCode,
            @RequestBody ReportTemplateDefaultParameters reportTemplateParams) {

        return new ResponseEntity(reportJasperService.generateTrialBalanceJasperReport(startDate, endDate,
                beginAccountCode, endAccountCode, reportTemplateParams), HttpStatus.OK);
    }

    @PostMapping(value = "/bank-reconciliation-Jasper-report")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "PRINT_RECONCILIATION_BANK_STATEMENT" })
    public ResponseEntity<FileUploadDto> getBankReconciliationJasperReport(@RequestParam Long fiscalYearId,
            @RequestParam Long accountId, @RequestParam int closeMonth, @RequestParam String period,
            @RequestParam String initialAmountCredit, @RequestParam String finalAmountCredit,
            @RequestParam String initialAmountDebit, @RequestParam String finalAmountDebit,
            @RequestBody ReportTemplateDefaultParameters reportTemplateParams) {
        return new ResponseEntity(reportJasperService.generateBankReconciliationJasperReport(fiscalYearId, accountId,
                closeMonth, period, initialAmountCredit, finalAmountCredit, initialAmountDebit, finalAmountDebit,
                reportTemplateParams), HttpStatus.OK);
    }

    @PostMapping(value = "/bank-reconciliation-Statement-Jasper-report")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "PRINT_RECONCILIATION_BANK_STATEMENT" })
    public ResponseEntity<FileUploadDto> getBankeconciliationStatementJasperReport(@RequestParam Long accountId,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam String period, @RequestBody ReportTemplateDefaultParameters reportTemplateParams) {
        return new ResponseEntity(reportJasperService.generateBankReconciliationStatementJasperReport(accountId,
                startDate, endDate, period, reportTemplateParams), HttpStatus.OK);
    }

    @PostMapping(value = "/annual-report-annex-jasper/{fiscalYearId}/{reportType}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "PRINT_FINANCIAL_STATES_REPORTS" })
    public ResponseEntity<FileUploadDto> generateAnnualReportAnnex(@PathVariable("fiscalYearId") Long fiscalYearId,
            @PathVariable("reportType") String reportType,
            @RequestBody ReportTemplateDefaultParameters reportTemplateParams) {
        try {
            return new ResponseEntity(reportJasperService.generateAnnualReportAnnexJasperReport(reportType,
                    fiscalYearId, reportTemplateParams), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            log.error(AccountingConstants.LOG_REPORT_TYPE_INVALID, reportType);
            throw new HttpCustomException(ApiErrors.Accounting.REPORT_TYPE_INVALID,
                    new ErrorsResponse().error(reportType));
        }
    }

    @PostMapping(value = "/annual-report-detailed-jasper/{fiscalYearId}/{reportType}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "PRINT_FINANCIAL_STATES_REPORTS" })
    public ResponseEntity<FileUploadDto> generateDetailedAnnualReport(@PathVariable("fiscalYearId") Long fiscalYearId,
            @PathVariable("reportType") String reportType,
            @RequestBody ReportTemplateDefaultParameters reportTemplateParams) {
        return new ResponseEntity(reportJasperService.generateDetailedAnnualReport(fiscalYearId,
                getAnnualReport(fiscalYearId, reportType), reportType, reportTemplateParams), HttpStatus.OK);
    }

    @GetMapping(value = "/annual-report-jasper/{fiscalYearId}/{reportType}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "PRINT_FINANCIAL_STATES_REPORTS" })
    public List<ReportLineDto> getAnnualReport(@PathVariable("fiscalYearId") Long fiscalYearId,
            @PathVariable("reportType") String reportType) {
        try {
            if (reportType.equalsIgnoreCase(AccountingConstants.INTERMEDIARY_BALANCE)) {
                List<ReportLineDto> reportLinesForBothIB = reportLineService.getAnnualReport(ReportType.CIB,
                        fiscalYearId);
                reportLinesForBothIB.addAll(reportLineService.getAnnualReport(ReportType.IIB, fiscalYearId));
                return reportLinesForBothIB;
            } else {
                if (reportType.startsWith(ReportType.BS.toString())) {
                    reportType = ReportType.BS.toString();
                }
                return reportLineService.getAnnualReport(
                        ReportType.valueOf(reportType.toUpperCase(AccountingConstants.LANGUAGE)), fiscalYearId);
            }
        } catch (IllegalArgumentException e) {
            log.error(AccountingConstants.LOG_REPORT_TYPE_INVALID, reportType);
            throw new HttpCustomException(ApiErrors.Accounting.REPORT_TYPE_INVALID,
                    new ErrorsResponse().error(reportType));
        }
    }

    @PostMapping(value = "/journals-state-report-jasper")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "PRINT_JOURNALS_REPORTS" })
    public ResponseEntity<FileUploadDto> generateJournalStateReport(
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            @RequestBody ReportTemplateDefaultParameters reportTemplateParams) {
        return new ResponseEntity(
                reportJasperService.generateJournalStateReport(startDate, endDate, reportTemplateParams),
                HttpStatus.OK);
    }

    @PostMapping(value = "/amortization-report/{fiscalYearId}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "PRINT_AMORTIZATION_TABLE" })
    public ResponseEntity<FileUploadDto> generateDepreciationAssets(HttpServletResponse response,
            @PathVariable Long fiscalYearId,
            @RequestHeader(value = "Content-Type") String contentType, @RequestHeader(value = "User") String user,
            @RequestHeader("Authorization") String authorization,
            @RequestBody ReportTemplateDefaultParameters reportTemplateParams) {
        return new ResponseEntity(reportJasperService.generateAmortizationReport(fiscalYearId, user,
                contentType, authorization, reportTemplateParams), HttpStatus.OK);
    }
}
