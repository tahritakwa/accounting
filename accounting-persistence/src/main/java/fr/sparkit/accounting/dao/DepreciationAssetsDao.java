package fr.sparkit.accounting.dao;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import fr.sparkit.accounting.entities.DepreciationAssets;

@Repository
public interface DepreciationAssetsDao extends BaseRepository<DepreciationAssets, Long> {

    Optional<DepreciationAssets> findByIdAssetsAndIsDeletedFalse(Long idAssets);

}
