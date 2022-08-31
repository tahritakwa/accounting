package fr.sparkit.accounting.dao;

import fr.sparkit.accounting.entities.AccountingConfiguration;

public interface AccountingConfigurationDao extends BaseRepository<AccountingConfiguration, Long> {

    AccountingConfiguration findFirstByIsDeletedFalseOrderByIdDesc();
}
