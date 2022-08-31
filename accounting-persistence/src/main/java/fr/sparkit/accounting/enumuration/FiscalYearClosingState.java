package fr.sparkit.accounting.enumuration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum FiscalYearClosingState {
    OPEN(0), PARTIALLY_CLOSED(1), CONCLUDED(2), CLOSED(-1);

    private int value;

    public static FiscalYearClosingState fromValue(int value) {
        for (FiscalYearClosingState state : FiscalYearClosingState.values()) {
            if (state.value == value) {
                return state;
            }
        }
        return null;
    }
}
