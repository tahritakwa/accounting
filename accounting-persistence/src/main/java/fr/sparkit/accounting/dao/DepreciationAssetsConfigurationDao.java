package fr.sparkit.accounting.dao;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import fr.sparkit.accounting.entities.DepreciationAssetsConfiguration;

@Repository
public interface DepreciationAssetsConfigurationDao extends BaseRepository<DepreciationAssetsConfiguration, Long> {

    Optional<DepreciationAssetsConfiguration> findByIdCategoryAndIsDeletedIsFalse(Long idCategory);
}
