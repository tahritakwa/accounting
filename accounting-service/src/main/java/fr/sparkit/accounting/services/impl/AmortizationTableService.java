package fr.sparkit.accounting.services.impl;

import static fr.sparkit.accounting.constants.AccountingConstants.ASSET_WITH_COMMISSION_DATE_NOT_IN_FISCAL_YEAR;
import static fr.sparkit.accounting.constants.AccountingConstants.ASSIGN_A_DEPRECIATION_PERIOD_TO_THIS_CATEGORY;
import static fr.sparkit.accounting.constants.AccountingConstants.CESSION_DATE_OUT_OF_SERVICE;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import fr.sparkit.accounting.constants.NumberConstant;
import fr.sparkit.accounting.convertor.AmortizationTableConvertor;
import fr.sparkit.accounting.convertor.FiscalYearConvertor;
import fr.sparkit.accounting.dao.AmortizationTableDao;
import fr.sparkit.accounting.dto.AmortizationTableDto;
import fr.sparkit.accounting.dto.DepreciationAssetsDto;
import fr.sparkit.accounting.dto.FiscalYearDto;
import fr.sparkit.accounting.entities.AmortizationTable;
import fr.sparkit.accounting.entities.DepreciationAssetsConfiguration;
import fr.sparkit.accounting.entities.FiscalYear;
import fr.sparkit.accounting.enumuration.AmortizationAssetsType;
import fr.sparkit.accounting.services.IAccountingConfigurationService;
import fr.sparkit.accounting.services.IAmortizationtableService;
import fr.sparkit.accounting.services.IDepreciationAssetConfigurationService;
import fr.sparkit.accounting.services.IDepreciationAssetService;
import fr.sparkit.accounting.services.IFiscalYearService;
import fr.sparkit.accounting.services.utils.AmortizationTableUtil;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.errors.ErrorsResponse;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AmortizationTableService extends GenericService<AmortizationTable, Long>
        implements IAmortizationtableService {

    private static final int ONE = 1;

    private final IFiscalYearService fiscalYearService;
    private final IAccountingConfigurationService accountingConfigurationService;
    private final IDepreciationAssetConfigurationService depreciationAssetConfigurationService;
    private final IDepreciationAssetService depreciationAssetService;
    private final AmortizationTableDao amortizationTableDao;

    @Autowired
    public AmortizationTableService(AmortizationTableDao amortizationTableDao, IFiscalYearService fiscalYearService,
            IAccountingConfigurationService accountingConfigurationService,
            IDepreciationAssetConfigurationService depreciationAssetConfigurationService,
            @Lazy IDepreciationAssetService depreciationAssetService) {
        super();
        this.amortizationTableDao = amortizationTableDao;
        this.fiscalYearService = fiscalYearService;
        this.accountingConfigurationService = accountingConfigurationService;
        this.depreciationAssetConfigurationService = depreciationAssetConfigurationService;
        this.depreciationAssetService = depreciationAssetService;
    }

    @Override
    public AmortizationTableDto calculateAmortization(DepreciationAssetsDto depreciationAssetsDto) {
        FiscalYearDto fiscalYear = accountingConfigurationService.getCurrentFiscalYear();
        AmortizationTableUtil.checkDateOfCommissioningNull(depreciationAssetsDto);
        if (depreciationAssetsDto.getDateOfCommissioning().isAfter(fiscalYear.getEndDate())
                || depreciationAssetsDto.getDateOfCommissioning().plusYears(depreciationAssetsDto.getNbreOfYears())
                        .isBefore(fiscalYear.getStartDate())) {
            log.error(ASSET_WITH_COMMISSION_DATE_NOT_IN_FISCAL_YEAR, depreciationAssetsDto.getDateOfCommissioning(),
                    fiscalYear.getName());
            throw new HttpCustomException(ApiErrors.Accounting.ASSETS_OUT_OF_SERVICE,
                    new ErrorsResponse().error(depreciationAssetsDto.getAssetsLabel()));
        } else {
            AmortizationTableUtil.checkNullCategory(depreciationAssetsDto.getIdCategory());
            checkCessionOutOfService(depreciationAssetsDto);
            return getDepreciationOfAsset(depreciationAssetsDto, fiscalYear, depreciationAssetsDto.getIdCategory());
        }
    }

    private void checkCessionOutOfService(DepreciationAssetsDto depreciationAssetsDto) {
        if (depreciationAssetsDto.getCession()) {
            LocalDate maxDateCession = depreciationAssetsDto.getDateOfCommissioning()
                    .plusYears(depreciationAssetsDto.getNbreOfYears()).toLocalDate().minusDays(1);
            if ((depreciationAssetsDto.getDateCession().toLocalDate().compareTo(maxDateCession)) > 0) {
                log.error(CESSION_DATE_OUT_OF_SERVICE);
                throw new HttpCustomException(ApiErrors.Accounting.DATE_CESSION_OUT_OF_SERVICE);
            }

        }
    }

    @Override
    public AmortizationTableDto getDepreciationOfAsset(DepreciationAssetsDto depreciationAssetsDto,
            FiscalYearDto fiscalYear, Long idCategory) {
        double rateOfCommissioning = ONE;
        BigDecimal previousDepreciation = BigDecimal.ZERO;
        DepreciationAssetsConfiguration depreciationAssetsConfiguration = depreciationAssetConfigurationService
                .findByIdCategory(idCategory);
        checkDepreciationAssetsConfigurationNull(depreciationAssetsConfiguration);
        Optional<AmortizationTable> amortizationTableOpt = findAmortizationTable(fiscalYear.getId(),
                depreciationAssetsDto.getId());
        if (amortizationTableOpt.isPresent()) {
            return AmortizationTableConvertor.modelToDto(amortizationTableOpt.get());
        } else {
            return calculateAmortizationOfCurrentFiscalYear(depreciationAssetsDto, previousDepreciation, fiscalYear,
                    depreciationAssetsConfiguration.getDepreciationPeriod(), rateOfCommissioning);
        }

    }

    public void checkDepreciationAssetsConfigurationNull(
            DepreciationAssetsConfiguration depreciationAssetsConfiguration) {
        if (depreciationAssetsConfiguration == null) {
            log.error(ASSIGN_A_DEPRECIATION_PERIOD_TO_THIS_CATEGORY);
            throw new HttpCustomException(ApiErrors.Accounting.ACCOUNTING_CONFIGURATION_DEPRECITION_PERIOD_UNAFFECTED);
        }
    }

    private AmortizationTableDto calculateAmortizationOfCurrentFiscalYear(DepreciationAssetsDto depreciationAssetsDto,
            BigDecimal previousDepreciation, FiscalYearDto fiscalYear, int depreciationPeriod,
            double rateOfCommissioning) {
        boolean check = true;
        BigDecimal annuityExercise;
        BigDecimal vcn;
        annuityExercise = depreciationAssetsDto.getAssetsAmount().divide(new BigDecimal(depreciationPeriod),
                BigDecimal.ROUND_FLOOR, RoundingMode.HALF_UP);
        // purchase and sale of the asset in the same fiscal year
        if (AmortizationTableUtil.checkDateOfCommisioningInFiscalYear(depreciationAssetsDto.getDateOfCommissioning(),
                fiscalYear) && depreciationAssetsDto.getCession()
                && AmortizationTableUtil.checkDateOfCessionInFiscalYear(depreciationAssetsDto.getDateCession(),
                        fiscalYear)) {
            rateOfCommissioning = AmortizationTableUtil.calculateRateOfCommissioning(depreciationAssetsDto, fiscalYear,
                    AmortizationAssetsType.COMMISIONING_AND_CESSION);
            check = false;
            // purchase the asset in the fiscal year
        } else if (AmortizationTableUtil
                .checkDateOfCommisioningInFiscalYear(depreciationAssetsDto.getDateOfCommissioning(), fiscalYear)) {
            rateOfCommissioning = AmortizationTableUtil.calculateRateOfCommissioning(depreciationAssetsDto, fiscalYear,
                    AmortizationAssetsType.COMMISIONING);
            check = false;
            // sale the asset in the fiscal year
        } else if (depreciationAssetsDto.getCession() && AmortizationTableUtil
                .checkDateOfCessionInFiscalYear(depreciationAssetsDto.getDateCession(), fiscalYear)) {
            rateOfCommissioning = AmortizationTableUtil.calculateRateOfCommissioning(depreciationAssetsDto, fiscalYear,
                    AmortizationAssetsType.CESSION);
        } else if (depreciationAssetsDto.getDateOfCommissioning().plusYears(depreciationPeriod)
                .isBefore(fiscalYear.getEndDate())) {
            rateOfCommissioning = AmortizationTableUtil.calculateRateOfCommissioning(depreciationAssetsDto, fiscalYear,
                    AmortizationAssetsType.END_COMMISSIONING);
        }
        annuityExercise = annuityExercise.multiply(BigDecimal.valueOf(rateOfCommissioning));
        Optional<FiscalYear> previousFiscalYear = fiscalYearService.findPreviousFiscalYear(fiscalYear.getId());
        if (check) {
            depreciationAssetsDto
                    .setId(depreciationAssetService.findByIdAssets(depreciationAssetsDto.getIdAssets()).getId());
            if (previousFiscalYear.isPresent()) {
                previousDepreciation = calculatePreviousDepreciation(depreciationAssetsDto,
                        FiscalYearConvertor.modelToDto(previousFiscalYear.get()), depreciationPeriod, fiscalYear);
            } else {
                previousDepreciation = calculatePreviousDepreciationOfAssetNotBelongsToFiscalYear(depreciationAssetsDto,
                        depreciationPeriod, fiscalYear);
            }
        }
        vcn = depreciationAssetsDto.getAssetsAmount().subtract(previousDepreciation).subtract(annuityExercise);
        if (vcn.compareTo(BigDecimal.ZERO) < NumberConstant.ZERO){
            vcn = BigDecimal.ZERO;
        }
        return new AmortizationTableDto(depreciationAssetsDto.getIdAssets(), fiscalYear.getId(),
                depreciationAssetsDto.getAssetsAmount().setScale(BigDecimal.ROUND_FLOOR, RoundingMode.HALF_UP),
                previousDepreciation.setScale(BigDecimal.ROUND_FLOOR, RoundingMode.HALF_UP),
                annuityExercise.setScale(BigDecimal.ROUND_FLOOR, RoundingMode.HALF_UP),
                vcn.setScale(BigDecimal.ROUND_FLOOR, RoundingMode.HALF_UP));
    }

    private BigDecimal calculatePreviousDepreciation(DepreciationAssetsDto depreciationAssetsDto,
            FiscalYearDto previousFiscalYear, int depreciationPeriod, FiscalYearDto fiscalYear) {
        Optional<AmortizationTable> amortizationTableOfPreviousFiscalYear = findAmortizationTable(
                previousFiscalYear.getId(), depreciationAssetsDto.getId());
        BigDecimal previousDeprication;
        if (amortizationTableOfPreviousFiscalYear.isPresent()) {
            previousDeprication = amortizationTableOfPreviousFiscalYear.get().getPreviousDepreciation();
            return amortizationTableOfPreviousFiscalYear.get().getAnnuityExercise()
                    .add(previousDeprication.setScale(BigDecimal.ROUND_FLOOR, BigDecimal.ROUND_HALF_UP));
        } else {
            previousDeprication = calculatePreviousDepreciationOfAssetNotBelongsToFiscalYear(depreciationAssetsDto,
                    depreciationPeriod, fiscalYear);
            return previousDeprication.setScale(BigDecimal.ROUND_FLOOR, BigDecimal.ROUND_HALF_UP);
        }
    }

    private BigDecimal calculatePreviousDepreciationOfAssetNotBelongsToFiscalYear(
            DepreciationAssetsDto depreciationAssetsDto, int depreciationPeriod, FiscalYearDto fiscalYearDto) {
        long numberOfCommissionedDays;
        LocalDateTime startDateOfFisrtFiscalYear = fiscalYearService.getStartDateOfFirstFiscalYear();
        if (fiscalYearDto.getStartDate().equals(startDateOfFisrtFiscalYear)) {
            numberOfCommissionedDays = depreciationAssetsDto.getDateOfCommissioning().until(startDateOfFisrtFiscalYear,
                    ChronoUnit.DAYS);
        } else {
            numberOfCommissionedDays = depreciationAssetsDto.getDateOfCommissioning()
                    .until(fiscalYearDto.getStartDate(), ChronoUnit.DAYS);
        }
        double rateOfCommissioning = (double) numberOfCommissionedDays
                / (depreciationPeriod * NumberConstant.NBRE_DAY_OF_YEAR);
        return depreciationAssetsDto.getAssetsAmount().multiply(BigDecimal.valueOf(rateOfCommissioning));
    }

    @Override
    public Optional<AmortizationTable> findAmortizationTable(Long fiscalYearId, Long assetsId) {
        return amortizationTableDao.findByFiscalYearIdAndAssetsIdAndIsDeletedFalse(fiscalYearId, assetsId);
    }

    @Override
    public List<AmortizationTable> findByFiscalYear(Long fiscalYearId) {
        return amortizationTableDao.findByFiscalYearIdAndIsDeletedFalse(fiscalYearId);
    }

}
