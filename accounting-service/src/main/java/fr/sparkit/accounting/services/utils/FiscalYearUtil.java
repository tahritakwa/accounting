package fr.sparkit.accounting.services.utils;

import static fr.sparkit.accounting.services.utils.AccountingServiceUtil.isDateBeforeOrEquals;

import fr.sparkit.accounting.dto.FiscalYearDto;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.http.HttpCustomException;

public final class FiscalYearUtil {

    private FiscalYearUtil() {
        super();
    }

    public static void checkValuesNotNull(FiscalYearDto fiscalYearDto) {
        if ((fiscalYearDto.getName() == null) || fiscalYearDto.getName().isEmpty()
                || (fiscalYearDto.getStartDate() == null) || (fiscalYearDto.getEndDate() == null)) {
            throw new HttpCustomException(ApiErrors.Accounting.FISCAL_YEAR_MISSING_PARAMETERS);
        }
    }

    public static void checkDatesOrderValid(FiscalYearDto fiscalYearDto) {
        if (!isDateBeforeOrEquals(fiscalYearDto.getStartDate(), fiscalYearDto.getEndDate())) {
            throw new HttpCustomException(ApiErrors.Accounting.FISCAL_YEAR_DATES_ORDER_INVALID);
        }
    }
}
