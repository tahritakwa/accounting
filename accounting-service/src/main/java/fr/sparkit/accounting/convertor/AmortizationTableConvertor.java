package fr.sparkit.accounting.convertor;

import fr.sparkit.accounting.dto.AmortizationTableDto;
import fr.sparkit.accounting.entities.AmortizationTable;
import fr.sparkit.accounting.entities.DepreciationAssets;
import fr.sparkit.accounting.entities.FiscalYear;

public final class AmortizationTableConvertor {

    private AmortizationTableConvertor() {
        super();
    }

    public static AmortizationTableDto modelToDto(AmortizationTable amortizationTable) {
        return new AmortizationTableDto(amortizationTable.getAssets().getId(),
                amortizationTable.getFiscalYear().getId(), amortizationTable.getAcquisitionValue(),
                amortizationTable.getPreviousDepreciation(), amortizationTable.getAnnuityExercise(),
                amortizationTable.getVcn());
    }

    public static AmortizationTable dtoToModel(AmortizationTableDto amortizationTableDto,
            DepreciationAssets depreciationAssets, FiscalYear fiscalYear) {
        return new AmortizationTable(depreciationAssets, fiscalYear, amortizationTableDto.getAcquisitionValue(),
                amortizationTableDto.getPreviousDepreciation(), amortizationTableDto.getAnnuityExercise(),
                amortizationTableDto.getVcn());
    }
}
