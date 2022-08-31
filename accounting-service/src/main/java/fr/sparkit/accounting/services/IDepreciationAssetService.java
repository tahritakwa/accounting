package fr.sparkit.accounting.services;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import fr.sparkit.accounting.dto.AmortizationTableReportDto;
import fr.sparkit.accounting.dto.DepreciationAssetsDto;
import fr.sparkit.accounting.dto.FiscalYearDto;
import fr.sparkit.accounting.entities.Account;
import fr.sparkit.accounting.entities.DepreciationAssets;

@Service
public interface IDepreciationAssetService extends IGenericService<DepreciationAssets, Long> {

    DepreciationAssets saveOrUpdateDepreciationAsset(DepreciationAssetsDto depreciationAssetsDto);

    DepreciationAssets findByIdAssets(Long idAssets);

    DepreciationAssets deleteByIdAssets(Long idAssets);

    List<AmortizationTableReportDto> getDistinctAmortizationAccount(List<DepreciationAssetsDto> depricationAssetsDtos,
            Long fiscalYearId);

    List<DepreciationAssetsDto> getAllDepreciations(String user, String contentType,
            String authorization);

    List<DepreciationAssetsDto> filtredImmobilization(
            Collection<DepreciationAssetsDto> depricationAssetsDtos, FiscalYearDto fiscalYear);

    Map<Long, String> getAccountDepreciationAssets(List<Long> idsAssets);

    int generateProposedAmortizationAccount(int planCode);

    void saveDepreciationsAssetsByIdCategory(String user, String contentType, String authorization,
            Long idCategory);

    boolean deleteAllDepreciationAssets();

    List<Boolean> isConsommingDatesInFiscalYear(List<LocalDateTime> consomingDates);

    public Account getImmobilizationAccountByIdAssetOfDepreciation(Long idAssetOfDepreciation);

}
