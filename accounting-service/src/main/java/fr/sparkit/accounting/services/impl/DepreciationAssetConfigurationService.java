package fr.sparkit.accounting.services.impl;

import static fr.sparkit.accounting.constants.AccountingConstants.LOG_ENTITY_CREATED;
import static fr.sparkit.accounting.constants.AccountingConstants.LOG_ENTITY_UPDATED;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.sparkit.accounting.dao.AccountDao;
import fr.sparkit.accounting.dao.DepreciationAssetsConfigurationDao;
import fr.sparkit.accounting.dto.DepreciationAssetsConfigurationDto;
import fr.sparkit.accounting.entities.Account;
import fr.sparkit.accounting.entities.DepreciationAssetsConfiguration;
import fr.sparkit.accounting.entities.DepreciationAssetsConfiguration.DepreciationAssetsConfigurationBuilder;
import fr.sparkit.accounting.services.IDepreciationAssetConfigurationService;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DepreciationAssetConfigurationService extends GenericService<DepreciationAssetsConfiguration, Long>
        implements IDepreciationAssetConfigurationService {

    private final DepreciationAssetsConfigurationDao depreciationAssetsConfigurationDao;
    private final AccountDao accountDao;

    @Autowired
    public DepreciationAssetConfigurationService(DepreciationAssetsConfigurationDao depreciationAssetsConfigurationDao,
            AccountDao accountDao) {
        super();
        this.depreciationAssetsConfigurationDao = depreciationAssetsConfigurationDao;
        this.accountDao = accountDao;
    }

    @Override
    public DepreciationAssetsConfiguration saveOrUpdateDepreciationAssetConfiguration(
            DepreciationAssetsConfigurationDto depreciationAssetsConfigurationDto) {
        Optional<DepreciationAssetsConfiguration> depreciationAssetsConfigurationOpt = depreciationAssetsConfigurationDao
                .findByIdCategoryAndIsDeletedIsFalse(depreciationAssetsConfigurationDto.getIdCategory());
        Account immobilizationAccount = accountDao
                .findOne(depreciationAssetsConfigurationDto.getImmobilizationAccount());
        Account amortizationAccount = accountDao.findOne(depreciationAssetsConfigurationDto.getAmortizationAccount());
        if (depreciationAssetsConfigurationOpt.isPresent()) {
            DepreciationAssetsConfiguration depreciationAssetsConfiguration = depreciationAssetsConfigurationOpt.get();
            depreciationAssetsConfiguration
                    .setDepreciationPeriod(depreciationAssetsConfigurationDto.getDepreciationPeriod());
            depreciationAssetsConfiguration.setImmobilizationAccount(immobilizationAccount);
            depreciationAssetsConfiguration.setAmortizationAccount(amortizationAccount);
            depreciationAssetsConfiguration
                    .setImmobilizationType(depreciationAssetsConfigurationDto.getImmobilisationTypeText());
            log.info(LOG_ENTITY_UPDATED, depreciationAssetsConfiguration);
            return saveAndFlush(depreciationAssetsConfiguration);
        } else {
            DepreciationAssetsConfigurationBuilder depreciationAssetsConfigurationBuilder = DepreciationAssetsConfiguration
                    .builder().idCategory(depreciationAssetsConfigurationDto.getIdCategory())
                    .depreciationPeriod(depreciationAssetsConfigurationDto.getDepreciationPeriod())
                    .immobilizationAccount(immobilizationAccount).amortizationAccount(amortizationAccount)
                    .immobilizationType(depreciationAssetsConfigurationDto.getImmobilisationTypeText());
            DepreciationAssetsConfiguration assetsConfiguration = saveAndFlush(
                    depreciationAssetsConfigurationBuilder.build());
            depreciationAssetsConfigurationBuilder.id(assetsConfiguration.getId());
            log.info(LOG_ENTITY_CREATED, depreciationAssetsConfigurationBuilder);
            return assetsConfiguration;
        }
    }

    @Override
    public DepreciationAssetsConfiguration findByIdCategory(Long idCategory) {
        Optional<DepreciationAssetsConfiguration> depreciationAssetsConfiguration = depreciationAssetsConfigurationDao
                .findByIdCategoryAndIsDeletedIsFalse(idCategory);
        if (depreciationAssetsConfiguration.isPresent()) {
            return depreciationAssetsConfiguration.get();
        } else {
            throw new HttpCustomException(ApiErrors.Accounting.ACCOUNTING_CONFIGURATION_CATEGORY_NOT_FOUND);
        }
    }

    @Override
    public boolean checkCategorySetById(Long idCategory) {
        Optional<DepreciationAssetsConfiguration> depreciationAssetsConfiguration = depreciationAssetsConfigurationDao
                .findByIdCategoryAndIsDeletedIsFalse(idCategory);
        return depreciationAssetsConfiguration.isPresent();
    }

    @Override
    public boolean deleteAllDepreciationAssetsConfiguration() {
        List<DepreciationAssetsConfiguration> depreciationAssetsConfigurationList = depreciationAssetsConfigurationDao
                .findAll();
        deleteInBatchSoft(depreciationAssetsConfigurationList);
        return true;
    }

    @Override
    public boolean deleteByIdCategory(Long idCategory) {
        Optional<DepreciationAssetsConfiguration> depreciationAssetsConfiguration = depreciationAssetsConfigurationDao
                .findByIdCategoryAndIsDeletedIsFalse(idCategory);
        if (depreciationAssetsConfiguration.isPresent()) {
            delete(depreciationAssetsConfiguration.get().getId());
        }
        return true;
    }

}
