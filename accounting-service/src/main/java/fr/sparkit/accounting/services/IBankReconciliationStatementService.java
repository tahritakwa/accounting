package fr.sparkit.accounting.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.sparkit.accounting.dto.BankReconciliationPageDto;
import fr.sparkit.accounting.dto.BankReconciliationStatementDto;
import fr.sparkit.accounting.dto.CloseDocumentAccountLineDto;
import fr.sparkit.accounting.entities.BankReconciliationStatement;

@Service
public interface IBankReconciliationStatementService extends IGenericService<BankReconciliationStatement, Long> {

    BankReconciliationStatement saveBankReconciliationStatement(
            BankReconciliationStatementDto bankReconciliationStatement);

    Optional<BigDecimal> getFinalAmountReconciliationBank(Long accountId, int closeMonth, Long fiscalYearId);

    BankReconciliationPageDto getBankReconciliationStatement(Long accountId, Long fiscalYearId, Integer closeMonth,
            List<CloseDocumentAccountLineDto> documentAccountLineReleased, Pageable pageable);

    List<CloseDocumentAccountLineDto> getAllBankReconciliationStatement(Long accountId, Long fiscalYearId, Integer closeMonth,
            List<CloseDocumentAccountLineDto> documentAccountLineReleased);

    BankReconciliationStatement saveOrUpdateBankReconciliationStatement(
            BankReconciliationStatementDto bankReconciliationStatementDto);

    List<CloseDocumentAccountLineDto> generateBankReconciliationReport(Long fiscalYearId, Long accountId,
            int closeMonth);
}
