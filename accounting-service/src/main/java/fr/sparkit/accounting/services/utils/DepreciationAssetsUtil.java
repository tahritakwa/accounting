package fr.sparkit.accounting.services.utils;

import static fr.sparkit.accounting.util.CalculationUtil.getFormattedBigDecimalValue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.dto.AmortizationTableDto;
import fr.sparkit.accounting.dto.AmortizationTableReportDto;
import fr.sparkit.accounting.dto.DepreciationAssetsDto;
import fr.sparkit.accounting.util.CalculationUtil;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.errors.ErrorsResponse;
import fr.sparkit.accounting.util.http.HttpCustomException;

public final class DepreciationAssetsUtil {

    private static final String TOTAL = "Total";

    private DepreciationAssetsUtil() {
        super();
    }

    public static void setImobilizationField(DepreciationAssetsDto depreciationAssetsDto,
            Map.Entry<String, Object> stringObjectEntry) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss",
                AccountingConstants.LANGUAGE);
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss",
                AccountingConstants.LANGUAGE);

        switch (stringObjectEntry.getKey()) {
        case "Id":
            checkFieldNull(stringObjectEntry.getValue(), AccountingConstants.ID_ACTIF_DOTNET,
                    depreciationAssetsDto.getAssetsLabel());
            depreciationAssetsDto.setIdAssets(Long.valueOf(stringObjectEntry.getValue().toString()));
            break;
        case "IdCategory":
            checkFieldNull(stringObjectEntry.getValue(), AccountingConstants.CATEGORY_ACTIF_DOTNET,
                    depreciationAssetsDto.getAssetsLabel());
            depreciationAssetsDto.setIdCategory(Long.parseLong(stringObjectEntry.getValue().toString()));
            break;
        case "Label":
            checkFieldNull(stringObjectEntry.getValue(), AccountingConstants.LABEL_ACTIF_DOTNET,
                    depreciationAssetsDto.getAssetsLabel());
            depreciationAssetsDto.setAssetsLabel(stringObjectEntry.getValue().toString());
            break;
        case "Value":
            checkFieldNull(stringObjectEntry.getValue(), AccountingConstants.BUYING_PRICE_ACTIF_DOTNET,
                    depreciationAssetsDto.getAssetsLabel());
            depreciationAssetsDto.setAssetsAmount(new BigDecimal(stringObjectEntry.getValue().toString()));
            break;
        case "ServiceDate":
            if (stringObjectEntry.getValue() != null) {
                LocalDateTime date = LocalDateTime.parse(stringObjectEntry.getValue().toString(), inputFormatter);
                depreciationAssetsDto
                .setDateOfCommissioning(LocalDateTime.parse(outputFormatter.format(date), outputFormatter));
            }
            break;
        default:
        }
    }

    public static void addAmortizationAssetsTotalLineToReport(
            Collection<AmortizationTableReportDto> amortizationTableReportDtos,
            BigDecimal totalCategoryAcquisitionValue, BigDecimal totalCategoryPreviousDepreciation,
            BigDecimal totalCategoryAnnuityExercise, BigDecimal totalCategoryVcn, String lineName) {
        amortizationTableReportDtos.add(new AmortizationTableReportDto(StringUtils.EMPTY, StringUtils.EMPTY,
                StringUtils.EMPTY, lineName, getFormattedBigDecimalValue(totalCategoryAcquisitionValue),
                getFormattedBigDecimalValue(totalCategoryPreviousDepreciation),
                getFormattedBigDecimalValue(totalCategoryAnnuityExercise),
                getFormattedBigDecimalValue(totalCategoryVcn)));
    }

    public static void addAmortizationAssetsLineToReport(
            Collection<AmortizationTableReportDto> amortizationTableReportDtos,
            DepreciationAssetsDto depricationAssetDto, AmortizationTableDto amortizationTableDto, Double rate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        amortizationTableReportDtos.add(new AmortizationTableReportDto(StringUtils.EMPTY,
                depricationAssetDto.getAssetsLabel(), CalculationUtil.getRateFormat().format(rate),
                depricationAssetDto.getDateOfCommissioning().toLocalDate().format(formatter),
                getFormattedBigDecimalValue(amortizationTableDto.getAcquisitionValue()),
                getFormattedBigDecimalValue(amortizationTableDto.getPreviousDepreciation()),
                getFormattedBigDecimalValue(amortizationTableDto.getAnnuityExercise()),
                getFormattedBigDecimalValue(amortizationTableDto.getVcn())));
    }

    public static void addTotalAmortizationLineToReport(BigDecimal totalAcquisitionValue,
            BigDecimal totalPreviousDepreciation, BigDecimal totalAnnuityExercise, BigDecimal totalVcn,
            Collection<AmortizationTableReportDto> amortizationTableReportDtos) {
        amortizationTableReportDtos.add(new AmortizationTableReportDto(StringUtils.EMPTY, StringUtils.EMPTY,
                StringUtils.EMPTY, TOTAL, getFormattedBigDecimalValue(totalAcquisitionValue),
                getFormattedBigDecimalValue(totalPreviousDepreciation),
                getFormattedBigDecimalValue(totalAnnuityExercise), getFormattedBigDecimalValue(totalVcn)));
    }

    public static void addAssetsLineToReport(Collection<AmortizationTableReportDto> amortizationTableReportDtos,
            String accountLabel) {
        amortizationTableReportDtos
        .add(new AmortizationTableReportDto(accountLabel, StringUtils.EMPTY, StringUtils.EMPTY,
                StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY));
    }

    public static void checkFieldNull(Object field, String fieldText, String labelAssets) {
        HashMap<String, String> errors = new HashMap<>();
        errors.put("field", fieldText.toUpperCase(AccountingConstants.LANGUAGE));
        errors.put("label", labelAssets);
        if (field == null) {
            throw new HttpCustomException(ApiErrors.Accounting.DEPRECIATION_ASSETS_FIELD_EMPTY,
                    new ErrorsResponse().error(errors));
        }
    }

}
