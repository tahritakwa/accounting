package fr.sparkit.accounting.services;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import fr.sparkit.accounting.dto.AnnexeReportDto;
import fr.sparkit.accounting.dto.FileUploadDto;
import fr.sparkit.accounting.dto.ReportLineDto;
import fr.sparkit.accounting.dto.ReportTemplateDefaultParameters;

public interface IReportJasperService {
    FileUploadDto generateGeneralLedgerReport(LocalDateTime startDate, LocalDateTime endDate, String beginAccountCode,
            String endAccountCode, String beginAmount, String endAmount, String accountType,
            ReportTemplateDefaultParameters reportTemplateParams, String letteringOperationType, String field,
            String direction);

    FileUploadDto generateCentralizingJournalReport(LocalDateTime startDate, LocalDateTime endDate, int breakingAccount,
            int breakingCustomerAccount, int breakingProviderAccount, List<Long> journalIds,
            ReportTemplateDefaultParameters reportTemplateParams);

    FileUploadDto generateTrialBalanceJasperReport(LocalDateTime startDate, LocalDateTime endDate,
            String beginAccountCode, String endAccountCode, ReportTemplateDefaultParameters reportTemplateParams);

    FileUploadDto generateAuxiliaryJournalJasperReport(LocalDateTime startDate, LocalDateTime endDate,
            List<Long> journalIds, ReportTemplateDefaultParameters reportTemplateParams);

    FileUploadDto generateBankReconciliationJasperReport(Long fiscalYearId, Long accountId, int closeMonth,
            String period, String initialAmountCredit, String finalAmountCredit, String initialAmountDebit,
            String finalAmountDebit, ReportTemplateDefaultParameters reportTemplateParams);

    FileUploadDto generateBankReconciliationStatementJasperReport(Long accountId, LocalDateTime startDate,
            LocalDateTime endDate, String period, ReportTemplateDefaultParameters reportTemplateParams);

    FileUploadDto generateAnnualReportAnnexJasperReport(String reportType, Long fiscalYearId,
            ReportTemplateDefaultParameters reportTemplateParams);

    FileUploadDto generateStateOfIncome(Long fiscalYearId, List<ReportLineDto> reportLines,
            ReportTemplateDefaultParameters reportTemplateParams);

    FileUploadDto generateDetailedAnnualReport(Long fiscalYearId, List<ReportLineDto> reportLines, String reportType,
            ReportTemplateDefaultParameters reportTemplateParams);

    FileUploadDto generateAssetBalanceSheet(Long fiscalYearId, List<ReportLineDto> reportLines,
                                            ReportTemplateDefaultParameters reportTemplateParams);

    FileUploadDto generateBalanceSheetEquityAndLiabilities(Long fiscalYearId, List<ReportLineDto> reportLines,
                                            ReportTemplateDefaultParameters reportTemplateParams);

    FileUploadDto generateIntermediaryBalance(Long fiscalYearId, List<ReportLineDto> reportLines,
            ReportTemplateDefaultParameters reportTemplateParams);

    FileUploadDto generateJournalStateReport(LocalDateTime startDate, LocalDateTime endDate,
            ReportTemplateDefaultParameters reportTemplateParams);

    FileUploadDto generateAmortizationReport(Long fiscalYearId, String user, String contentType,
            String authorization, ReportTemplateDefaultParameters reportTemplateParams);

    FileUploadDto generateCashFlowOfReference(Long fiscalYearId, List<ReportLineDto> reportLines,
            ReportTemplateDefaultParameters reportTemplateParams);

    FileUploadDto generateCashFlowAuthorized(Long fiscalYearId, List<ReportLineDto> reportLines,
            ReportTemplateDefaultParameters reportTemplateParams);

    FileUploadDto generateBalanceSheetAnnex(Long fiscalYearId, Collection<?> annexeReportDtos,
                                            ReportTemplateDefaultParameters reportTemplateParams);

    FileUploadDto generateStateOfIncomeAnnex(Long fiscalYearId, Collection<?> annexeReportDtos,
            ReportTemplateDefaultParameters reportTemplateParams);

    FileUploadDto generateCashFlowAuthorizedAnnex(Long fiscalYearId, List<AnnexeReportDto> annexeReportDtos,
            ReportTemplateDefaultParameters reportTemplateParams);


}
