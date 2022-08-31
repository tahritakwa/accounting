package fr.sparkit.accounting.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import fr.sparkit.accounting.dto.GeneralLedgerAccountDetailsDto;
import fr.sparkit.accounting.dto.GeneralLedgerAccountDto;
import fr.sparkit.accounting.dto.GeneralLedgerDetailsPageDto;
import fr.sparkit.accounting.dto.GeneralLedgerPageDto;
import fr.sparkit.accounting.dto.GeneralLedgerReportLineDto;
import fr.sparkit.accounting.entities.DocumentAccountLine;

public interface IGeneralLedgerService {

    GeneralLedgerPageDto findGeneralLedgerAccounts(int page, int size, LocalDateTime startDate, LocalDateTime endDate,
            String beginAccountCode, String endAccountCode, String beginAmount, String endAmount, String accountType,
            String letteringOperationType);

    GeneralLedgerDetailsPageDto findGeneralLedgerAccountDetails(Long accountId, int page, int size,
            LocalDateTime startDate, LocalDateTime endDate, String beginAmount, String endAmount,
            String letteringOperationType, String field, String direction);

    void addToGeneralLedgerAccountDetails(DocumentAccountLine documentAccountLine,
            List<GeneralLedgerAccountDetailsDto> generalLedgerDetails, BigDecimal lastBalance);

    List<GeneralLedgerReportLineDto> generateGeneralLedgerTelerikReport(LocalDateTime startDate, LocalDateTime endDate,
            String beginAccountCode, String endAccountCode, String beginAmount, String endAmount, String accountType,
            String letteringOperationType, String field, String direction);

    void insertSubTotalAccountLineIntoReport(List<GeneralLedgerReportLineDto> generalLedgerReportDto,
            GeneralLedgerAccountDto generalLedgerAccountDto);

    void insertGeneralLedgerTotalLineIntoReport(List<GeneralLedgerReportLineDto> generalLedgerReport,
            LocalDateTime endDate, BigDecimal totalCredit, BigDecimal totalDebit);

    void insertDocumentAccountLineIntoReport(BigDecimal balance, List<GeneralLedgerReportLineDto> generalLedgerReport,
            DocumentAccountLine documentAccountLine);

    BigDecimal calculateTotalCreditGeneralLedger(List<GeneralLedgerAccountDto> generalLedgerAccounts);

    BigDecimal calculateTotalDebitGeneralLedger(List<GeneralLedgerAccountDto> generalLedgerAccounts);

}
