package fr.sparkit.accounting.convertor;

import java.io.Serializable;

import fr.sparkit.accounting.dto.ChartAccountsDto;
import fr.sparkit.accounting.entities.ChartAccounts;

public final class ChartAccountConvertor implements Serializable {
    private static final long serialVersionUID = 1L;

    public ChartAccountConvertor() {
        super();
    }

    public static ChartAccountsDto modelToDto(ChartAccounts chartAccounts) {
        if (chartAccounts == null) {
            return null;
        }
        ChartAccounts accountParent = new ChartAccounts();
        if (chartAccounts.getAccountParent() != null) {
            accountParent = chartAccounts.getAccountParent();
        }
        return new ChartAccountsDto(chartAccounts.getId(), accountParent.getId(), chartAccounts.getCode(),
                chartAccounts.getLabel(), null);
    }

}
