package fr.sparkit.accounting.services;

import java.time.LocalDateTime;
import java.util.List;

import fr.sparkit.accounting.dto.FileUploadDto;
import fr.sparkit.accounting.dto.ReportTemplateDefaultParameters;
import fr.sparkit.accounting.enumuration.ReportType;

public interface IReportExcelService {
    FileUploadDto exportAnnualReportAsExcelFile(Long fiscalYearId, ReportType reportType,
            ReportTemplateDefaultParameters reportParams);

    FileUploadDto exportAnnualReportAnnexAsExcelFile(Long fiscalYearId, ReportType reportType,
            ReportTemplateDefaultParameters reportParams);

    FileUploadDto exportTrialBalanceReportAsExcelFile(LocalDateTime startDate, LocalDateTime endDate,
            String beginAccountCode, String endAccountCode, ReportTemplateDefaultParameters reportParams);

    FileUploadDto exportGeneralLedgerReportAsExcelFile(LocalDateTime startDate, LocalDateTime endDate,
            String beginAccountCode, String endAccountCode, String beginAmount, String endAmount, String accountType,
            ReportTemplateDefaultParameters reportParams, String letteringOperationType, String field,
            String direction);

    FileUploadDto exportAuxiliaryJournalReportAsExcelFile(LocalDateTime startDate, LocalDateTime endDate,
            List<Long> journalIds, ReportTemplateDefaultParameters reportParams);

    FileUploadDto exportCentralizingJournalReportAsExcelFile(LocalDateTime startDate, LocalDateTime endDate,
            int breakingAccount, int breakingCustomerAccount, int breakingSupplierAccount, List<Long> journalIds,
            ReportTemplateDefaultParameters reportParams);
    FileUploadDto exportCentralizingJournalReportByDateAsExcelFile(LocalDateTime startDate, LocalDateTime endDate,
            int breakingAccount, int breakingCustomerAccount, int breakingSupplierAccount, List<Long> journalIds,
            ReportTemplateDefaultParameters reportParams);

    FileUploadDto exportStateOfJournalsReportAsExcelFile(LocalDateTime startDate, LocalDateTime endDate,
            ReportTemplateDefaultParameters reportParams);

    FileUploadDto exportAmortizationReportAsExcelFile(Long fiscalYearId, String user, String contentType,
        String authorization, ReportTemplateDefaultParameters reportParams);
}
