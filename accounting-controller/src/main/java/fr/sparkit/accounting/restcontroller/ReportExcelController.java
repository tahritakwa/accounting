package fr.sparkit.accounting.restcontroller;

import java.time.LocalDateTime;
import java.util.List;

import fr.sparkit.accounting.auditing.HasRoles;
import fr.sparkit.accounting.dto.FileUploadDto;
import fr.sparkit.accounting.dto.ReportTemplateDefaultParameters;
import fr.sparkit.accounting.enumuration.ReportType;
import fr.sparkit.accounting.services.IReportExcelService;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.errors.ErrorsResponse;
import fr.sparkit.accounting.util.http.HttpCustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.sparkit.accounting.constants.AccountingConstants;
import lombok.extern.slf4j.Slf4j;

@RestController
@CrossOrigin("*")
@Slf4j
@RequestMapping("/api/accounting/reports/excel")
public class ReportExcelController {

    private final IReportExcelService reportExcelService;

    @Autowired
    public ReportExcelController(IReportExcelService reportExcelService) {
        super();
        this.reportExcelService = reportExcelService;
    }

    @PostMapping(value = "/annual-report/{fiscalYearId}/{reportType}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "PRINT_FINANCIAL_STATES_REPORTS" })
    public ResponseEntity<FileUploadDto> exportAnnualReportAsExcelFile(@PathVariable("fiscalYearId") Long fiscalYearId,
            @PathVariable("reportType") String reportType, @RequestBody ReportTemplateDefaultParameters reportParams) {
        try {
            return new ResponseEntity<>(
                    reportExcelService.exportAnnualReportAsExcelFile(fiscalYearId,
                            ReportType.valueOf(reportType.toUpperCase(AccountingConstants.LANGUAGE)), reportParams),
                    HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            log.error(AccountingConstants.LOG_REPORT_TYPE_INVALID, reportType);
            throw new HttpCustomException(ApiErrors.Accounting.REPORT_TYPE_INVALID,
                    new ErrorsResponse().error(reportType));
        }
    }

    @PostMapping(value = "/annual-report-annex/{fiscalYearId}/{reportType}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "PRINT_FINANCIAL_STATES_REPORTS" })
    public ResponseEntity<FileUploadDto> exportAnnualReportAnnexAsExcelFile(
            @PathVariable("fiscalYearId") Long fiscalYearId, @PathVariable("reportType") String reportType,
            @RequestBody ReportTemplateDefaultParameters reportParams) {
        try {
            return new ResponseEntity<>(
                    reportExcelService.exportAnnualReportAnnexAsExcelFile(fiscalYearId,
                            ReportType.valueOf(reportType.toUpperCase(AccountingConstants.LANGUAGE)), reportParams),
                    HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            log.error(AccountingConstants.LOG_REPORT_TYPE_INVALID, reportType);
            throw new HttpCustomException(ApiErrors.Accounting.REPORT_TYPE_INVALID,
                    new ErrorsResponse().error(reportType));
        }
    }

    @PostMapping(value = "/trial-balance")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "PRINT_EDITIONS_REPORTS" })
    public ResponseEntity<FileUploadDto> exportTrialBalanceReportAsExcelFile(
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            @RequestParam String beginAccountCode, @RequestParam String endAccountCode,
            @RequestBody ReportTemplateDefaultParameters reportParams) {
        return new ResponseEntity<>(reportExcelService.exportTrialBalanceReportAsExcelFile(startDate, endDate,
                beginAccountCode, endAccountCode, reportParams), HttpStatus.OK);

    }

    @PostMapping(value = "/general-ledger")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "PRINT_EDITIONS_REPORTS" })
    public ResponseEntity<FileUploadDto> exportGeneralLedgerReportAsExcelFile(
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            @RequestParam String beginAccountCode, @RequestParam String endAccountCode,
            @RequestParam String beginAmount, @RequestParam String endAmount, @RequestParam String accountType,
            @RequestBody ReportTemplateDefaultParameters reportParams, @RequestParam String letteringOperationType,
            @RequestParam String field, @RequestParam String direction) {
        return new ResponseEntity<>(reportExcelService.exportGeneralLedgerReportAsExcelFile(startDate, endDate,
                beginAccountCode, endAccountCode, beginAmount, endAmount, accountType, reportParams,
                letteringOperationType, field, direction), HttpStatus.OK);

    }

    @PostMapping(value = "/auxiliary-journal-report")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "PRINT_JOURNALS_REPORTS" })
    public ResponseEntity<FileUploadDto> exportAuxiliaryJournalReportAsExcelFile(
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            @RequestParam List<Long> journalIds, @RequestBody ReportTemplateDefaultParameters reportParams) {
        return new ResponseEntity<>(reportExcelService.exportAuxiliaryJournalReportAsExcelFile(startDate, endDate,
                journalIds, reportParams), HttpStatus.OK);
    }

    @PostMapping(value = "/centralizing-journal-report")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "PRINT_JOURNALS_REPORTS" })
    public ResponseEntity<FileUploadDto> exportCentralizingJournalReportAsExcelFile(
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            @RequestParam int breakingAccount, @RequestParam int breakingCustomerAccount,
            @RequestParam int breakingSupplierAccount, @RequestParam List<Long> journalIds,
            @RequestBody ReportTemplateDefaultParameters reportParams) {
        return new ResponseEntity<>(reportExcelService.exportCentralizingJournalReportAsExcelFile(startDate, endDate,
                breakingAccount, breakingCustomerAccount, breakingSupplierAccount, journalIds, reportParams),
                HttpStatus.OK);
    }

    @PostMapping(value = "/centralizing-journal-by-date")
    public ResponseEntity<FileUploadDto> exportCentralizingJournalReportByDateAsExcelFile(
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            @RequestParam int breakingAccount, @RequestParam int breakingCustomerAccount,
            @RequestParam int breakingSupplierAccount, @RequestParam List<Long> journalIds,
            @RequestBody ReportTemplateDefaultParameters reportParams) {
        return new ResponseEntity<>(reportExcelService.exportCentralizingJournalReportByDateAsExcelFile(startDate, endDate,
                breakingAccount, breakingCustomerAccount, breakingSupplierAccount, journalIds, reportParams),
                HttpStatus.OK);
    }

    @PostMapping(value = "/journals-state")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "PRINT_JOURNALS_REPORTS" })
    public ResponseEntity<FileUploadDto> exportStateOfJournalsReportAsExcelFile(
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = AccountingConstants.YYYY_MM_DD_HH_MM_SS) LocalDateTime endDate,
            @RequestBody ReportTemplateDefaultParameters reportParams) {
        return new ResponseEntity<>(
                reportExcelService.exportStateOfJournalsReportAsExcelFile(startDate, endDate, reportParams),
                HttpStatus.OK);
    }

    @PostMapping(value = "/amortization-report/{fiscalYearId}")
    @PreAuthorize("isAuthenticated()")
    @HasRoles(permissions = { "PRINT_AMORTIZATION_TABLE" })
    public ResponseEntity<FileUploadDto> generateDepreciationAssets(@PathVariable Long fiscalYearId,
            @RequestHeader(value = "Content-Type") String contentType,
            @RequestHeader(value = "User") String user, @RequestHeader("Authorization") String authorization,
            @RequestBody ReportTemplateDefaultParameters reportParams) {
        return new ResponseEntity<>(reportExcelService.exportAmortizationReportAsExcelFile(fiscalYearId, user,
                contentType, authorization, reportParams), HttpStatus.OK);
    }
}
