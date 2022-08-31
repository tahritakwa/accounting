package fr.sparkit.accounting.services;

import fr.sparkit.accounting.dto.AccountingConfigurationDto;
import fr.sparkit.accounting.dto.CloseAndReopeningFiscalYearDto;
import fr.sparkit.accounting.entities.Account;
import fr.sparkit.accounting.entities.FiscalYear;

public interface IFiscalYearClosingAndReOpeningService {

    void closeAndReOpenFiscalYear(String contentType, String user, String authorization,
            CloseAndReopeningFiscalYearDto closeAndReopeningFiscalYear);

    void discardRevenueAndExpenseAccounts(FiscalYear currentFiscalYear, Account resultAccount);

    void reopeningAccountsFromPreviousFiscal(FiscalYear currentFiscalYear, FiscalYear targetFiscalYear,
            boolean literableAccounts, Account resultAccount, AccountingConfigurationDto configuration);

    void stopJournalEntries(FiscalYear currentFiscalYear, AccountingConfigurationDto accountingConfiguration);

    void transferOfDepreciationPeriod(FiscalYear currentFiscalYear, String contentType, String user,
            String authorization);
}
