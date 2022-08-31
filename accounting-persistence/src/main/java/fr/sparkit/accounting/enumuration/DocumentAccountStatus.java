package fr.sparkit.accounting.enumuration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum DocumentAccountStatus {
    MANUALLY_CREATED(0),
    BY_IMPORT_DOCUMENT_IS_CREATED(1),
    BY_CONCLUDING_CURRENT_FISCAL_YEAR_IS_CREATED(2),
    BY_GENERATION_FROM_AMORTIZAION_IS_CREATED(3),
    BY_IMPORT_SETTLEMENT_IS_CREATED(4);

    private int index;

}
