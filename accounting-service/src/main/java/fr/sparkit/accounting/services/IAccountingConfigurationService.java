package fr.sparkit.accounting.services;

import java.time.LocalDateTime;

import fr.sparkit.accounting.dto.AccountingConfigurationDto;
import fr.sparkit.accounting.dto.AccountingEnvironmentDto;
import fr.sparkit.accounting.dto.FiscalYearDto;

public interface IAccountingConfigurationService {
    AccountingConfigurationDto findLastConfig();

    AccountingConfigurationDto saveConfiguration(AccountingConfigurationDto accountingConfigurationDto);

    Long getCurrentFiscalYearId();

    FiscalYearDto getCurrentFiscalYear();

    boolean isDateInCurrentFiscalYear(LocalDateTime date);

    AccountingEnvironmentDto getBuildProperties();

    FiscalYearDto updateCurrentFiscalYear(Long fiscalYearId);

}
