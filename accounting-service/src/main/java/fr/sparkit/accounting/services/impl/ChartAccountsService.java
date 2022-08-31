package fr.sparkit.accounting.services.impl;

import static fr.sparkit.accounting.constants.AccountingConstants.ALREADY_USED_CHART_ACCOUNT_CANT_CHANGE_CODE;
import static fr.sparkit.accounting.constants.AccountingConstants.ALREADY_USED_CHART_ACCOUNT_CANT_DELETE;
import static fr.sparkit.accounting.constants.AccountingConstants.CHART_ACCOUNT_NO_EXIST;
import static fr.sparkit.accounting.constants.AccountingConstants.ERROR_CREATING_FILE;
import static fr.sparkit.accounting.constants.AccountingConstants.EXCEEDED_CODE_MAX;
import static fr.sparkit.accounting.constants.AccountingConstants.EXISTING_CODE;
import static fr.sparkit.accounting.constants.AccountingConstants.EXPORT_FILE_NAME;
import static fr.sparkit.accounting.constants.AccountingConstants.IMPORT_MODEL_FILE_NAME;
import static fr.sparkit.accounting.constants.AccountingConstants.INVALID_CODE;
import static fr.sparkit.accounting.constants.AccountingConstants.LOG_ENTITY_CREATED;
import static fr.sparkit.accounting.constants.AccountingConstants.MAX_CHART_ACCOUNT_CODE;
import static fr.sparkit.accounting.constants.AccountingConstants.MISSING_PARAMETERS;
import static fr.sparkit.accounting.constants.AccountingConstants.PREFIX_ERROR;
import static fr.sparkit.accounting.constants.AccountingConstants.SIMULATION_EXPORT_FILE_NAME;
import static fr.sparkit.accounting.constants.AccountingConstants.THERE_IS_NO_CHART_ACCOUNT_WITH_THIS_ID;
import static java.lang.Math.toIntExact;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import fr.sparkit.accounting.auditing.ChartAccountExcelCell;
import fr.sparkit.accounting.constants.LanguageConstants;
import fr.sparkit.accounting.constants.NumberConstant;
import fr.sparkit.accounting.constants.XLSXErrors;
import fr.sparkit.accounting.convertor.ChartAccountConvertor;
import fr.sparkit.accounting.dao.ChartAccountsDao;
import fr.sparkit.accounting.dto.ChartAccountsDto;
import fr.sparkit.accounting.dto.ChartAccountsToBalancedDto;
import fr.sparkit.accounting.dto.FileUploadDto;
import fr.sparkit.accounting.dto.excel.ChartAccountXLSXFormatDto;
import fr.sparkit.accounting.entities.ChartAccounts;
import fr.sparkit.accounting.services.IChartAccountsService;
import fr.sparkit.accounting.services.utils.excel.ExcelCellStyleHelper;
import fr.sparkit.accounting.services.utils.excel.GenericExcelPOIHelper;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.errors.ErrorsResponse;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ChartAccountsService extends GenericService<ChartAccounts, Long> implements IChartAccountsService {

    public static final String CHART_ACCOUNTS_SHEET_NAME = "Plans Comptables";
    @Value("${accounting.excel.storage-directory}")
    private Path excelStoragePath;
    private static final DataFormatter dataFormatter = new DataFormatter();
    private final ChartAccountsDao chartAccountDao;
    private List<Field> excelHeaderFields;

    private List<String> acceptedHeaders;

    @Autowired
    public ChartAccountsService(ChartAccountsDao chartAccountDao) {
        this.chartAccountDao = chartAccountDao;
        excelHeaderFields = ChartAccountXLSXFormatDto.getChartAccountExcelHeaderFields();
        acceptedHeaders = excelHeaderFields.stream()
                .map(field -> field.getAnnotation(ChartAccountExcelCell.class).headerName())
                .collect(Collectors.toList());
    }

    @Override
    public ChartAccountsDto findById(Long id) {
        ChartAccounts chartAccount = chartAccountDao.findOne(id);
        if (chartAccount != null) {
            return ChartAccountConvertor.modelToDto(chartAccount);
        }
        log.error(THERE_IS_NO_CHART_ACCOUNT_WITH_THIS_ID, id);
        throw new HttpCustomException(ApiErrors.Accounting.CHART_ACCOUNT_INEXISTANT, new ErrorsResponse().error(id));
    }

    @Override
    public List<ChartAccountsDto> findAllCharts() {
        List<ChartAccountsDto> alltrees = new ArrayList<>();
        List<ChartAccounts> roots = chartAccountDao.findAll();
        roots.forEach((ChartAccounts root) -> {
            if (root.getAccountParent() != null) {
                ChartAccountsDto node = new ChartAccountsDto(root.getId(), null, root.getCode(), root.getLabel(),
                        Collections.emptyList());
                alltrees.add(node);
            }
        });
        alltrees.sort((ChartAccountsDto firstCode, ChartAccountsDto secondCode) -> {
            String firstCodeAsString = ((Integer) firstCode.getCode()).toString();
            String secondCodeAsString = ((Integer) secondCode.getCode()).toString();
            return firstCodeAsString.compareTo(secondCodeAsString);
        });
        return alltrees;
    }

    @Override
    @Cacheable(value = "ChartAccountsCache", key = "'ChartAccountsCache_'"
            + "+T(fr.sparkit.accounting.util.CompanyContextHolder).getCompanyContext()", unless = "#result==null")
    public List<ChartAccountsDto> buildAllTree() {
        List<ChartAccounts> chartAcountsList = chartAccountDao.findByIsDeletedFalse();
        return chartAcountsList.stream().filter(chartAcount -> chartAcount.getAccountParent() == null)
                .map(chartAcount -> buidChildrenChartAccount(chartAcount, chartAcountsList))
                .collect(Collectors.toList());
    }

    private ChartAccountsDto buidChildrenChartAccount(ChartAccounts chartAcount, List<ChartAccounts> chartAcountsList) {
        return new ChartAccountsDto(chartAcount.getId(), null, chartAcount.getCode(), chartAcount.getLabel(),
                buildTreeChild(chartAcount.getId(), chartAcountsList));
    }

    public List<ChartAccountsDto> buildTreeChild(Long parentId, List<ChartAccounts> chartAcountsList) {
        List<ChartAccountsDto> chartAccountChild = new ArrayList<>();
        List<ChartAccounts> chartAccounts = chartAcountsList.stream()
                .filter(chartAcount -> chartAcount.getAccountParent() != null
                        && chartAcount.getAccountParent().getId().equals(parentId))
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(chartAccounts)) {
            chartAccounts.forEach((ChartAccounts account) -> {
                ChartAccountsDto child = new ChartAccountsDto(account.getId(), parentId, account.getCode(),
                        account.getLabel(), buildTreeChild(account.getId(), chartAcountsList));
                int index = getIndexChild(chartAccountChild, child);
                chartAccountChild.add(index, child);
            });
        }
        return chartAccountChild;
    }

    @Override
    public List<ChartAccountsDto> findSubTreeByLabelOrCode(String value) {
        int code;
        try {
            code = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            code = 0;
        }
        List<ChartAccounts> chartAccounts = chartAccountDao
                .findByLabelContainsIgnoreCaseAndIsDeletedFalseOrCodeLikeAndIsDeletedFalse(value, code);
        List<ChartAccounts> chartAcountsList = chartAccountDao.findByIsDeletedFalse();
        return chartAccounts.stream()
                .map(chartAcount -> buidChildrenChartAccount(chartAcount, chartAcountsList)).collect(Collectors.toList());
    }

    @Override
    public ChartAccountsDto findByCode(Integer code) {
        Optional<ChartAccounts> chartAccountsOpt = chartAccountDao.findByCodeAndIsDeletedFalse(code);
        ChartAccountsDto chartAccountsDto = new ChartAccountsDto();
        if (chartAccountsOpt.isPresent()) {
            chartAccountsDto.setId(chartAccountsOpt.get().getId());
            chartAccountsDto.setCode(chartAccountsOpt.get().getCode());
            chartAccountsDto.setLabel(chartAccountsOpt.get().getLabel());
        }
        return chartAccountsDto;
    }

    @Override
    public ChartAccountsDto findByCodeIteration(Integer code) {
        StringBuilder chartAccountCode = new StringBuilder();
        chartAccountCode.append(code);
        ChartAccountsDto chartAccountsDto = null;
        do {
            chartAccountsDto = findByCode(Integer.parseInt(chartAccountCode.toString()));
            chartAccountCode.deleteCharAt(chartAccountCode.length() - 1);
        } while (chartAccountCode.length() >= NumberConstant.TWO && chartAccountsDto.getCode() == NumberConstant.ZERO);
        return chartAccountsDto;
    }

    @Override
    public List<ChartAccountsDto> buildChildTree(Long parentId) {
        List<ChartAccountsDto> children = new ArrayList<>();
        List<ChartAccounts> chartAccounts = chartAccountDao.findByAccountParentIdAndIsDeletedFalse(parentId);
        if (!CollectionUtils.isEmpty(chartAccounts)) {
            chartAccounts.forEach((ChartAccounts account) -> {
                ChartAccountsDto child = new ChartAccountsDto(account.getId(), parentId, account.getCode(),
                        account.getLabel(), buildChildTree(account.getId()));
                int index = getIndexChild(children, child);
                children.add(index, child);
            });
        }

        return children;
    }

    public int getIndexChild(Collection<ChartAccountsDto> chartAccounts, ChartAccountsDto child) {
        long index = chartAccounts.stream().filter((ChartAccountsDto ca) -> ca.getCode() < child.getCode()).count();
        return toIntExact(index);
    }

    @Override
    @CacheEvict(value = "ChartAccountsCache", allEntries = true)
    public boolean deleteSubTree(Long id) {
        ChartAccountsDto chartAccountsToDeleted = findById(id);
        if (isChartAccountUsed(id)) {
            log.error(ALREADY_USED_CHART_ACCOUNT_CANT_DELETE, chartAccountsToDeleted.getCode());
            throw new HttpCustomException(ApiErrors.Accounting.CHART_ACCOUNT_ALREADY_USED_CANT_DELETE,
                    new ErrorsResponse().error(chartAccountsToDeleted.getCode()));
        }
        delete(id);
        return true;
    }

    @Override
    @CacheEvict(value = "ChartAccountsCache", allEntries = true)
    public ChartAccounts update(ChartAccountsDto chartAccountsDto) {
        checkValuesNotNull(chartAccountsDto);
        ChartAccounts chartAccount = chartAccountDao.findOne(chartAccountsDto.getId());
        Objects.requireNonNull(chartAccount);
        if (chartAccountsDto.getCode() != chartAccount.getCode()) {
            checkCodeExists(chartAccountsDto.getCode());
        }
        if (Objects.nonNull(chartAccountsDto.getParentId())) {
            ChartAccounts chartAccountParent = Optional
                    .ofNullable(chartAccountDao.findOne(chartAccountsDto.getParentId())).orElse(null);
            checkParentChartAccountNonNull(chartAccountParent);
            checkValidCodePrefix(chartAccountParent, chartAccountsDto);
        }
        if (isChartAccountUsed(chartAccountsDto.getId()) && chartAccountsDto.getCode() != chartAccount.getCode()) {
            log.error(ALREADY_USED_CHART_ACCOUNT_CANT_CHANGE_CODE);
            throw new HttpCustomException(ApiErrors.Accounting.CHART_ACCOUNT_ALREADY_USED_CANT_CHANGE_CODE);
        }
        checkMaxCodeLengthNotExceeded(chartAccountsDto.getCode());
        chartAccount.setCode(chartAccountsDto.getCode());
        chartAccount.setLabel(chartAccountsDto.getLabel());
        ChartAccounts updateChartAccounts = saveAndFlush(chartAccount);
        log.info(LOG_ENTITY_CREATED, updateChartAccounts);
        return updateChartAccounts;
    }

    public void checkMaxCodeLengthNotExceeded(int code) {
        if (code > MAX_CHART_ACCOUNT_CODE) {
            log.error(EXCEEDED_CODE_MAX, code, MAX_CHART_ACCOUNT_CODE);
            throw new HttpCustomException(ApiErrors.Accounting.CHART_ACCOUNT_MAX_CODE_EXCEEDED,
                    new ErrorsResponse().error(MAX_CHART_ACCOUNT_CODE));
        }
    }

    @Override
    public boolean isChartAccountUsed(Long idChart) {
        return !isRelated(idChart, ChartAccounts.class.getName());
    }

    @Override
    @CacheEvict(value = "ChartAccountsCache", allEntries = true)
    public FileUploadDto loadChartAccountsExcelData(FileUploadDto fileUploadDto) {
        try (Workbook workbook = GenericExcelPOIHelper
                .createWorkBookFromBase64String(fileUploadDto.getBase64Content())) {
            List<ChartAccounts> chartAccounts = new ArrayList<>();
            boolean chartAccountsAreValid = true;
            GenericExcelPOIHelper.validateWorkbookSheetsHeaders(workbook, acceptedHeaders);
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                log.info("Parsing sheet #{}", sheetIndex + 1);
                Sheet sheet = workbook.getSheetAt(sheetIndex);

                List<Integer> previousChartCodes = new ArrayList<>();
                for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    log.info("Parsing row #{} in sheet #{}", rowIndex, sheetIndex + 1);
                    Row row = sheet.getRow(rowIndex);
                    if (GenericExcelPOIHelper.isRowNotEmpty(row)) {
                        GenericExcelPOIHelper.validateNumberOfCellsInRowAgainstHeaders(row, acceptedHeaders.size());
                        ChartAccounts chartAccount = new ChartAccounts();
                        chartAccounts.add(chartAccount);
                        chartAccountsAreValid &= isChartAccountValuesAddedInRow(chartAccount, row, previousChartCodes,
                                excelHeaderFields, acceptedHeaders);
                    }
                }
            }
            if (chartAccountsAreValid) {
                log.info("Saving chart accounts");
                saveChartAccountsComingFromExcel(chartAccounts);
                return new FileUploadDto();
            } else {
                return GenericExcelPOIHelper.getFileUploadDtoFromWorkbook(workbook, excelStoragePath.toFile(),
                        String.format(SIMULATION_EXPORT_FILE_NAME, CHART_ACCOUNTS_SHEET_NAME));
            }
        } catch (IOException e) {
            log.error(ERROR_CREATING_FILE, e);
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_FILE_CREATION_FAIL);
        }
    }

    public void saveChartAccountsComingFromExcel(Collection<ChartAccounts> chartAccounts) {
        if (chartAccounts.isEmpty()) {
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_NO_CHART_ACCOUNTS_TO_BE_SAVED);
        }
        for (ChartAccounts chartAccount : chartAccounts) {
            ChartAccountsDto chartAccountDto = new ChartAccountsDto();
            if (chartAccount.getAccountParent() != null) {
                ChartAccountsDto parent = findByCode(chartAccount.getAccountParent().getCode());
                chartAccountDto.setParentId(parent.getId());
            }
            chartAccountDto.setLabel(chartAccount.getLabel());
            chartAccountDto.setCode(chartAccount.getCode());
            save(chartAccountDto);
        }
    }

    private boolean isChartAccountValuesAddedInRow(ChartAccounts chartAccount, Row row,
            List<Integer> previousChartCodes, List<Field> excelHeaderFields, List<String> acceptedHeaders) {
        boolean isValid = true;
        for (int i = 0; i < excelHeaderFields.size(); i++) {
            Cell cell = row.getCell(i);
            if (cell == null) {
                cell = row.createCell(i);
            }
            cell.setCellComment(null);
            switch (excelHeaderFields.get(i).getAnnotation(ChartAccountExcelCell.class).headerName()) {
            case LanguageConstants.XLSXHeaders.PARENT_PLAN_CODE_HEADER_NAME:
                isValid &= isChartAccountParentSet(cell, chartAccount, previousChartCodes);
                break;
            case LanguageConstants.XLSXHeaders.PLAN_CODE_HEADER_NAME:
                isValid &= isChartAccountCodeSet(cell, chartAccount, previousChartCodes);
                break;
            case LanguageConstants.XLSXHeaders.LABEL_HEADER_NAME:
                isValid &= isChartAccountLabelsSet(cell, chartAccount);
                break;
            default:
                isValid = false;
            }
        }
        return isValid && validateParentCodeWithChartCodeInRow(chartAccount, row, acceptedHeaders);
    }

    public boolean isChartAccountLabelsSet(Cell cell, ChartAccounts chartAccount) {
        if (GenericExcelPOIHelper.isLabelInCellValid(cell)) {
            chartAccount.setLabel(dataFormatter.formatCellValue(cell).trim());
            return true;
        }
        return false;
    }

    public void setChartAccountCodeFromCell(Cell cell, ChartAccounts chartAccount,
            Collection<Integer> previousChartCodes) {
        String cellValue = dataFormatter.formatCellValue(cell).trim();
        int chartAccountCode = Integer.parseInt(cellValue);
        chartAccount.setCode(chartAccountCode);
        previousChartCodes.add(chartAccountCode);
    }

    public boolean bothParentCodeAndChartCodeAreValid(Row row) {
        return row.getCell(acceptedHeaders.indexOf(LanguageConstants.XLSXHeaders.PLAN_CODE_HEADER_NAME))
                .getCellComment() == null
                && row.getCell(acceptedHeaders.indexOf(LanguageConstants.XLSXHeaders.PARENT_PLAN_CODE_HEADER_NAME))
                        .getCellComment() == null;
    }

    public boolean isChartAccountCodeSet(Cell cell, ChartAccounts chartAccount, List<Integer> previousChartCodes) {
        if (isChartAccountCodeInCellValid(cell, previousChartCodes)) {
            setChartAccountCodeFromCell(cell, chartAccount, previousChartCodes);
            return true;
        }
        return false;
    }

    public boolean isChartAccountParentSet(Cell cell, ChartAccounts chartAccount, List<Integer> previousChartCodes) {
        if (isChartAccountParentCodeInCellValid(chartAccount, cell, previousChartCodes)) {
            setChartAccountParentCodeFromCell(cell, chartAccount);
            return true;
        }
        return false;
    }

    public boolean validateParentCodeWithChartCodeInRow(ChartAccounts chartAccount, Row row,
            List<String> acceptedHeaders) {
        if (bothParentCodeAndChartCodeAreValid(row)) {
            if (chartAccount.getAccountParent() != null) {
                if (!String.valueOf(chartAccount.getCode())
                        .startsWith(String.valueOf(chartAccount.getAccountParent().getCode()))
                        || String.valueOf(chartAccount.getCode()).length()
                                - String.valueOf(chartAccount.getAccountParent().getCode()).length() != 1) {
                    Cell cell = row
                            .getCell(acceptedHeaders.indexOf(LanguageConstants.XLSXHeaders.PLAN_CODE_HEADER_NAME));
                    ExcelCellStyleHelper.setInvalidCell(cell,
                            XLSXErrors.ChartAccountXLSXErrors.CHART_ACCOUNT_CODE_SHOULD_BE_IMMEDIAT_CHILD_TO_PARENT_CODE);
                    return false;
                }
            } else if (String.valueOf(chartAccount.getCode()).length() != 1) {
                Cell cell = row
                        .getCell(acceptedHeaders.indexOf(LanguageConstants.XLSXHeaders.PARENT_PLAN_CODE_HEADER_NAME));
                ExcelCellStyleHelper.setInvalidCell(cell,
                        XLSXErrors.ChartAccountXLSXErrors.PARENT_CODE_SHOULD_BE_SPECIFIED);
                return false;
            }
        }
        return true;
    }

    public boolean isChartAccountCodeInCellValid(Cell cell, Collection<Integer> previousChartCodes) {
        String chartAccountCodeString = dataFormatter.formatCellValue(cell).trim();
        if (!chartAccountCodeString.isEmpty()) {
            try {
                return validateChartAccountCodeCell(cell, previousChartCodes, chartAccountCodeString);

            } catch (NumberFormatException e) {
                ExcelCellStyleHelper.setInvalidCell(cell,
                        XLSXErrors.ChartAccountXLSXErrors.CHART_ACCOUNT_CODE_CELL_SHOULD_BE_OF_TYPE_NUMBER);
            }
        } else {
            ExcelCellStyleHelper.setInvalidCell(cell, XLSXErrors.REQUIRED_FIELD);
        }
        return false;
    }

    private boolean validateChartAccountCodeCell(Cell cell, Collection<Integer> previousChartCodes,
            String chartAccountCodeString) {
        int chartAccountCode = Integer.parseInt(chartAccountCodeString);
        if (chartAccountCode <= 0) {
            ExcelCellStyleHelper.setInvalidCell(cell,
                    XLSXErrors.ChartAccountXLSXErrors.CHART_ACCOUNT_CODE_CANT_BE_NEGATIVE_OR_ZERO);
        } else if (chartAccountCode > MAX_CHART_ACCOUNT_CODE) {
            ExcelCellStyleHelper.setInvalidCell(cell,
                    String.format(XLSXErrors.ChartAccountXLSXErrors.CHART_ACCOUNT_MAX_CHART_ACCOUNT_CODE_EXCEEDED,
                            MAX_CHART_ACCOUNT_CODE));
        } else {
            ChartAccountsDto chartAccountWithCode = findByCode(chartAccountCode);
            if (chartAccountWithCode.getId() != null || previousChartCodes.contains(chartAccountCode)) {
                ExcelCellStyleHelper.setInvalidCell(cell, XLSXErrors.ChartAccountXLSXErrors.CHART_ACCOUNT_EXISTS);
            } else {
                return true;
            }
        }
        return false;
    }

    private boolean isChartAccountParentCodeInCellValid(ChartAccounts chartAccount, Cell cell,
            List<Integer> previousChartCodes) {
        String cellValue = dataFormatter.formatCellValue(cell).trim();
        if (!cellValue.isEmpty()) {
            try {
                return validateChartAccountParentCodeCell(chartAccount, cell, previousChartCodes, cellValue);
            } catch (NumberFormatException e) {
                ExcelCellStyleHelper.setInvalidCell(cell,
                        XLSXErrors.ChartAccountXLSXErrors.CHART_ACCOUNT_CODE_CELL_SHOULD_BE_OF_TYPE_NUMBER);
            }
        } else {
            return true;
        }
        return false;

    }

    private boolean validateChartAccountParentCodeCell(ChartAccounts chartAccount, Cell cell,
            List<Integer> previousChartCodes, String cellValue) {
        int chartAccountCode = Integer.parseInt(cellValue);
        if (chartAccountCode <= 0) {
            ExcelCellStyleHelper.setInvalidCell(cell,
                    XLSXErrors.ChartAccountXLSXErrors.CHART_ACCOUNT_CODE_CANT_BE_NEGATIVE_OR_ZERO);
        } else if (chartAccountCode > MAX_CHART_ACCOUNT_CODE / NumberConstant.TEN) {
            ExcelCellStyleHelper.setInvalidCell(cell,
                    String.format(
                            XLSXErrors.ChartAccountXLSXErrors.CHART_ACCOUNT_MAX_PARENT_CHART_ACCOUNT_CODE_EXCEEDED,
                            MAX_CHART_ACCOUNT_CODE / NumberConstant.TEN));
        } else {
            ChartAccountsDto chartAccountWithCode = findByCode(chartAccountCode);
            if (chartAccountWithCode.getId() == null && !previousChartCodes.contains(chartAccountCode)) {
                ExcelCellStyleHelper.setInvalidCell(cell,
                        String.format(XLSXErrors.ChartAccountXLSXErrors.NO_CHART_ACCOUNT_WITH_CODE, chartAccountCode));
            } else {
                ChartAccounts parentChartAccount = new ChartAccounts();
                parentChartAccount.setCode(chartAccountCode);
                chartAccount.setAccountParent(parentChartAccount);
                return true;
            }
        }
        return false;
    }

    @Override
    @CacheEvict(value = "ChartAccountsCache", allEntries = true)
    public ChartAccounts save(ChartAccountsDto chartAccountsDto) {
        checkValuesNotNull(chartAccountsDto);
        checkCodeExists(chartAccountsDto.getCode());
        ChartAccounts chartAccountParent = Optional.ofNullable(chartAccountDao.findOne(chartAccountsDto.getParentId()))
                .orElse(null);
        ChartAccounts chartAccount = new ChartAccounts();
        if (chartAccountParent != null) {
            checkValidCodePrefix(chartAccountParent, chartAccountsDto);
            chartAccount.setAccountParent(chartAccountParent);
        } else {
            checkValidCodeClass(chartAccountsDto);
        }
        checkMaxCodeLengthNotExceeded(chartAccountsDto.getCode());
        chartAccount.setCode(chartAccountsDto.getCode());
        chartAccount.setLabel(chartAccountsDto.getLabel());
        ChartAccounts savedChartAccounts = chartAccountDao.saveAndFlush(chartAccount);
        log.info(LOG_ENTITY_CREATED, savedChartAccounts);
        return savedChartAccounts;
    }

    @Override
    public List<ChartAccountsDto> findByCodes(int code) {
        return chartAccountDao.findByCodes(Integer.toString(code));
    }

    public void checkValidCodePrefix(ChartAccounts parent, ChartAccountsDto child) {
        if (!String.valueOf(child.getCode()).startsWith(String.valueOf(parent.getCode()))) {
            log.error(PREFIX_ERROR);
            throw new HttpCustomException(ApiErrors.Accounting.ACCOUNT_CODE_DIFFERENT_THAN_PARENT);
        }
    }

    public void checkValidCodeClass(ChartAccountsDto chartAccountDto) {
        if (String.valueOf(chartAccountDto.getCode()).length() != 1) {
            log.error(INVALID_CODE);
            throw new HttpCustomException(ApiErrors.Accounting.ACCOUNT_CODE_LENGTH_INVALID);
        }
    }

    public void checkValuesNotNull(ChartAccountsDto chartAccountsDto) {
        if (chartAccountsDto.getCode() < 0 || chartAccountsDto.getLabel() == null
                || chartAccountsDto.getLabel().isEmpty()) {
            log.error(MISSING_PARAMETERS);
            throw new HttpCustomException(ApiErrors.Accounting.CHART_ACCOUNT_MISSING_PARAMETERS);
        }

    }

    public void checkCodeExists(int code) {
        if (this.chartAccountDao.existsByCodeAndIsDeletedFalse(code)) {
            findByCode(code);
            log.error(EXISTING_CODE, code);
            throw new HttpCustomException(ApiErrors.Accounting.CHART_ACCOUNT_CODE_EXISTS,
                    new ErrorsResponse().error(code));
        }
    }

    public void checkParentChartAccountNonNull(ChartAccounts parentChartAccount) {
        if (parentChartAccount == null) {
            log.error(CHART_ACCOUNT_NO_EXIST);
            throw new HttpCustomException(ApiErrors.Accounting.CHART_ACCOUNT_PARENT_CHART_ACCOUNT_DONT_EXIST);
        }
    }

    public void setChartAccountParentCodeFromCell(Cell cell, ChartAccounts chartAccount) {
        String cellValue = dataFormatter.formatCellValue(cell).trim();
        if (!cellValue.isEmpty()) {
            int chartAccountCode = Integer.parseInt(cellValue);
            ChartAccounts parentChartAccount = new ChartAccounts();
            parentChartAccount.setCode(chartAccountCode);
            chartAccount.setAccountParent(parentChartAccount);
        }
    }

    @Override
    public void balanceChartAccounts(List<Long> chartAccounts) {
        chartAccountDao.updateAllChartAccountsToNotBalanced();
        if (chartAccounts != null && !chartAccounts.isEmpty()) {
            chartAccountDao.updateChartAccountsToBalanced(chartAccounts);
        }
    }

    @Override
    public ChartAccountsToBalancedDto getChartAccountsToBalanced() {
        return new ChartAccountsToBalancedDto(chartAccountDao.findChartAccountsToBalanced());
    }

    @Override
    public List<Integer> getChartAccountsCodeToBalanced() {
        return chartAccountDao.findChartAccountsCodeToBalanced();
    }

    @Override
    public byte[] exportChartAccountsAsExcelFile() {
        List<ChartAccounts> chartAccounts = findAll();
        chartAccounts.sort(Comparator.comparing(ChartAccounts::getCode));
        List<Object> chartAccountXLSXFormatDtoList = new ArrayList<>();
        for (ChartAccounts chartAccount : chartAccounts) {
            ChartAccountXLSXFormatDto chartAccountXLSXFormatDto = new ChartAccountXLSXFormatDto();
            if (chartAccount.getAccountParent() != null) {
                chartAccountXLSXFormatDto.setParentPlanCode(chartAccount.getAccountParent().getCode());
            }
            chartAccountXLSXFormatDto.setPlanCode(chartAccount.getCode());
            chartAccountXLSXFormatDto.setLabel(chartAccount.getLabel());
            chartAccountXLSXFormatDtoList.add(chartAccountXLSXFormatDto);
        }
        File file = GenericExcelPOIHelper.generateXLSXFileFromData(chartAccountXLSXFormatDtoList,
                String.format(EXPORT_FILE_NAME, CHART_ACCOUNTS_SHEET_NAME), excelStoragePath.toFile(), acceptedHeaders,
                excelHeaderFields, CHART_ACCOUNTS_SHEET_NAME);
        return GenericExcelPOIHelper.convertFileToByteArray(file);
    }

    @Override
    public byte[] exportChartAccountsExcelModel() {
        File file = GenericExcelPOIHelper.generateXLSXFileFromData(new ArrayList<>(),
                String.format(IMPORT_MODEL_FILE_NAME, CHART_ACCOUNTS_SHEET_NAME), excelStoragePath.toFile(),
                acceptedHeaders, excelHeaderFields, CHART_ACCOUNTS_SHEET_NAME);
        return GenericExcelPOIHelper.convertFileToByteArray(file);
    }

    @Override
    public byte[] getChartAccountsExcelFile(String fileName) {
        return GenericExcelPOIHelper.getExcelFile(fileName, excelStoragePath.toFile());
    }

    @Override
    public void deleteChartAccountsExcelFile(String fileName) {
        GenericExcelPOIHelper.deleteExcelFile(fileName, excelStoragePath.toFile());
    }

    @Override
    public List<ChartAccounts> findChartAccountByLength(int length, int customerCode, int supplierCode) {
        return chartAccountDao.findChartAccountByLength(length, customerCode, supplierCode);
    }

    @Override
    public List<ChartAccounts> findChartAccountTierByLength(int length, int tierCode) {
        return chartAccountDao.findChartAccountTierByLength(length, tierCode);
    }
}
