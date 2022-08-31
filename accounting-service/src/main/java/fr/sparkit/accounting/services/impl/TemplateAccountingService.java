package fr.sparkit.accounting.services.impl;

import static fr.sparkit.accounting.constants.AccountingConstants.*;
import static fr.sparkit.accounting.constants.LanguageConstants.ACCOUNTING_TEMPLATE_SHEET_NAME;
import static fr.sparkit.accounting.services.utils.excel.ExcelCellStyleHelper.setInvalidCell;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import fr.sparkit.accounting.auditing.AccountingTemplateExcelCell;
import fr.sparkit.accounting.constants.LanguageConstants;
import fr.sparkit.accounting.constants.XLSXErrors;
import fr.sparkit.accounting.convertor.JournalConverter;
import fr.sparkit.accounting.convertor.TemplateAccountingConverter;
import fr.sparkit.accounting.convertor.TemplateAccountingDetailsConverter;
import fr.sparkit.accounting.dao.TemplateAccountingDao;
import fr.sparkit.accounting.dao.TemplateAccountingDetailsDao;
import fr.sparkit.accounting.dto.FileUploadDto;
import fr.sparkit.accounting.dto.Filter;
import fr.sparkit.accounting.dto.JournalDto;
import fr.sparkit.accounting.dto.TemplateAccountingDetailsDto;
import fr.sparkit.accounting.dto.TemplateAccountingDto;
import fr.sparkit.accounting.dto.TemplatePageDto;
import fr.sparkit.accounting.dto.excel.AccountingTemplateXLSXFormatDto;
import fr.sparkit.accounting.entities.Journal;
import fr.sparkit.accounting.entities.TemplateAccounting;
import fr.sparkit.accounting.entities.TemplateAccountingDetails;
import fr.sparkit.accounting.services.IDocumentAccountLineService;
import fr.sparkit.accounting.services.IJournalService;
import fr.sparkit.accounting.services.ITempalteAccountingService;
import fr.sparkit.accounting.services.ITemplateAccountingDetailsService;
import fr.sparkit.accounting.services.utils.AccountingServiceUtil;
import fr.sparkit.accounting.services.utils.TemplateAccountingUtil;
import fr.sparkit.accounting.services.utils.excel.ExcelCellStyleHelper;
import fr.sparkit.accounting.services.utils.excel.GenericExcelPOIHelper;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.errors.ErrorsResponse;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TemplateAccountingService extends GenericService<TemplateAccounting, Long>
        implements ITempalteAccountingService {
    @Value("${accounting.excel.storage-directory}")
    private Path excelStoragePath;

    private final List<Field> excelHeaderFields;

    private List<String> acceptedHeaders;

    private static final DataFormatter dataFormatter = new DataFormatter();

    private final ITemplateAccountingDetailsService templateAccountingDetailsService;
    private final TemplateAccountingDetailsDao templateAccountingDetailsDao;
    private final TemplateAccountingDao templateAccountingDao;
    private final IJournalService journalService;

    @Autowired
    public TemplateAccountingService(TemplateAccountingDetailsDao templateAccountingDetailsDao,
            TemplateAccountingDao templateAccountingDao, IJournalService journalService,
            ITemplateAccountingDetailsService templateAccountingDetailsService,
            IDocumentAccountLineService documentAccountLineService) {
        super();
        this.templateAccountingDetailsDao = templateAccountingDetailsDao;
        this.templateAccountingDao = templateAccountingDao;
        this.journalService = journalService;
        this.templateAccountingDetailsService = templateAccountingDetailsService;
        excelHeaderFields = AccountingTemplateXLSXFormatDto.getAccountingTemplateExcelHeaderFields();
        acceptedHeaders = excelHeaderFields.stream()
                .map(field -> field.getAnnotation(AccountingTemplateExcelCell.class).headerName())
                .collect(Collectors.toList());
    }

    @Override
    public List<TemplateAccountingDto> getAllTemplateAccounting() {
        List<TemplateAccountingDto> templateAccountingDtos = new ArrayList<>();
        for (TemplateAccounting templateAccounting : templateAccountingDao.findAll()) {
            templateAccountingDtos.add(TemplateAccountingConverter.modelToDto(templateAccounting,
                    TemplateAccountingDetailsConverter.modelsToDtos(templateAccountingDetailsDao
                            .findBytemplateAccountingIdAndIsDeletedFalse(templateAccounting.getId()))));
        }
        return templateAccountingDtos;
    }

    @Override
    public TemplateAccountingDto getTemplateAccountingById(Long id) {
        return TemplateAccountingConverter.modelToDto(templateAccountingDao.findOne(id),
                TemplateAccountingDetailsConverter
                        .modelsToDtos(templateAccountingDetailsDao.findBytemplateAccountingIdAndIsDeletedFalse(id)));
    }

    @Override
    public boolean deleteTemplateAccounting(Long id) {
        TemplateAccountingUtil.checkNull(id);
        log.info(LOG_ENTITY_DELETED, ENTITY_NAME_TEMPLATE_ACCOUNTING, id);
        List<TemplateAccountingDetails> templateAccountingDetails = templateAccountingDetailsDao
                .findBytemplateAccountingIdAndIsDeletedFalse(id);
        templateAccountingDetails.forEach(
                (TemplateAccountingDetails templateAccountingDetail) -> delete(templateAccountingDetail.getId()));
        delete(id);
        return true;
    }

    @Override
    public TemplateAccounting saveTemplateAccounting(TemplateAccountingDto templateAccountingDto) {
        TemplateAccounting templateAccountingWithTheSameName = templateAccountingDao
                .findByLabelAndIsDeletedFalse(templateAccountingDto.getLabel());
        if (templateAccountingWithTheSameName != null
                && !templateAccountingWithTheSameName.getId().equals(templateAccountingDto.getId())) {
            log.error(TRYING_TO_SAVE_TEMPLATE_ACCOUNTING_WITH_EXISTING_LABEL,
                    templateAccountingWithTheSameName.getLabel());
            throw new HttpCustomException(ApiErrors.Accounting.TEMPLATE_ACCOUNTING_LABEL_EXISTS,
                    new ErrorsResponse().error(templateAccountingDto.getLabel()));
        }
        if (!templateAccountingDto.getTemplateAccountingDetails().isEmpty()) {
            JournalDto journal = journalService.findById(templateAccountingDto.getJournalId());
            TemplateAccounting templateAccounting = TemplateAccountingConverter.dtoToModel(templateAccountingDto,
                    JournalConverter.dtoToModel(journal));

            if (TemplateAccountingUtil.isUpdateTemplateAccounting(templateAccountingDto)) {
                templateAccounting.setId(templateAccountingDto.getId());
                deleteUnsavedTemplateAccountingDetails(templateAccountingDto);
            }

            TemplateAccounting templateAccountingSaved = templateAccountingDao.saveAndFlush(templateAccounting);

            saveTemplateAccountingDetails(templateAccountingDto.getTemplateAccountingDetails(),
                    templateAccountingSaved);

            return templateAccountingSaved;

        } else {
            log.error(TEMPLATE_ACCOUNT_WITHOUT_LINES);
            throw new HttpCustomException(ApiErrors.Accounting.TEMPLATE_ACCOUNTING_WITHOUT_LINES_CODE);
        }
    }

    private void saveTemplateAccountingDetails(List<TemplateAccountingDetailsDto> templateAccountingDetails,
            TemplateAccounting templateAccounting) {
        templateAccountingDetails
                .forEach((TemplateAccountingDetailsDto templateAccountingLine) -> templateAccountingDetailsService
                        .save(templateAccountingLine, templateAccounting));
    }

    private void deleteUnsavedTemplateAccountingDetails(TemplateAccountingDto templateAccountingDto) {
        List<Long> templateAccountingDetailsIdToSave = new ArrayList<>();

        templateAccountingDto.getTemplateAccountingDetails()
                .forEach((TemplateAccountingDetailsDto templateAccountingDetail) -> templateAccountingDetailsIdToSave
                        .add(templateAccountingDetail.getId()));

        List<TemplateAccountingDetails> templateAccountingDetails = templateAccountingDetailsDao
                .findBytemplateAccountingIdAndIsDeletedFalse(templateAccountingDto.getId());
        templateAccountingDetails.stream()
                .filter((TemplateAccountingDetails templateAccountingDetail) -> !templateAccountingDetailsIdToSave
                        .contains(templateAccountingDetail.getId()))
                .forEach(
                        (TemplateAccountingDetails templateAccountingDetailToDelete) -> templateAccountingDetailsService
                                .delete(templateAccountingDetailToDelete.getId()));
    }

    private String getDefaultSortFieldForTemplate() {
        String defaultSortFieldForAccount = FIELD_NAME_LABEL;
        if (AccountingServiceUtil.fieldExistsInEntity(defaultSortFieldForAccount, Journal.class)) {
            return defaultSortFieldForAccount;
        } else {
            log.error(TRYING_TO_SORT_USING_NO_EXIST_FILED);
            throw new HttpCustomException(ApiErrors.Accounting.TRYING_TO_SORT_USING_NON_EXISTENT_FIELD);
        }
    }

    @Override
    public List<TemplateAccounting> getTemplateAccountingByJournal(Long journalId) {
        return templateAccountingDao.findByJournalIdAndIsDeletedFalse(journalId);
    }

    @Override
    public TemplatePageDto filterTemplateAccounting(List<Filter> filters, Pageable pageable) {

        if (!pageable.getSort().get().findFirst().isPresent()) {
            pageable = AccountingServiceUtil.getPageable(pageable.getPageNumber(), pageable.getPageSize(),
                    getDefaultSortFieldForTemplate(), Sort.Direction.ASC.toString());
        }

        Page<TemplateAccounting> page = FilterService.getPageOfFilterableEntity(TemplateAccounting.class,
                templateAccountingDao, filters, pageable);

        Stream<TemplateAccountingDto> templateAccountingDtoStream = page.getContent().stream()
                .map(templateAccounting -> TemplateAccountingConverter.modelToDto(templateAccounting,
                        TemplateAccountingDetailsConverter.modelsToDtos(templateAccountingDetailsService
                                .findByTemplateAccountingIdAndIsDeletedFalse(templateAccounting.getId()))));
        return new TemplatePageDto(templateAccountingDtoStream.collect(Collectors.toList()), page.getTotalElements());

    }

    @Override
    public byte[] exportAccountingTemplatesExcelModel() {
        File file = GenericExcelPOIHelper.generateXLSXFileFromData(new ArrayList<>(),
                String.format(IMPORT_MODEL_FILE_NAME, ACCOUNTING_TEMPLATE_SHEET_NAME), excelStoragePath.toFile(),
                acceptedHeaders, excelHeaderFields, ACCOUNTING_TEMPLATE_SHEET_NAME);
        return GenericExcelPOIHelper.convertFileToByteArray(file);
    }

    @Override
    public FileUploadDto loadAccountingTemplatesExcelData(FileUploadDto fileUploadDto) {
        List<TemplateAccountingDto> accountingTemplates = new ArrayList<>();
        List<List<TemplateAccountingDetailsDto>> accountingTemplatesDetails = new ArrayList<>();
        boolean allSheetsAreEmpty;
        boolean documentsAreValid = true;
        Map<TemplateAccountingDetailsDto, Row> linesMap = new HashMap<>();
        ExcelCellStyleHelper.resetStyles();
        try (Workbook workbook = GenericExcelPOIHelper
                .createWorkBookFromBase64String(fileUploadDto.getBase64Content())) {
            allSheetsAreEmpty = true;
            if (workbook.getNumberOfSheets() == 0) {
                throw new HttpCustomException(ApiErrors.Accounting.EXCEL_EMPTY_FILE);
            }
            GenericExcelPOIHelper.validateWorkbookSheetsHeaders(workbook, acceptedHeaders);
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                log.info("Parsing sheet #{}", sheetIndex + 1);
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                boolean isSheetEmpty = GenericExcelPOIHelper.isSheetEmpty(sheet);
                allSheetsAreEmpty &= isSheetEmpty;
                if (isSheetEmpty) {
                    continue;
                }
                documentsAreValid &= isAccountingTemplateValuesAddedToSheet(accountingTemplates,
                        accountingTemplatesDetails, documentsAreValid, sheet, linesMap);
            }
            if (allSheetsAreEmpty) {
                log.error("Trying to import empty document");
                throw new HttpCustomException(ApiErrors.Accounting.EXCEL_EMPTY_FILE);
            } else if (documentsAreValid) {
                log.info("Saving documents");
                saveAccountingTemplatesComingFromExcel(accountingTemplates, accountingTemplatesDetails);
                return new FileUploadDto();
            } else {
                return GenericExcelPOIHelper.getFileUploadDtoFromWorkbook(workbook, excelStoragePath.toFile(),
                        String.format(SIMULATION_EXPORT_FILE_NAME, ACCOUNTING_TEMPLATE_SHEET_NAME));
            }
        } catch (IOException e) {
            log.error(ERROR_CREATING_FILE, e);
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_FILE_CREATION_FAIL);
        }
    }

    private void saveAccountingTemplatesComingFromExcel(List<TemplateAccountingDto> accountingTemplates,
            List<List<TemplateAccountingDetailsDto>> accountingTemplatesDetails) {
        if (accountingTemplates.isEmpty()) {
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_NO_ACCOUNTING_TEMPLATES_TO_BE_SAVED);
        }
        for (int i = 0; i < accountingTemplates.size(); i++) {
            accountingTemplates.get(i).setTemplateAccountingDetails(accountingTemplatesDetails.get(i));
            saveTemplateAccounting(accountingTemplates.get(i));
        }
    }

    private boolean isRowOfTypeTemplate(Row row, int acceptedHeadersSize) {
        for (int cellIndex = 0; cellIndex < acceptedHeadersSize; cellIndex++) {
            Cell cell = row.getCell(cellIndex);
            if (cell != null
                    && cell.getCellType() != CellType.BLANK && excelHeaderFields.get(cell.getColumnIndex())
                            .getAnnotation(AccountingTemplateExcelCell.class).isAccountingTemplateField()
                    && !dataFormatter.formatCellValue(cell).trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean isAccountingTemplateValuesAddedToSheet(List<TemplateAccountingDto> accountingTemplates,
            List<List<TemplateAccountingDetailsDto>> accountingTemplatesDetails, boolean documentsAreValid, Sheet sheet,
            Map<TemplateAccountingDetailsDto, Row> linesMap) {
        TemplateAccountingDto accountingTemplate;
        for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            log.info("Parsing row #{} in sheet {}", rowIndex, sheet.getSheetName());
            Row row = sheet.getRow(rowIndex);
            if (GenericExcelPOIHelper.isRowNotEmpty(row)) {
                if (isRowOfTypeTemplate(row, acceptedHeaders.size())) {
                    accountingTemplate = new TemplateAccountingDto();
                    accountingTemplatesDetails.add(new ArrayList<>());
                    accountingTemplates.add(accountingTemplate);
                    documentsAreValid &= accountingTemplateValuesAddedToRow(accountingTemplate, row, excelHeaderFields);
                }
                if (accountingTemplates.isEmpty()) {
                    log.error("The first data row should contain documentAccount information");
                    throw new HttpCustomException(
                            ApiErrors.Accounting.EXCEL_FIRST_ROW_SHOULD_CONTAIN_DOCUMENT_INFORMATION,
                            new ErrorsResponse().error(sheet.getSheetName()));
                }
                TemplateAccountingDetailsDto accountingTemplateDetail = new TemplateAccountingDetailsDto();
                documentsAreValid &= templateAccountingDetailsService.isAccountingTemplateDetailValuesAddedToRow(
                        accountingTemplateDetail, row, excelHeaderFields, acceptedHeaders);
                documentsAreValid &= validateDocumentAccountDebitAndCreditValues(accountingTemplateDetail,
                        row.getCell(acceptedHeaders.indexOf(LanguageConstants.XLSXHeaders.DEBIT_HEADER_NAME)),
                        row.getCell(acceptedHeaders.indexOf(LanguageConstants.XLSXHeaders.CREDIT_HEADER_NAME)));
                accountingTemplatesDetails.get(accountingTemplates.size() - 1).add(accountingTemplateDetail);
                linesMap.put(accountingTemplateDetail, row);
            }
        }
        return documentsAreValid;
    }

    private static boolean validateDocumentAccountDebitAndCreditValues(
            TemplateAccountingDetailsDto accountingTemplateDetail, Cell creditCell, Cell debitCell) {
        boolean creditCellIsValid = creditCell != null && creditCell.getCellComment() == null;
        boolean debitCellCellIsValid = debitCell != null && debitCell.getCellComment() == null;
        if (creditCellIsValid && debitCellCellIsValid && accountingTemplateDetail.getCreditAmount()
                .multiply(accountingTemplateDetail.getDebitAmount()).compareTo(BigDecimal.ZERO) != 0) {
            setInvalidCell(debitCell, XLSXErrors.DEBIT_OR_CREDIT_VALUE_INVALID);
            setInvalidCell(creditCell, XLSXErrors.DEBIT_OR_CREDIT_VALUE_INVALID);
            return false;
        }
        return true;
    }

    private boolean accountingTemplateValuesAddedToRow(TemplateAccountingDto accountingTemplate, Row row,
            List<Field> excelHeaderFields) {
        boolean isValid = true;
        for (int i = 0; i < excelHeaderFields.size(); i++) {
            Cell cell = row.getCell(i);
            if (cell == null) {
                cell = row.createCell(i);
            }
            cell.setCellComment(null);
            if (excelHeaderFields.get(i).getAnnotation(AccountingTemplateExcelCell.class).isAccountingTemplateField()) {
                switch (excelHeaderFields.get(i).getAnnotation(AccountingTemplateExcelCell.class).headerName()) {
                case LanguageConstants.XLSXHeaders.LABEL_HEADER_NAME:
                    isValid &= setupTemplateLabelFromCell(cell, accountingTemplate);
                    break;
                case LanguageConstants.XLSXHeaders.JOURNAL_HEADER_NAME:
                    isValid &= setupTemplateJournalFromCell(cell, accountingTemplate);
                    break;
                default:
                    isValid = false;
                }
            }
        }
        return isValid;
    }

    private boolean setupTemplateJournalFromCell(Cell cell, TemplateAccountingDto accountingTemplate) {
        if (isJournalInCellValid(cell)) {
            String journalCode = dataFormatter.formatCellValue(cell).trim();
            accountingTemplate.setJournalId(journalService.findByCode(journalCode).getId());
            return true;
        }
        return false;
    }

    private boolean isJournalInCellValid(Cell cell) {
        String journalCode = dataFormatter.formatCellValue(cell).trim();
        if (journalCode.isEmpty()) {
            setInvalidCell(cell, XLSXErrors.REQUIRED_FIELD);
        } else {
            Journal journal = journalService.findByCode(journalCode);
            if (journal == null) {
                setInvalidCell(cell, String.format(XLSXErrors.JournalXLSXErrors.NO_JOURNAL_WITH_CODE, journalCode));
            } else {
                return true;
            }
        }
        return false;
    }

    public boolean setupTemplateLabelFromCell(Cell cell, TemplateAccountingDto accountingTemplate) {
        if (GenericExcelPOIHelper.isLabelInCellValid(cell)) {
            accountingTemplate.setLabel(dataFormatter.formatCellValue(cell).trim());
            return true;
        }
        return false;
    }

    @Override
    public byte[] exportAccountingTemplatesAsExcelFile() {
        List<TemplateAccounting> accountingTemplates = findAll();
        List<Object> accountingTemplatesXLSXFormatDtoList = new ArrayList<>();
        for (TemplateAccounting accountingTemplate : accountingTemplates) {
            createAccountingTemplateXLSXFormatDto(accountingTemplatesXLSXFormatDtoList, accountingTemplate);
        }
        File file = GenericExcelPOIHelper.generateXLSXFileFromData(accountingTemplatesXLSXFormatDtoList,
                String.format(EXPORT_FILE_NAME, ACCOUNTING_TEMPLATE_SHEET_NAME), excelStoragePath.toFile(),
                acceptedHeaders, excelHeaderFields, ACCOUNTING_TEMPLATE_SHEET_NAME);
        return GenericExcelPOIHelper.convertFileToByteArray(file);
    }

    private void createAccountingTemplateXLSXFormatDto(List<Object> accountingTemplatesXLSXFormatDtoList,
            TemplateAccounting accountingTemplate) {
        List<TemplateAccountingDetails> accountingTemplateDetails = templateAccountingDetailsService
                .findByTemplateAccountingIdAndIsDeletedFalse(accountingTemplate.getId());

        boolean isTemplateInfoSet = false;
        AccountingTemplateXLSXFormatDto accountingTemplateXLSXFormatDto = new AccountingTemplateXLSXFormatDto();
        for (TemplateAccountingDetails accountingTemplateDetail : accountingTemplateDetails) {
            if (!isTemplateInfoSet) {
                accountingTemplateXLSXFormatDto.setTemplateLabel(accountingTemplate.getLabel());
                accountingTemplateXLSXFormatDto.setJournal(accountingTemplate.getJournal().getCode());
                isTemplateInfoSet = true;
            } else {
                accountingTemplateXLSXFormatDto = new AccountingTemplateXLSXFormatDto();
            }
            accountingTemplateXLSXFormatDto.setAccountCode(accountingTemplateDetail.getAccount().getCode());
            accountingTemplateXLSXFormatDto.setAccountingTemplateDetailLabel(accountingTemplateDetail.getLabel());
            accountingTemplateXLSXFormatDto
                    .setAccountingTemplateDetailDebitValue(accountingTemplateDetail.getDebitAmount());
            accountingTemplateXLSXFormatDto
                    .setAccountingTemplateDetailCreditValue(accountingTemplateDetail.getCreditAmount());
            accountingTemplatesXLSXFormatDtoList.add(accountingTemplateXLSXFormatDto);
        }
    }
}
