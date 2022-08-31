package fr.sparkit.accounting.services.impl;

import static fr.sparkit.accounting.constants.AccountingConstants.SUB_TOTAL_PARAM;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.constants.NumberConstant;
import fr.sparkit.accounting.dao.DepreciationAssetsConfigurationDao;
import fr.sparkit.accounting.dao.DepreciationAssetsDao;
import fr.sparkit.accounting.dao.DocumentAccountDao;
import fr.sparkit.accounting.dto.AmortizationTableDto;
import fr.sparkit.accounting.dto.AmortizationTableReportDto;
import fr.sparkit.accounting.dto.DepreciationAssetsDto;
import fr.sparkit.accounting.dto.FiscalYearDto;
import fr.sparkit.accounting.entities.Account;
import fr.sparkit.accounting.entities.DepreciationAssets;
import fr.sparkit.accounting.entities.DepreciationAssetsConfiguration;
import fr.sparkit.accounting.services.IAccountService;
import fr.sparkit.accounting.services.IAmortizationtableService;
import fr.sparkit.accounting.services.IDepreciationAssetConfigurationService;
import fr.sparkit.accounting.services.IDepreciationAssetService;
import fr.sparkit.accounting.services.IFiscalYearService;
import fr.sparkit.accounting.services.utils.AmortizationTableUtil;
import fr.sparkit.accounting.services.utils.DepreciationAssetsUtil;
import fr.sparkit.accounting.services.utils.TraductionServiceUtil;
import fr.sparkit.accounting.util.CompanyContextHolder;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DepreciationAssetService extends GenericService<DepreciationAssets, Long>
        implements IDepreciationAssetService {

    private final IAccountService accountService;
    private final IFiscalYearService fiscalYearService;
    private final IAmortizationtableService amortizationTableService;
    private final DepreciationAssetsDao depreciationAssetsDao;
    private final DepreciationAssetsConfigurationDao depreciationAssetsConfigurationDao;
    private final IDepreciationAssetConfigurationService depreciationAssetConfigurationService;
    private final DocumentAccountDao documentAccountDao;

    @Value("${dotnet.url}")
    private String dotnetRessource;

    @Autowired
    public DepreciationAssetService(IAccountService accountService, IFiscalYearService fiscalYearService,
            IAmortizationtableService amortizationTableService, DepreciationAssetsDao depreciationAssetsDao,
            DepreciationAssetsConfigurationDao depreciationAssetsConfigurationDao,
            IDepreciationAssetConfigurationService depreciationAssetConfigurationService,
            DocumentAccountDao documentAccountDao) {
        super();
        this.accountService = accountService;
        this.depreciationAssetsDao = depreciationAssetsDao;
        this.depreciationAssetsConfigurationDao = depreciationAssetsConfigurationDao;
        this.fiscalYearService = fiscalYearService;
        this.amortizationTableService = amortizationTableService;
        this.depreciationAssetConfigurationService = depreciationAssetConfigurationService;
        this.documentAccountDao = documentAccountDao;
    }

    @Override
    public DepreciationAssets saveOrUpdateDepreciationAsset(DepreciationAssetsDto depreciationAssetsDto) {
        Account accountAmortization;
        Account accountImmobilization;
        if (depreciationAssetsDto.getIdAmortizationAccount() == null
                || depreciationAssetsDto.getIdImmobilizationAccount() == null) {
            DepreciationAssetsConfiguration depreciationAssetsResult = depreciationAssetConfigurationService
                    .findByIdCategory(depreciationAssetsDto.getIdCategory());
            accountAmortization = depreciationAssetsResult.getAmortizationAccount();
            accountImmobilization = depreciationAssetsResult.getImmobilizationAccount();
        } else {
            accountAmortization = accountService.findOne(depreciationAssetsDto.getIdAmortizationAccount());
            accountImmobilization = accountService.findOne(depreciationAssetsDto.getIdImmobilizationAccount());
        }
        Optional<DepreciationAssets> depreciationAssetsOpt = depreciationAssetsDao
                .findByIdAssetsAndIsDeletedFalse(depreciationAssetsDto.getIdAssets());
        if (depreciationAssetsOpt.isPresent()) {
            DepreciationAssets depreciationAssets = depreciationAssetsOpt.get();
            depreciationAssets.setAmortizationAccount(accountAmortization);
            depreciationAssets.setImmobilizationAccount(accountImmobilization);
            depreciationAssets.setCession(depreciationAssetsDto.getCession());
            depreciationAssets.setDateCession(depreciationAssetsDto.getDateCession());
            depreciationAssets.setAmountCession(depreciationAssetsDto.getAmountCession());
            DepreciationAssets saveDepreciationAsset = saveAndFlush(depreciationAssets);
            log.info(AccountingConstants.LOG_ENTITY_UPDATED, saveDepreciationAsset);
            return saveDepreciationAsset;
        } else {
            AmortizationTableUtil.checkDateOfCommissioningNull(depreciationAssetsDto);
            DepreciationAssets newDepreciationAsset = saveAndFlush(
                    new DepreciationAssets(depreciationAssetsDto.getIdAssets(), accountImmobilization,
                            accountAmortization, depreciationAssetsDto.getCession(),
                            depreciationAssetsDto.getDateCession(), depreciationAssetsDto.getAmountCession()));
            log.info(AccountingConstants.LOG_ENTITY_CREATED, newDepreciationAsset);
            return newDepreciationAsset;
        }
    }

    @Override
    public DepreciationAssets findByIdAssets(Long idAssets) {
        Optional<DepreciationAssets> depreciationAssetsOpt = depreciationAssetsDao
                .findByIdAssetsAndIsDeletedFalse(idAssets);
        if (depreciationAssetsOpt.isPresent()) {
            return depreciationAssetsOpt.get();
        } else {
            log.error(AccountingConstants.ASSET_ACCOUNT_TO_ASSET);
            throw new HttpCustomException(ApiErrors.Accounting.REPORT_LINE_ASSETS_NOT_FOUND);
        }
    }

    @Override
    public DepreciationAssets deleteByIdAssets(Long idAssets) {
        DepreciationAssets depreciationAssets = findByIdAssets(idAssets);
        log.info(AccountingConstants.LOG_ENTITY_DELETED, AccountingConstants.ENTITY_NAME_DEPRECIATION_ASSETS, idAssets);
        delete(depreciationAssets);
        return depreciationAssets;
    }

    @Override
    public List<AmortizationTableReportDto> getDistinctAmortizationAccount(
            List<DepreciationAssetsDto> depricationAssetsDtos, Long fiscalYearId) {
        BigDecimal totalAcquisitionValue = BigDecimal.ZERO;
        BigDecimal totalPreviousDepreciation = BigDecimal.ZERO;
        BigDecimal totalAnnuityExercise = BigDecimal.ZERO;
        BigDecimal totalVcn = BigDecimal.ZERO;
        FiscalYearDto fiscalYear = fiscalYearService.findById(fiscalYearId);
        List<AmortizationTableReportDto> amortizationTableReportDtos = new ArrayList<>();

        List<DepreciationAssetsDto> depricationAssetsList = filtredImmobilization(depricationAssetsDtos, fiscalYear);

        List<DepreciationAssetsDto> corporelleDepricationAssets = depricationAssetsList.stream()
                .filter((DepreciationAssetsDto depreciationAssetsDto) -> depreciationAssetsConfigurationDao
                        .findByIdCategoryAndIsDeletedIsFalse(depreciationAssetsDto.getIdCategory()).get()
                        .getImmobilizationType().equals(AccountingConstants.CORPORELLE))
                .collect(Collectors.toList());
        List<DepreciationAssetsDto> incorporelleDepricationAssets = depricationAssetsList.stream()
                .filter((DepreciationAssetsDto depreciationAssetsDto) -> depreciationAssetsConfigurationDao
                        .findByIdCategoryAndIsDeletedIsFalse(depreciationAssetsDto.getIdCategory()).get()
                        .getImmobilizationType().equals(AccountingConstants.INCORPORELLE))
                .collect(Collectors.toList());
        Map<String, List<DepreciationAssetsDto>> depricationAssetsMap = new HashMap<>();
        depricationAssetsMap.put(AccountingConstants.CORPORELLE, corporelleDepricationAssets);
        depricationAssetsMap.put(AccountingConstants.INCORPORELLE, incorporelleDepricationAssets);
        for (Map.Entry<String, List<DepreciationAssetsDto>> depricationAssetList : depricationAssetsMap.entrySet()) {
            BigDecimal sousTotalCategoryAcquisitionValue = BigDecimal.ZERO;
            BigDecimal sousTotalCategoryPreviousDepreciation = BigDecimal.ZERO;
            BigDecimal sousTotalCategoryAnnuityExercise = BigDecimal.ZERO;
            BigDecimal sousTotalCategoryVcn = BigDecimal.ZERO;
            Map<Long, List<DepreciationAssetsDto>> depricationAssetsGroupedByAccountMap = depricationAssetList
                    .getValue().stream()
                    .collect(Collectors.groupingBy(DepreciationAssetsDto::getIdImmobilizationAccount));
            DepreciationAssetsUtil.addAssetsLineToReport(amortizationTableReportDtos, depricationAssetList.getKey());
            for (Map.Entry<Long, List<DepreciationAssetsDto>> depricationAssets : depricationAssetsGroupedByAccountMap
                    .entrySet()) {
                BigDecimal totalCategoryAcquisitionValue = BigDecimal.ZERO;
                BigDecimal totalCategoryPreviousDepreciation = BigDecimal.ZERO;
                BigDecimal totalCategoryAnnuityExercise = BigDecimal.ZERO;
                BigDecimal totalCategoryVcn = BigDecimal.ZERO;
                String accountLabel = accountService.findById(depricationAssets.getKey()).getLabel();
                DepreciationAssetsUtil.addAssetsLineToReport(amortizationTableReportDtos, accountLabel);
                for (DepreciationAssetsDto depricationAssetDto : depricationAssets.getValue()) {
                    AmortizationTableDto amortizationTableDto;
                    amortizationTableDto = amortizationTableService.getDepreciationOfAsset(depricationAssetDto,
                            fiscalYear, depricationAssetDto.getIdCategory());
                    Optional<DepreciationAssetsConfiguration> depreciationAssetsConfiguration = depreciationAssetsConfigurationDao
                            .findByIdCategoryAndIsDeletedIsFalse(depricationAssetDto.getIdCategory());
                    if (depreciationAssetsConfiguration.isPresent()) {
                        DepreciationAssetsUtil.addAmortizationAssetsLineToReport(amortizationTableReportDtos,
                                depricationAssetDto, amortizationTableDto, (double) NumberConstant.ONE_HUNDRAND
                                        / depreciationAssetsConfiguration.get().getDepreciationPeriod());
                    }
                    totalCategoryAcquisitionValue = totalCategoryAcquisitionValue
                            .add(amortizationTableDto.getAcquisitionValue());
                    totalCategoryPreviousDepreciation = totalCategoryPreviousDepreciation
                            .add(amortizationTableDto.getPreviousDepreciation());
                    totalCategoryAnnuityExercise = totalCategoryAnnuityExercise
                            .add(amortizationTableDto.getAnnuityExercise());
                    totalCategoryVcn = totalCategoryVcn.add(amortizationTableDto.getVcn());
                }
                DepreciationAssetsUtil.addAmortizationAssetsTotalLineToReport(amortizationTableReportDtos,
                        totalCategoryAcquisitionValue, totalCategoryPreviousDepreciation, totalCategoryAnnuityExercise,
                        totalCategoryVcn, TraductionServiceUtil.getI18nResourceBundle().getString(SUB_TOTAL_PARAM));
                sousTotalCategoryAcquisitionValue = sousTotalCategoryAcquisitionValue
                        .add(totalCategoryAcquisitionValue);
                sousTotalCategoryPreviousDepreciation = sousTotalCategoryPreviousDepreciation
                        .add(totalCategoryPreviousDepreciation);
                sousTotalCategoryAnnuityExercise = sousTotalCategoryAnnuityExercise.add(totalCategoryAnnuityExercise);
                sousTotalCategoryVcn = sousTotalCategoryVcn.add(totalCategoryVcn);
                totalAcquisitionValue = totalAcquisitionValue.add(totalCategoryAcquisitionValue);
                totalPreviousDepreciation = totalPreviousDepreciation.add(totalCategoryPreviousDepreciation);
                totalAnnuityExercise = totalAnnuityExercise.add(totalCategoryAnnuityExercise);
                totalVcn = totalVcn.add(totalCategoryVcn);
            }
            DepreciationAssetsUtil.addAmortizationAssetsTotalLineToReport(amortizationTableReportDtos,
                    sousTotalCategoryAcquisitionValue, sousTotalCategoryPreviousDepreciation,
                    sousTotalCategoryAnnuityExercise, sousTotalCategoryVcn,
                    TraductionServiceUtil.getI18nResourceBundle().getString(SUB_TOTAL_PARAM) + " "
                            + depricationAssetList.getKey());
        }
        DepreciationAssetsUtil.addTotalAmortizationLineToReport(totalAcquisitionValue, totalPreviousDepreciation,
                totalAnnuityExercise, totalVcn, amortizationTableReportDtos);
        return amortizationTableReportDtos;
    }

    @Override
    public List<DepreciationAssetsDto> filtredImmobilization(Collection<DepreciationAssetsDto> depricationAssetsDtos,
            FiscalYearDto fiscalYear) {
        return depricationAssetsDtos.stream().filter((DepreciationAssetsDto depricationAssetsDto) -> {
            Optional<DepreciationAssets> depreciationAssets = depreciationAssetsDao
                    .findByIdAssetsAndIsDeletedFalse(depricationAssetsDto.getIdAssets());
            return (depreciationAssets.isPresent() && depricationAssetsDto.getDateOfCommissioning() != null
                    && !(depricationAssetsDto.getDateOfCommissioning().isAfter(fiscalYear.getEndDate())
                            || depricationAssetsDto.getDateOfCommissioning()
                                    .plusYears(depreciationAssetsConfigurationDao
                                            .findByIdCategoryAndIsDeletedIsFalse(depricationAssetsDto.getIdCategory())
                                            .get().getDepreciationPeriod())
                                    .isBefore(fiscalYear.getStartDate())));
        }).map((DepreciationAssetsDto depreciation) -> {
            Optional<DepreciationAssetsConfiguration> depreciationConfiguration = depreciationAssetsConfigurationDao
                    .findByIdCategoryAndIsDeletedIsFalse(depreciation.getIdCategory());

            long depreciationPeriod = depreciationConfiguration.get().getDepreciationPeriod();
            DepreciationAssets depreciationAssets = depreciationAssetsDao
                    .findByIdAssetsAndIsDeletedFalse(depreciation.getIdAssets()).get();

            return new DepreciationAssetsDto(depreciation.getId(), depreciation.getIdAssets(),
                    depreciation.getIdCategory(), depreciation.getAssetsAmount(), depreciation.getAssetsLabel(),
                    getImmobilizationAccountByIdAssetOfDepreciation(depreciation.getIdAssets()).getId(),
                    getAmortizationAccount(depreciation).getId(), depreciationPeriod,
                    depreciation.getDateOfCommissioning(), depreciationAssets.getCession(),
                    depreciationAssets.getDateCession(), depreciationAssets.getAmountCession());
        }).filter((DepreciationAssetsDto depreciationAssetsDto) -> (!(depreciationAssetsDto.getCession()
                && depreciationAssetsDto.getDateCession().isBefore(fiscalYear.getStartDate()))))
                .collect(Collectors.toList());
    }

    @Override
    public Map<Long, String> getAccountDepreciationAssets(List<Long> idsAssets) {
        Map<Long, String> map = new HashMap<>();
        idsAssets.forEach((Long id) -> {
            Optional<DepreciationAssets> depreciationAssetsOpt = depreciationAssetsDao
                    .findByIdAssetsAndIsDeletedFalse(id);
            if (depreciationAssetsOpt.isPresent()) {
                map.put(id, depreciationAssetsOpt.get().getImmobilizationAccount().getLabel());
            }
        });
        return map;
    }

    @Override
    public int generateProposedAmortizationAccount(int planCode) {
        StringBuilder sb = new StringBuilder(String.valueOf(planCode));
        sb.insert(NumberConstant.ONE, NumberConstant.EIGHT);
        int zerosLength = NumberConstant.EIGHT - sb.toString().length();
        String zeros = StringUtils.repeat("0", zerosLength);
        return Integer.parseInt(sb.toString().concat(zeros));
    }

    @Override
    public void saveDepreciationsAssetsByIdCategory(String user, String contentType,
            String authorization, Long idCatgory) {
        AmortizationTableUtil.checkNullCategory(idCatgory);
        List<DepreciationAssetsDto> allDepreciationAssets = getAllDepreciations(user, contentType,
                authorization);
        List<DepreciationAssetsDto> depreciationAssetsFiltred = null;
        depreciationAssetsFiltred = allDepreciationAssets.stream()
                .filter((DepreciationAssetsDto depricationAssetsDto) -> {
                    Optional<DepreciationAssets> depreciationAssetsOpt = depreciationAssetsDao
                            .findByIdAssetsAndIsDeletedFalse(depricationAssetsDto.getIdAssets());
                    return !depreciationAssetsOpt.isPresent() && depricationAssetsDto.getDateOfCommissioning() != null
                            && depricationAssetsDto.getIdCategory().equals(idCatgory);
                }).collect(Collectors.toList());
        depreciationAssetsFiltred.forEach((DepreciationAssetsDto depricationAssetsDto) -> {
            depricationAssetsDto.setCession(Boolean.FALSE);
            saveOrUpdateDepreciationAsset(depricationAssetsDto);
        });
    }

    public Account getAmortizationAccount(DepreciationAssetsDto deprication) {
        Optional<DepreciationAssets> depreciationAssetsOpt = depreciationAssetsDao
                .findByIdAssetsAndIsDeletedFalse(deprication.getIdAssets());
        if (depreciationAssetsOpt.isPresent()) {
            return depreciationAssetsOpt.get().getAmortizationAccount();
        } else {
            throw new HttpCustomException(ApiErrors.Accounting.DEPRECIATION_ASSETS_NOT_ACCOUNTED);
        }
    }

    @Override
    public Account getImmobilizationAccountByIdAssetOfDepreciation(Long idAssetOfDepreciation) {
        Optional<DepreciationAssets> depreciationAssetsOpt = depreciationAssetsDao
                .findByIdAssetsAndIsDeletedFalse(idAssetOfDepreciation);
        if (depreciationAssetsOpt.isPresent()) {
            return depreciationAssetsOpt.get().getImmobilizationAccount();
        } else {
            log.error(AccountingConstants.ASSET_ACCOUNT_BEFORE_CALCULATE);
            throw new HttpCustomException(ApiErrors.Accounting.DEPRECIATION_ASSETS_NOT_ACCOUNTED);
        }
    }

    @Override
    public List<DepreciationAssetsDto> getAllDepreciations(String user, String contentType,
            String authorization) {
        RestTemplate restTemplate = new RestTemplate();
        String fooResourceUrl = dotnetRessource.concat(AccountingConstants.GET_IMOBILIZATION_URL_DOTNET);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Module", "Immobilisation");
        headers.set("TModel", "Active");
        headers.set("User", user);
        headers.set("Content-Type", contentType);
        headers.set("Authorization", authorization);
        headers.set("CompanyName", CompanyContextHolder.getCompanyContext());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        List<DepreciationAssetsDto> depreciationAssetsDtos = new ArrayList<>();
        try {
            List<LinkedHashMap<String, Object>> result = restTemplate
                    .exchange(fooResourceUrl, HttpMethod.GET, entity, List.class).getBody();

            if (result != null) {
                for (LinkedHashMap<String, Object> objectLinkedHashMap : result) {
                    DepreciationAssetsDto depreciationAssetsDto = new DepreciationAssetsDto();
                    objectLinkedHashMap.entrySet().forEach(stringObjectEntry -> DepreciationAssetsUtil
                            .setImobilizationField(depreciationAssetsDto, stringObjectEntry));
                    depreciationAssetsDtos.add(depreciationAssetsDto);
                }
            }
        } catch (RestClientException e) {
            log.error(AccountingConstants.CANNOT_CONNECT_TO_DOT_NET, e);
            throw new HttpCustomException(ApiErrors.Accounting.RESOURCE_NOT_FOUND);
        }
        return depreciationAssetsDtos;
    }

    @Override
    public boolean deleteAllDepreciationAssets() {
        List<DepreciationAssets> depreciationAssets = depreciationAssetsDao.findAll();
        deleteInBatchSoft(depreciationAssets);
        return true;

    }

    @Override
    public List<Boolean> isConsommingDatesInFiscalYear(List<LocalDateTime> datesOfCommissioning) {
        List<Boolean> commissioningDatesInCurrentFiscalYear = new ArrayList<>();
        datesOfCommissioning.forEach((LocalDateTime dateOfCommissioning) -> {
            Long idFiscalYear = fiscalYearService.findFiscalYearOfDate(dateOfCommissioning);
            if (idFiscalYear == null) {
                idFiscalYear = fiscalYearService.findAll().get(0).getId();
            }
            List<Long> listOfGeneratedDocumentAccountIdFromAmortization = documentAccountDao
                    .getListOfDocumentAccountGeneratedFromAmortization(idFiscalYear);
            commissioningDatesInCurrentFiscalYear.add(listOfGeneratedDocumentAccountIdFromAmortization.isEmpty());
        });
        return commissioningDatesInCurrentFiscalYear;
    }
}
