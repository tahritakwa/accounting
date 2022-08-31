package fr.sparkit.accounting.services.utils;

import static fr.sparkit.accounting.services.utils.AccountingServiceUtil.dateIsAfterOrEquals;
import static fr.sparkit.accounting.services.utils.AccountingServiceUtil.isDateBeforeOrEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import fr.sparkit.accounting.constants.NumberConstant;
import fr.sparkit.accounting.dto.DepreciationAssetsDto;
import fr.sparkit.accounting.dto.FiscalYearDto;
import fr.sparkit.accounting.enumuration.AmortizationAssetsType;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.http.HttpCustomException;

public final class AmortizationTableUtil {

    private static final int ONE = 1;

    private AmortizationTableUtil() {
        super();
    }

    public static Double calculateRateOfCommissioning(DepreciationAssetsDto depreciationAssetsDto,
            FiscalYearDto fiscalYear, AmortizationAssetsType amortizationAssetsType) {
        long nbreDaysOfCommissioning = 0;
        LocalDate specialDay = LocalDate.of(depreciationAssetsDto.getDateOfCommissioning().getYear(),
                NumberConstant.THREE, NumberConstant.ONE);
        switch (amortizationAssetsType.toString()) {
        case "COMMISIONING":
            nbreDaysOfCommissioning = depreciationAssetsDto.getDateOfCommissioning().toLocalDate()
                    .until(fiscalYear.getEndDate().toLocalDate(), ChronoUnit.DAYS) + ONE;
            break;
        case "CESSION":
            nbreDaysOfCommissioning = fiscalYear.getStartDate().toLocalDate()
                    .until(depreciationAssetsDto.getDateCession().toLocalDate(), ChronoUnit.DAYS);

            break;
        case "COMMISIONING_AND_CESSION":
            nbreDaysOfCommissioning = depreciationAssetsDto.getDateOfCommissioning().toLocalDate()
                    .until(depreciationAssetsDto.getDateCession().toLocalDate(), ChronoUnit.DAYS);
            break;
        case "END_COMMISSIONING":
            nbreDaysOfCommissioning = fiscalYear.getStartDate().toLocalDate().until(depreciationAssetsDto
                    .getDateOfCommissioning().plusYears(depreciationAssetsDto.getNbreOfYears()).toLocalDate(),
                    ChronoUnit.DAYS);

            break;
        default:
        }
        if (checkLeapYear(fiscalYear.getStartDate().getYear())
                && (depreciationAssetsDto.getDateOfCommissioning().toLocalDate().isAfter(specialDay)
                        || depreciationAssetsDto.getDateOfCommissioning().toLocalDate().isEqual(specialDay))) {
            nbreDaysOfCommissioning--;
        }
        return nbreDaysOfCommissioning / Double.valueOf(NumberConstant.NBRE_DAY_OF_YEAR);
    }

    private static boolean checkLeapYear(int year) {
        boolean leap = false;
        if (year % NumberConstant.FOUR == NumberConstant.ZERO) {
            if (year % NumberConstant.ONE_HUNDRAND == NumberConstant.ZERO) {
                if (year % NumberConstant.FOUR_HUNDRAND == NumberConstant.ZERO) {
                    leap = true;
                } else {
                    leap = false;
                }
            } else {
                leap = true;
            }
        }

        return leap;
    }

    public static void checkDateCessionAfterCommissioning(DepreciationAssetsDto depreciationAssetsDto) {
        if (depreciationAssetsDto.getDateCession().isBefore(depreciationAssetsDto.getDateOfCommissioning())) {
            throw new HttpCustomException(ApiErrors.Accounting.DATE_CESSION_AFTER_DATE_COMMISSIONING);
        }
    }

    public static boolean checkDateOfCessionInFiscalYear(LocalDateTime cessionDate, FiscalYearDto fiscalYear) {
        checkDateCessionNull(cessionDate);
        return dateIsAfterOrEquals(cessionDate, fiscalYear.getStartDate())
                && isDateBeforeOrEquals(cessionDate, fiscalYear.getEndDate());
    }

    private static void checkDateCessionNull(LocalDateTime cessionDate) {
        if (cessionDate == null) {
            throw new HttpCustomException(ApiErrors.Accounting.DATE_CESSION_NULL);
        }
    }

    public static boolean checkDateOfCommisioningInFiscalYear(LocalDateTime dateOfCommissioning,
            FiscalYearDto fiscalYear) {
        return (dateOfCommissioning.isAfter(fiscalYear.getStartDate())
                || dateOfCommissioning.isEqual(fiscalYear.getStartDate()))
                && (dateOfCommissioning.isBefore(fiscalYear.getEndDate())
                        || dateOfCommissioning.isEqual(fiscalYear.getEndDate()));
    }

    public static void checkNullCategory(Long idCategory) {
        if (idCategory == null) {
            throw new HttpCustomException(ApiErrors.Accounting.ACCOUNTING_CONFIGURATION_CATEGORY_NOT_FOUND);
        }

    }

    public static void checkDateOfCommissioningNull(DepreciationAssetsDto depreciationAssetsDto) {
        DepreciationAssetsUtil.checkFieldNull(depreciationAssetsDto.getDateOfCommissioning(), "ServiceDate",
                depreciationAssetsDto.getAssetsLabel());
    }
}
