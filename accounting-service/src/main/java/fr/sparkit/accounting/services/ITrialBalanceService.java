package fr.sparkit.accounting.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;

import fr.sparkit.accounting.dto.TrialBalanceAccountDto;
import fr.sparkit.accounting.dto.TrialBalancePageDto;
import fr.sparkit.accounting.dto.TrialBalanceReportLineDto;

public interface ITrialBalanceService {

    TrialBalancePageDto findTrialBalanceAccounts(int page, int size, LocalDateTime startDate, LocalDateTime endDate,
            String beginAccountCode, String endAccountCode);

    TrialBalancePageDto getTrialBalanceAccounts(Pageable pageable, LocalDateTime startDate, LocalDateTime endDate,
            int parseBeginAccountCode, int parseEndAccountCode);

    List<TrialBalanceReportLineDto> generateTrialBalanceTelerikReport(LocalDateTime startDate, LocalDateTime endDate,
            String beginAccountCode, String endAccountCode);

    void insertTrialBalanceAccountLineIntoReport(List<TrialBalanceReportLineDto> trialBalanceReport,
            TrialBalanceAccountDto trialBalanceAccountDto);

    void insertTrialBalanceTotalLineIntoReport(List<TrialBalanceAccountDto> trialBalanceAccounts,
            List<TrialBalanceReportLineDto> trialBalanceReport);

    BigDecimal calculateTotalInitialCreditTrialBalance(List<TrialBalanceAccountDto> trialBalanceAccounts);

    BigDecimal calculateTotalInitialDebitTrialBalance(List<TrialBalanceAccountDto> trialBalanceAccounts);

    BigDecimal calculateTotalCurrentCreditTrialBalance(List<TrialBalanceAccountDto> trialBalanceAccounts);

    BigDecimal calculateTotalCurrentDebitTrialBalance(List<TrialBalanceAccountDto> trialBalanceAccounts);

    void insertTrialBalanceSubTotalLineIntoReport(int classNumber, TrialBalanceAccountDto subTotalClassDto,
            List<TrialBalanceReportLineDto> trialBalanceReport);

    void insertTrialBalanceResultLineIntoReport(TrialBalanceAccountDto trialBalanceResultDto,
            List<TrialBalanceReportLineDto> trialBalanceReport);

}
