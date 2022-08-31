package fr.sparkit.accounting.services.utils.excel;

import static fr.sparkit.accounting.constants.AccountingConstants.*;
import static fr.sparkit.accounting.services.utils.excel.ExcelCellStyleHelper.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.EmptyFileException;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hssf.OldExcelFormatException;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import fr.sparkit.accounting.auditing.AccountExcelCell;
import fr.sparkit.accounting.auditing.AccountingTemplateExcelCell;
import fr.sparkit.accounting.auditing.ChartAccountExcelCell;
import fr.sparkit.accounting.auditing.DocumentAccountExcelCell;
import fr.sparkit.accounting.auditing.JournalExcelCell;
import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.constants.XLSXErrors;
import fr.sparkit.accounting.dto.FileUploadDto;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.errors.ErrorsResponse;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public final class GenericExcelPOIHelper {

    private static final DataFormatter dataFormatter = new DataFormatter();

    private GenericExcelPOIHelper() {
        super();
    }

    public static File generateXLSXFileFromData(List<Object> data, String fileName, File storagePath,
            Collection<String> acceptedHeaders, Collection<Field> excelHeaderFields, String sheetName) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        resetStyles();
        Sheet sheet = workbook.createSheet(sheetName);
        sheet.setPrintGridlines(true);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.cloneStyleFrom(getHeaderStyle(workbook));
        CellStyle darkerHeaderStyle = workbook.createCellStyle();
        darkerHeaderStyle.cloneStyleFrom(getDarkerHeaderStyle(workbook));
        int rowIndex = 0;
        int cellIndex = 0;
        Row rowHeader = sheet.createRow(rowIndex);
        for (String headerName : acceptedHeaders) {
            if (headerName.equalsIgnoreCase(AccountingConstants.ERRORS_HEADER_NAME)) {
                cellIndex = createHeaderCell(fileName, cellIndex, rowHeader, headerName);
                continue;
            }
            Cell headerCell = rowHeader.createCell(cellIndex);
            headerCell.setCellValue(headerName);
            Field field = (Field) excelHeaderFields.toArray()[cellIndex];
            if (isFieldHeaderDarker(field)) {
                headerCell.setCellStyle(darkerHeaderStyle);
            } else {
                headerCell.setCellStyle(headerStyle);
            }
            String toolTipMessage = getToolTipMessageFromCellField(field);
            if (!toolTipMessage.isEmpty()) {
                setComment(headerCell, toolTipMessage);
            }
            cellIndex++;
        }
        initDataInRows(data, sheet, excelHeaderFields);
        for (int i = 0; i < acceptedHeaders.size(); i++) {
            sheet.autoSizeColumn(i);
        }
        try {
            String excelFilePrefix = getFileNamePrefix(fileName);
            File excelFile = File.createTempFile(excelFilePrefix, AccountingConstants.XLSX, storagePath);
            OutputStream fileOutputStream = new FileOutputStream(excelFile);
            workbook.write(fileOutputStream);
            fileOutputStream.close();
            return excelFile;
        } catch (IOException e) {
            log.error("Excel file couldn't be created for {}", sheetName, e);
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_FILE_CREATION_FAIL);
        }
    }

    public static boolean isFieldHeaderDarker(AnnotatedElement field) {
        Boolean isFieldHeaderDarker = null;
        if (field.getAnnotation(DocumentAccountExcelCell.class) != null) {
            isFieldHeaderDarker = field.getAnnotation(DocumentAccountExcelCell.class).isDarker();
        }
        if (field.getAnnotation(ChartAccountExcelCell.class) != null) {
            isFieldHeaderDarker = field.getAnnotation(ChartAccountExcelCell.class).isDarker();
        }
        if (field.getAnnotation(JournalExcelCell.class) != null) {
            isFieldHeaderDarker = field.getAnnotation(JournalExcelCell.class).isDarker();
        }
        if (field.getAnnotation(AccountExcelCell.class) != null) {
            isFieldHeaderDarker = field.getAnnotation(AccountExcelCell.class).isDarker();
        }
        if (field.getAnnotation(AccountingTemplateExcelCell.class) != null) {
            isFieldHeaderDarker = field.getAnnotation(AccountingTemplateExcelCell.class).isDarker();
        }
        if (isFieldHeaderDarker != null) {
            return isFieldHeaderDarker;
        }
        throw new HttpCustomException(ApiErrors.Accounting.FIELD_NOT_ANNOTATED_WITH_EXCEL_CELL);
    }

    private static String getToolTipMessageFromCellField(Field field) {
        String toolTipMessage = null;
        if (field.getAnnotation(DocumentAccountExcelCell.class) != null) {
            toolTipMessage = field.getAnnotation(DocumentAccountExcelCell.class).tooltipMessage();
        }
        if (field.getAnnotation(ChartAccountExcelCell.class) != null) {
            toolTipMessage = field.getAnnotation(ChartAccountExcelCell.class).tooltipMessage();
        }
        if (field.getAnnotation(JournalExcelCell.class) != null) {
            toolTipMessage = field.getAnnotation(JournalExcelCell.class).tooltipMessage();
        }
        if (field.getAnnotation(AccountExcelCell.class) != null) {
            toolTipMessage = field.getAnnotation(AccountExcelCell.class).tooltipMessage();
        }
        if (field.getAnnotation(AccountingTemplateExcelCell.class) != null) {
            toolTipMessage = field.getAnnotation(AccountingTemplateExcelCell.class).tooltipMessage();
        }
        if (toolTipMessage != null) {
            return toolTipMessage;
        }
        throw new HttpCustomException(ApiErrors.Accounting.FIELD_NOT_ANNOTATED_WITH_EXCEL_CELL);
    }

    public static int createHeaderCell(String fileName, int cellIndex, Row header, String headerName) {
        if (fileName.contains(SIMULATION_EXPORT_FILE_NAME)) {
            Cell headerCell = header.createCell(cellIndex);
            headerCell.setCellValue(headerName);
            headerCell.setCellStyle(getErrorHeaderStyle(header.getSheet().getWorkbook()));
            cellIndex++;
        }
        return cellIndex;
    }

    private static void initDataInRows(Iterable<?> data, Sheet sheet, Iterable<Field> excelHeaderFields) {
        int rowIndex = 1;
        for (Object dataItem : data) {
            int cellIndex = 0;
            Row row = sheet.createRow(rowIndex);
            for (Field field : excelHeaderFields) {
                createCellInRow(field, dataItem, row, cellIndex, sheet.getWorkbook());
                cellIndex++;
            }
            rowIndex++;
        }
    }

    @SuppressWarnings("squid:S2095")
    public static Workbook createWorkBookFromBase64String(String base64String) {
        /*
         * inflateRatio is set to 0 to allow the parsing of Excel files no matter what
         * the compression ratio is.
         */
        ZipSecureFile.setMinInflateRatio(0);
        if (StringUtils.isEmpty(base64String)) {
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_EMPTY_FILE);
        }
        byte[] bytesFromBase64String;
        try {
            bytesFromBase64String = Base64.getDecoder().decode(base64String);
        } catch (IllegalArgumentException e) {
            log.error("The excel file format is invalid", e);
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_INVALID_CONTENT_FORMAT);
        }
        InputStream fileContentAsInputStream = new ByteArrayInputStream(bytesFromBase64String);
        Workbook workbook;
        try {
            /*
             * Don't auto-close this resource since it's used in other methods, the closing
             * is done in the calling methods
             */
            workbook = WorkbookFactory.create(fileContentAsInputStream);
            if (workbook.getNumberOfSheets() == 0) {
                throw new HttpCustomException(ApiErrors.Accounting.EXCEL_EMPTY_FILE);
            }
            boolean allSheetsAreEmpty = true;
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                allSheetsAreEmpty &= sheet.getLastRowNum() == 0 && sheet.getRow(0) == null;
            }
            if (allSheetsAreEmpty) {
                log.error("Trying to import empty document");
                throw new HttpCustomException(ApiErrors.Accounting.EXCEL_EMPTY_FILE);
            } else {
                return workbook;
            }
        } catch (EncryptedDocumentException e) {
            log.error("There was a problem accessing the excel file since it's password protected ", e);
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_FILE_LOCKED_BY_PASSWORD);
        } catch (OldExcelFormatException e) {
            log.error("The excel file format is too old and not supported by Apache POI ", e);
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_OLD_FORMAT_NOT_SUPPORTED);
        } catch (POIXMLException e) {
            log.error("Strict Open XML Spreadsheet format isn't currently supported by Apache POI ", e);
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_OOXML_FORMAT_NOT_SUPPORTED);
        } catch (EmptyFileException e) {
            log.error("The supplied file was empty (zero bytes long) ", e);
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_EMPTY_FILE);
        } catch (IOException e) {
            log.error(AccountingConstants.ERROR_CREATING_FILE, e);
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_FILE_CREATION_FAIL);
        }
    }

    public static boolean isRowNotEmpty(Row row) {
        if (row == null) {
            return false;
        }
        DataFormatter dataFormatter = new DataFormatter();
        for (Cell cell : row) {
            if (!dataFormatter.formatCellValue(cell).trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSheetEmpty(Sheet sheet) {
        return sheet.getLastRowNum() == 0 && sheet.getRow(0) == null;
    }

    public static String getFileNamePrefix(String fileName) {
        LocalDateTime ldt = LocalDateTime.now();
        return fileName.replace(AccountingConstants.XLSX, StringUtils.EMPTY) + String.format(
                AccountingConstants.EXCEL_FILE_PREFIX_FORMAT, ldt.getDayOfMonth(), ldt.getMonthValue(), ldt.getYear());
    }

    public static void createCellInRow(Field field, Object dataItem, Row row, int cellIndex, Workbook wb) {
        try {
            boolean accessible = field.isAccessible();
            field.setAccessible(true);
            Cell cell = row.createCell(cellIndex);
            if (field.getType().isAssignableFrom(LocalDateTime.class)) {
                if (field.get(dataItem) != null) {
                    cell.setCellType(CellType.NUMERIC);
                    cell.setCellStyle(getDateCellStyleFromWorkbook(wb));
                    LocalDateTime documentDate = (LocalDateTime) field.get(dataItem);
                    setCellValueDocumentDate(cell, documentDate, field);
                }
            } else if (field.getType().isAssignableFrom(BigDecimal.class)) {
                if (field.get(dataItem) != null) {
                    cell.setCellStyle(getMoneyCellStyleFromWorkbook(wb));
                    cell.setCellValue(((BigDecimal) field.get(dataItem)).doubleValue());
                }
            } else if (field.getType().isAssignableFrom(Integer.class)) {
                if (field.get(dataItem) != null) {
                    cell.setCellStyle(getNumberCellStyleFromWorkbook(wb));
                    cell.setCellValue(((Integer) field.get(dataItem)));
                }
            } else {
                cell.setCellStyle(getDefaultCellStyleFromWorkbook(wb));
                cell.setCellValue(Objects.toString(field.get(dataItem), StringUtils.EMPTY));
            }
            field.setAccessible(accessible);
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
            log.error(
                    "There was an error trying to use Reflection on the field {} on the entity ChartAccountXLSXFormatDto",
                    field.getName(), e);
        }
    }

    public static boolean isLabelInCellValid(Cell cell) {
        String label = dataFormatter.formatCellValue(cell).trim();
        if (label.length() < ENTITY_DEFAULT_LABEL_MIN_LENGTH || label.length() > ENTITY_DEFAULT_LABEL_MAX_LENGTH) {
            setInvalidCell(cell, String.format(XLSXErrors.LENGTH_INVALID, ENTITY_DEFAULT_LABEL_MIN_LENGTH,
                    ENTITY_DEFAULT_LABEL_MAX_LENGTH));
            return false;
        } else {
            return true;
        }
    }

    public static void validateWorkbookSheetsHeaders(Workbook workbook, List<String> acceptedHeaders) {
        for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            log.info("Validating sheet {} header", sheet.getSheetName());
            if (headerRow.getRowNum() != 0) {
                throw new HttpCustomException(ApiErrors.Accounting.EXCEL_HEADER_SHOULD_BE_IN_FIRST_ROW,
                        new ErrorsResponse().error(headerRow.getSheet().getSheetName()));
            }
            if (headerRow.getFirstCellNum() != 0) {
                throw new HttpCustomException(ApiErrors.Accounting.EXCEL_HEADER_SHOULD_START_IN_FIRST_CELL,
                        new ErrorsResponse().error(headerRow.getSheet().getSheetName()));
            }
            List<String> inputFileHeader = new ArrayList<>();
            for (Cell headerCell : headerRow) {
                inputFileHeader.add(headerCell.getStringCellValue());
            }
            inputFileHeader.removeIf(header -> header.equals(ERRORS_HEADER_NAME) || header.isEmpty());
            if (!inputFileHeader.equals(acceptedHeaders)) {
                throw new HttpCustomException(ApiErrors.Accounting.EXCEL_INVALID_HEADERS,
                        new ErrorsResponse().error(headerRow.getSheet().getSheetName()));
            }
        }
    }

    public static void validateNumberOfCellsInRowAgainstHeaders(Row row, int maxNumberOfCells) {
        log.info("Validating cells against headers");
        if (row.getLastCellNum() > maxNumberOfCells) {
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_INVALID_ROW,
                    new ErrorsResponse().error(row.getSheet().getSheetName()).error(row.getRowNum() + 1));
        }
    }

    public static byte[] convertFileToByteArray(File file) {
        try (InputStream input = new FileInputStream(file)) {
            return IOUtils.toByteArray(input);
        } catch (FileNotFoundException e) {
            log.error("File not found for {}", file.getName(), e);
        } catch (IOException e) {
            log.error("Couldn't parse inputStream", e);
        }
        return new byte[] {};
    }

    public static void deleteExcelFile(String fileName, File excelStoragePath) {
        File[] files = Objects.requireNonNull(excelStoragePath.listFiles((dir, name) -> name.equals(fileName)));
        for (File file : files) {
            try {
                Files.delete(file.toPath());
                log.info("temporary excel upload file with name {} deleted", fileName);
            } catch (IOException e) {
                log.error("Could not delete temporary temporary excel upload file with name {} . {}", fileName, e);
            }
        }
    }

    public static byte[] getExcelFile(String fileName, File excelStoragePath) {
        if (excelStoragePath != null) {
            File[] files = excelStoragePath.listFiles((dir, name) -> name.equals(fileName));
            if (files != null && files.length != 0) {
                return convertFileToByteArray(files[0]);
            } else {
                log.error("Excel file not found with name {} not found, it may have been manually deleted", fileName);
                throw new HttpCustomException(ApiErrors.Accounting.EXCEL_FILE_NOT_FOUND);
            }
        } else {
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_NO_FOLDER_FOR_GENERATED_REPORTS);
        }
    }

    public static FileUploadDto getFileUploadDtoFromWorkbook(Workbook wb, File excelStoragePath, String fileName) {
        LocalDateTime ldt = LocalDateTime.now();
        String excelFilePrefix = fileName.replace(AccountingConstants.XLSX, StringUtils.EMPTY) + String.format(
                AccountingConstants.EXCEL_FILE_PREFIX_FORMAT, ldt.getDayOfMonth(), ldt.getMonthValue(), ldt.getYear());
        File excelFile;
        excelFile = createTempFile(excelStoragePath, excelFilePrefix);
        try (OutputStream outputStream = new FileOutputStream(excelFile)) {
            wb.write(outputStream);
            FileUploadDto fileUploadDto = new FileUploadDto();
            fileUploadDto.setName(excelFile.getName());
            fileUploadDto.setBase64Content(Base64.getEncoder().encodeToString(Files.readAllBytes(excelFile.toPath())));
            return fileUploadDto;
        } catch (FileNotFoundException e) {
            log.error("File not found", e);
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_FILE_NOT_FOUND);
        } catch (IOException e) {
            log.error(AccountingConstants.ERROR_CREATING_FILE, e);
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_FILE_CREATION_FAIL);
        }
    }

    public static File createTempFile(File excelStoragePath, String excelFilePrefix) {
        File excelFile;
        try {
            excelFile = File.createTempFile(excelFilePrefix, AccountingConstants.XLSX, excelStoragePath);
        } catch (IllegalArgumentException | SecurityException | IOException e) {
            log.error(AccountingConstants.ERROR_CREATING_FILE, e);
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_FILE_CREATION_FAIL);
        }
        return excelFile;
    }

    private static void setCellValueDocumentDate(Cell cell, LocalDateTime documentDate, Field field) {
        try {
            cell.setCellValue(new SimpleDateFormat(DD_MM_YYYY)
                    .parse(documentDate.format(DateTimeFormatter.ofPattern(DD_MM_YYYY))));
        } catch (ParseException e) {
            log.error("There was an error parsing the LocalDateTime object", e);
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_ERROR_PARSING_LOCAL_DATE_OBJECT,
                    new ErrorsResponse().error(field.getName()));
        }
    }
}
