package fr.sparkit.accounting.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import fr.sparkit.accounting.entities.AmortizationTable;

@Repository
public interface AmortizationTableDao extends BaseRepository<AmortizationTable, Long> {

    Optional<AmortizationTable> findByFiscalYearIdAndAssetsIdAndIsDeletedFalse(Long fiscalYearId, Long assetsId);

    List<AmortizationTable> findByFiscalYearIdAndIsDeletedFalse(Long fiscalYearId);

}
