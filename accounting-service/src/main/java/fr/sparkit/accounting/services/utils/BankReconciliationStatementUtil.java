package fr.sparkit.accounting.services.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class BankReconciliationStatementUtil {

    private BankReconciliationStatementUtil() {
        super();
    }

    public static LocalDate getCloseMonth(LocalDateTime fiscalYearFirstDate, int closeMonth) {
        return LocalDate.of(fiscalYearFirstDate.getYear(), closeMonth, fiscalYearFirstDate.getDayOfMonth());
    }
}
