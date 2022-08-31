package fr.sparkit.accounting.services.utils.excel;

import static fr.sparkit.accounting.constants.XLSXErrors.INVALID_CELL_FORMAT;

import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;
import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.constants.NumberConstant;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ExcelCellStyleHelper {

    private static CellStyle headerStyle;
    private static CellStyle darkerHeaderStyle;
    private static CellStyle invalidCellStyle;
    private static CellStyle dateCellStyle;
    private static CellStyle numberCellStyle;
    private static CellStyle defaultCellStyle;
    private static CellStyle moneyCellStyle;

    private ExcelCellStyleHelper() {
        super();
    }

    public static CellStyle getDateCellStyleFromWorkbook(Workbook workbook) {
        if (dateCellStyle == null) {
            dateCellStyle = workbook.createCellStyle();
        }
        dateCellStyle.setDataFormat(
                workbook.getCreationHelper().createDataFormat().getFormat(AccountingConstants.DD_MM_YYYY));
        dateCellStyle.setFont(workbook.createFont());
        setDefaultBorders(dateCellStyle);
        dateCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        dateCellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        dateCellStyle.setAlignment(HorizontalAlignment.LEFT);
        return dateCellStyle;
    }

    public static CellStyle getNumberCellStyleFromWorkbook(Workbook workbook) {
        if (numberCellStyle == null) {
            numberCellStyle = workbook.createCellStyle();
        }
        numberCellStyle
                .setDataFormat(HSSFDataFormat.getBuiltinFormat(AccountingConstants.APACHE_POI_BUILT_IN_INTEGER_FORMAT));
        numberCellStyle.setFont(workbook.createFont());
        setDefaultBorders(numberCellStyle);
        numberCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        numberCellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        numberCellStyle.setAlignment(HorizontalAlignment.LEFT);
        return numberCellStyle;
    }

    public static CellStyle getMoneyCellStyleFromWorkbook(Workbook workbook) {
        if (moneyCellStyle == null) {
            moneyCellStyle = workbook.createCellStyle();
        }
        moneyCellStyle.setDataFormat(workbook.createDataFormat().getFormat(AccountingConstants.EXCEL_EXPORT_FORMAT));
        moneyCellStyle.setFont(workbook.createFont());
        setDefaultBorders(moneyCellStyle);
        moneyCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        moneyCellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        moneyCellStyle.setAlignment(HorizontalAlignment.RIGHT);
        return moneyCellStyle;
    }

    public static void setDefaultBorders(CellStyle cellStyle) {
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
    }

    public static void setCellErrorStyle(Cell cell) {
        Workbook wb = cell.getSheet().getWorkbook();
        if (invalidCellStyle == null) {
            invalidCellStyle = cell.getSheet().getWorkbook().createCellStyle();
        }
        Font font = wb.createFont();
        font.setBold(false);
        font.setColor(IndexedColors.RED1.getIndex());
        invalidCellStyle.setFont(font);
        font.setBold(false);
    }

    public static void setComment(Cell cell, String commentString) {
        CreationHelper factory = cell.getSheet().getWorkbook().getCreationHelper();
        ClientAnchor anchor = factory.createClientAnchor();
        anchor.setCol1(cell.getColumnIndex());
        anchor.setCol2(cell.getColumnIndex() + NumberConstant.THREE);
        anchor.setRow1(cell.getRow().getRowNum());
        anchor.setRow2(cell.getRow().getRowNum() + NumberConstant.THREE);
        Drawing drawing = cell.getSheet().createDrawingPatriarch();
        Comment comment = cell.getCellComment();
        if (comment == null) {
            comment = drawing.createCellComment(anchor);
        }
        comment.setString(factory.createRichTextString(commentString));
        cell.setCellComment(comment);
    }

    public static void setInvalidCell(Cell cell, String invalidCellMessage) {
        ExcelCellStyleHelper.setCellErrorStyle(cell);
        setComment(cell, invalidCellMessage);
        logInvalidCell(cell, invalidCellMessage);
    }

    public static void logInvalidCell(Cell cell, String invalidCellMessage) {
        log.error(INVALID_CELL_FORMAT, invalidCellMessage, cell.getColumnIndex() + 1, cell.getRowIndex() + 1,
                cell.getSheet().getSheetName());
    }

    public static CellStyle getErrorHeaderStyle(Workbook workbook) {
        CellStyle errorHeaderStyle = getHeaderStyle(workbook);
        errorHeaderStyle.setFillForegroundColor(IndexedColors.RED1.getIndex());
        return errorHeaderStyle;
    }

    public static CellStyle getDarkerHeaderStyle(Workbook workbook) {
        if (darkerHeaderStyle == null) {
            darkerHeaderStyle = workbook.createCellStyle();
        }
        setDefaultBorders(darkerHeaderStyle);
        darkerHeaderStyle.setFillForegroundColor(HSSFColor.HSSFColorPredefined.GREY_40_PERCENT.getIndex());
        darkerHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        darkerHeaderStyle.setFont(font);
        return darkerHeaderStyle;
    }

    public static CellStyle getDefaultCellStyleFromWorkbook(Workbook workbook) {
        if (defaultCellStyle == null) {
            defaultCellStyle = workbook.createCellStyle();
        }
        defaultCellStyle.setFont(workbook.createFont());
        setDefaultBorders(defaultCellStyle);
        defaultCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        defaultCellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        defaultCellStyle.setAlignment(HorizontalAlignment.LEFT);
        return defaultCellStyle;
    }

    public static CellStyle getHeaderStyle(Workbook workbook) {
        if (headerStyle == null) {
            headerStyle = workbook.createCellStyle();
        }
        setDefaultBorders(headerStyle);
        headerStyle.setFillForegroundColor(HSSFColor.HSSFColorPredefined.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        return headerStyle;
    }

    public static void resetStyles() {
        headerStyle = null;
        invalidCellStyle = null;
        dateCellStyle = null;
        numberCellStyle = null;
        defaultCellStyle = null;
        moneyCellStyle = null;
    }

}
