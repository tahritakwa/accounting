package fr.sparkit.accounting.services;

import java.util.List;

import java.util.Optional;

import org.springframework.stereotype.Service;

import fr.sparkit.accounting.dto.AmortizationTableDto;
import fr.sparkit.accounting.dto.DepreciationAssetsDto;
import fr.sparkit.accounting.dto.FiscalYearDto;
import fr.sparkit.accounting.entities.AmortizationTable;

@Service
public interface IAmortizationtableService extends IGenericService<AmortizationTable, Long> {

    AmortizationTableDto calculateAmortization(DepreciationAssetsDto depreciationAssetsDto);

    Optional<AmortizationTable> findAmortizationTable(Long fiscalYearId, Long assetsId);

    AmortizationTableDto getDepreciationOfAsset(DepreciationAssetsDto depreciationAssetsDto, FiscalYearDto fiscalYear,
            Long idCategory);

    List<AmortizationTable> findByFiscalYear(Long fiscalYearId);
}
