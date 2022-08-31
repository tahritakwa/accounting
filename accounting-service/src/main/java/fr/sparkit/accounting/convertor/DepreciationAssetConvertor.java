package fr.sparkit.accounting.convertor;

import fr.sparkit.accounting.dto.DepreciationAssetsDto;
import fr.sparkit.accounting.entities.Account;
import fr.sparkit.accounting.entities.DepreciationAssets;

public final class DepreciationAssetConvertor {

    private DepreciationAssetConvertor() {
        super();
    }

    public static DepreciationAssets dtoToModel(DepreciationAssetsDto depreciationAssetsDto,
            Account amortizationAccount, Account immobilizationAccount) {
        return new DepreciationAssets(depreciationAssetsDto.getId(), depreciationAssetsDto.getIdAssets(),
                immobilizationAccount, amortizationAccount, depreciationAssetsDto.getCession(),
                depreciationAssetsDto.getDateCession(), depreciationAssetsDto.getAmountCession(), false, null);
    }

}
