package fr.sparkit.accounting.dao;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import fr.sparkit.accounting.entities.BankReconciliationStatement;

@Repository
public interface BankReconciliationStatementDao extends BaseRepository<BankReconciliationStatement, Long> {

    Optional<BankReconciliationStatement> findByFiscalYearIdAndAccountIdAndCloseMonthAndIsDeletedFalse(
            long fiscalYearId, long accountId, int closeMonth);

    @Query("SELECT ba.finalAmount FROM BankReconciliationStatement ba WHERE ba.closeMonth=?1 AND ba.account.id=?2 "
            + "AND ba.fiscalYear.id=?3 AND ba.isDeleted=false")
    Optional<BigDecimal> getFinalAmount(int closeMonth, Long accountId, Long fisclaYearId);
}
