package fr.sparkit.accounting.services;

import org.springframework.stereotype.Service;

import fr.sparkit.accounting.dto.DepreciationAssetsConfigurationDto;
import fr.sparkit.accounting.entities.DepreciationAssetsConfiguration;

@Service
public interface IDepreciationAssetConfigurationService extends IGenericService<DepreciationAssetsConfiguration, Long> {

    DepreciationAssetsConfiguration saveOrUpdateDepreciationAssetConfiguration(
            DepreciationAssetsConfigurationDto depreciationAssetsConfigurationDto);

    DepreciationAssetsConfiguration findByIdCategory(Long idCategory);

    boolean checkCategorySetById(Long idCategory);

    boolean deleteAllDepreciationAssetsConfiguration();

    boolean deleteByIdCategory(Long idCategory);
}
