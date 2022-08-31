package fr.sparkit.accounting.services.utils;

import fr.sparkit.accounting.dto.TemplateAccountingDto;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.http.HttpCustomException;

public final class TemplateAccountingUtil {

    private TemplateAccountingUtil() {
        super();
    }

    public static boolean isUpdateTemplateAccounting(TemplateAccountingDto templateAccountingDto) {
        return templateAccountingDto.getId() != null;
    }

    public static void checkNull(Object obj) {
        if (obj == null) {
            throw new HttpCustomException(ApiErrors.Accounting.TEMPLATE_ACCOUNTING_MISSING_PARAMETERS);
        }
    }
}
