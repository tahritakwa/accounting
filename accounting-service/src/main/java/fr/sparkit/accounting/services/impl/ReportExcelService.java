package fr.sparkit.accounting.services.impl;

import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.constants.LanguageConstants;
import fr.sparkit.accounting.convertor.FiscalYearConvertor;
import fr.sparkit.accounting.dto.*;
import fr.sparkit.accounting.entities.FiscalYear;
import fr.sparkit.accounting.enumuration.ReportType;
import fr.sparkit.accounting.enumuration.excel.reports.*;
import fr.sparkit.accounting.services.*;
import fr.sparkit.accounting.services.utils.TraductionServiceUtil;
import fr.sparkit.accounting.services.utils.excel.GenericExcelPOIHelper;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static fr.sparkit.accounting.constants.AccountingConstants.*;
import static fr.sparkit.accounting.constants.AccountingConstants.ExcelReportTemplatesCodes.*;
import static fr.sparkit.accounting.util.CalculationUtil.getBigDecimalValueFromFormattedString;

@Service
@Slf4j
public class ReportExcelService implements IReportExcelService {

    private static final String ANNEX_SUFFIX = "-annex";
    private static final String EXCEL_REPORT_TEMPLATES_FOLDER = "/excel-reports-templates/";
    private static final DataFormatter dataFormatter = new DataFormatter();
    private static final int FOOTER_HEIGHT = 3;
    private static final int TRIAL_BALANCE_TABLE_HEADER_ROW_INDEX = 5;
    private static final int DEFAULT_TABLE_HEADER_ROW_INDEX = 4;
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter
            .ofPattern(AccountingConstants.DD_MM_YYYY);
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(AccountingConstants.HH_MM);
    private final IReportLineService reportLineService;
    private final IFiscalYearService fiscalYearService;
    private final ITrialBalanceService trialBalanceService;
    private final IGeneralLedgerService generalLedgerService;
    private final IStateOfAuxiliaryJournalService stateOfAuxiliaryJournalService;
    private final IJournalService journalService;
    private final IJournalStateService journalStateService;
    @Value("${accounting.excel.storage-directory}")
    private Path excelStoragePath;

    @Autowired
    public ReportExcelService(IReportLineService reportLineService, IFiscalYearService fiscalYearService,
                              ITrialBalanceService trialBalanceService, IGeneralLedgerService generalLedgerService,
                              IStateOfAuxiliaryJournalService stateOfAuxiliaryJournalService, IJournalService journalService,
                              IJournalStateService journalStateService) {
        this.reportLineService = reportLineService;
        this.fiscalYearService = fiscalYearService;
        this.trialBalanceService = trialBalanceService;
        this.generalLedgerService = generalLedgerService;
        this.stateOfAuxiliaryJournalService = stateOfAuxiliaryJournalService;
        this.journalService = journalService;
        this.journalStateService = journalStateService;
    }

    private static void fillRowAnnualReport(ReportTemplateDefaultParameters reportParams, FiscalYearDto fiscalYear,
                                            FiscalYear previousFiscalYear, List<ReportLineDto> reportLines, List<String> keys, Row row) {
        if (GenericExcelPOIHelper.isRowNotEmpty(row)) {
            for (FinancialReportExcelHeader financialReportExcelHeader : FinancialReportExcelHeader
                    .values()) {
                int reportColumnIndex = CellReference
                        .convertColStringToIndex(financialReportExcelHeader.getColumnIndexCode());
                Cell cell = row.getCell(reportColumnIndex);
                if (cell != null) {
                    cell.setCellType(CellType.STRING);
                    String templatePlaceHolderValue = dataFormatter.formatCellValue(cell).trim();
                    if (keys.contains(templatePlaceHolderValue)) {
                        ReportLineDto reportLine = reportLines
                                .get(keys.indexOf(templatePlaceHolderValue));
                        setupFinancialReportData(financialReportExcelHeader, cell, reportLine);
                    } else {
                        setupTemplatePlaceholderValues(reportParams, fiscalYear.getStartDate(),
                                fiscalYear.getEndDate(), fiscalYear,
                                FiscalYearConvertor.modelToDto(previousFiscalYear), cell);
                    }
                }
            }
        }
    }

    private static void setupTemplatePlaceholders(ReportTemplateDefaultParameters reportParams,
                                                  LocalDateTime startDate, LocalDateTime endDate, FiscalYearDto currentFiscalYear,
                                                  FiscalYearDto previousFiscalYear, Sheet sheet, List<String> headerColumnCodes) {
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            setupTemplatePlaceHoldersForRow(reportParams, startDate, endDate, currentFiscalYear,
                    previousFiscalYear, headerColumnCodes, row);
        }
    }

    private static void setupTemplatePlaceHoldersForRow(ReportTemplateDefaultParameters reportParams,
                                                        LocalDateTime startDate, LocalDateTime endDate, FiscalYearDto currentFiscalYear,
                                                        FiscalYearDto previousFiscalYear, List<String> headerColumnCodes, Row row) {
        if (GenericExcelPOIHelper.isRowNotEmpty(row)) {
            for (String headerColumnCode : headerColumnCodes) {
                int headerColumnIndex = CellReference.convertColStringToIndex(headerColumnCode);
                Cell cell = row.getCell(headerColumnIndex);
                if (cell != null) {
                    cell.setCellType(CellType.STRING);
                    setupTemplatePlaceholderValues(reportParams, startDate, endDate,
                            currentFiscalYear, previousFiscalYear, cell);
                }
            }
        }
    }

    private static void setupTemplatePlaceholderValues(ReportTemplateDefaultParameters reportParams,
                                                       LocalDateTime startDate, LocalDateTime endDate, FiscalYearDto currentFiscalYear,
                                                       FiscalYearDto previousFiscalYear, Cell cell) {
        String templatePlaceHolderValue = dataFormatter.formatCellValue(cell).trim();
        switch (templatePlaceHolderValue) {
            case GENERATION_DATE_EXCEL_MODEL_CODE:
                cell.setCellValue(String.format(LanguageConstants.GENERATION_DATE,
                        reportParams.getGenerationDate().format(dateFormatter)));
                break;
            case GENERATION_TIME_EXCEL_MODEL_CODE:
                cell.setCellValue(String.format(LanguageConstants.GENERATION_TIME,
                        reportParams.getGenerationDate().format(timeFormatter)));
                break;
            case COMPANY_NAME_EXCEL_MODEL_CODE:
                cell.setCellValue(reportParams.getCompanyName());
                break;
            case PROVISIONAL_EDITION_EXCEL_MODEL_CODE:
                if (reportParams.isProvisionalEdition()) {
                    cell.setCellValue(LanguageConstants.PROVISIONAL_EDITION);
                } else {
                    cell.setCellValue(StringUtils.EMPTY);
                }
                break;
            case COMPANY_ADDRESS_INFO_EXCEL_MODEL_CODE:
                cell.setCellValue(reportParams.getCompanyAdressInfo());
                cell.getCellStyle().setWrapText(true);
                break;
            case FISCAL_YEAR_DATE_EXCEL_MODEL_CODE:
                cell.setCellValue(
                        String.format(LanguageConstants.FISCAL_YEAR_DATE, startDate.format(dateFormatter),
                                endDate.format(dateFormatter)));
                break;
            case CURRENT_FISCAL_YEAR_EXCEL_MODEL_CODE:
                cell.setCellValue(currentFiscalYear.getName());
                break;
            case PREVIOUS_FISCAL_YEAR_EXCEL_MODEL_CODE:
                if (previousFiscalYear != null) {
                    cell.setCellValue(previousFiscalYear.getName());
                } else {
                    cell.setCellValue(AccountingConstants.NO_FISCAL_YEAR_LABEL_PLACEHOLDER);
                }
                break;
            default:
                break;
        }
    }

    private static void setupReportFooter(Sheet sheet, int indexRowIndex, String footerFirstColumnCode,
                                          ReportTemplateDefaultParameters reportParams) {
        ResourceBundle i18nResourceBundle = TraductionServiceUtil.getI18nResourceBundle();

        Row footerFirstRow = sheet.createRow(indexRowIndex + 1);
        Row footerSecondRow = sheet.createRow(indexRowIndex + 2);
        Row footerThirdRow = sheet.createRow(indexRowIndex + 3);

        Cell fiscalNumberCell = footerFirstRow
                .createCell(CellReference.convertColStringToIndex(footerFirstColumnCode));
        Cell fiscalNumberCellInfo = footerFirstRow
                .createCell(CellReference.convertColStringToIndex(footerFirstColumnCode) + 1);
        Cell phoneCell = footerFirstRow
                .createCell(CellReference.convertColStringToIndex(footerFirstColumnCode) + 2);
        Cell phoneCellInfo = footerFirstRow
                .createCell(CellReference.convertColStringToIndex(footerFirstColumnCode) + 3);
        Cell RC = footerFirstRow.createCell(CellReference.convertColStringToIndex(footerFirstColumnCode) + 4);
        Cell RCInfo = footerFirstRow
                .createCell(CellReference.convertColStringToIndex(footerFirstColumnCode) + 5);
        Cell companyAddress = footerSecondRow
                .createCell(CellReference.convertColStringToIndex(footerFirstColumnCode));
        Cell companyAddressInfo = footerSecondRow
                .createCell(CellReference.convertColStringToIndex(footerFirstColumnCode) + 1);
        Cell companyEmail = footerSecondRow
                .createCell(CellReference.convertColStringToIndex(footerFirstColumnCode) + 2);
        Cell companyEmailInfo = footerSecondRow
                .createCell(CellReference.convertColStringToIndex(footerFirstColumnCode) + 3);
        Cell companyWebSite = footerThirdRow
                .createCell(CellReference.convertColStringToIndex(footerFirstColumnCode) + 2);
        Cell companyWebSiteInfo = footerThirdRow
                .createCell(CellReference.convertColStringToIndex(footerFirstColumnCode) + 3);

        fiscalNumberCell.setCellValue(i18nResourceBundle.getString(FISCAL_NUMBER_PARAM) + " :");
        fiscalNumberCellInfo.setCellValue(reportParams.getMatriculeFisc());
        phoneCell.setCellValue(i18nResourceBundle.getString(COMPANY_PHONE_NUMBER_PARAM) + " :");
        phoneCellInfo.setCellValue(reportParams.getTel());
        RC.setCellValue(i18nResourceBundle.getString(COMPANY_RC_PARAM) + " :");
        RCInfo.setCellValue(reportParams.getCommercialRegister());
        companyAddress.setCellValue(i18nResourceBundle.getString(COMPANY_ADDRESS_PARAM) + " :");
        companyAddressInfo.setCellValue(reportParams.getCompanyAdressInfo());
        companyEmail.setCellValue(i18nResourceBundle.getString(COMPANY_EMAIL_PARAM) + " :");
        companyEmailInfo.setCellValue(reportParams.getMail());
        companyWebSite.setCellValue(i18nResourceBundle.getString(COMPANY_WEB_SITE_PARAM) + " :");
        companyWebSiteInfo.setCellValue(reportParams.getWebSite());
    }

    private static void setupFinancialReportData(FinancialReportExcelHeader financialReportExcelHeader, Cell cell,
                                                 ReportLineDto reportLine) {

        switch (financialReportExcelHeader) {
            case LABEL:
                cell.setCellValue(reportLine.getLabel());
                break;
            case ANNEX:
                cell.setCellValue(reportLine.getAnnexCode());
                break;
            case CURRENT_FISCAL_YEAR:
                setUpMonetaryCellValue(cell, reportLine.getAmount(), reportLine.isTotal());
                break;
            case PREVIOUS_FISCAL_YEAR:
                setUpMonetaryCellValue(cell, reportLine.getPreviousFiscalYearAmount(), reportLine.isTotal());
                break;
            default:
                break;
        }
    }

    private static void setupFinancialReportAnnexData(ReportTemplateDefaultParameters reportParams, Sheet sheet,
                                                      List<AnnexeReportDto> annexReportLines) {
        int indexRowIndex = DEFAULT_TABLE_HEADER_ROW_INDEX;
        CellStyle verticalBorderStyle = sheet.getWorkbook().createCellStyle();
        verticalBorderStyle.setBorderLeft(BorderStyle.MEDIUM);
        verticalBorderStyle.setBorderRight(BorderStyle.MEDIUM);

        CellStyle verticalAndBottomBorderStyle = sheet.getWorkbook().createCellStyle();
        verticalAndBottomBorderStyle.cloneStyleFrom(verticalBorderStyle);
        verticalAndBottomBorderStyle.setBorderBottom(BorderStyle.MEDIUM);
        Iterator<AnnexeReportDto> iterator = annexReportLines.iterator();
        while (iterator.hasNext()) {
            AnnexeReportDto reportLine = iterator.next();
            Row row = sheet.createRow(indexRowIndex);
            for (FinancialReportAnnexExcelHeader financialReportAnnexExcelHeader : FinancialReportAnnexExcelHeader
                    .values()) {
                row.createCell(CellReference
                        .convertColStringToIndex(financialReportAnnexExcelHeader.getColumnIndexCode()));
            }

            Cell annexCell = row.getCell(CellReference
                    .convertColStringToIndex(FinancialReportAnnexExcelHeader.ANNEX.getColumnIndexCode()));
            annexCell.setCellValue(reportLine.getAnnexe());
            annexCell.setCellStyle(verticalBorderStyle);
            Cell labelCell = row.getCell(CellReference
                    .convertColStringToIndex(FinancialReportAnnexExcelHeader.LABEL.getColumnIndexCode()));
            labelCell.setCellValue(reportLine.getLabel());
            labelCell.setCellStyle(verticalBorderStyle);

            Cell accountCodeCell = row.getCell(CellReference.convertColStringToIndex(
                    FinancialReportAnnexExcelHeader.ACCOUNT_CODE.getColumnIndexCode()));
            accountCodeCell.setCellValue(reportLine.getCodeAccount());
            accountCodeCell.setCellStyle(verticalBorderStyle);

            Cell accountLabelCell = row.getCell(CellReference.convertColStringToIndex(
                    FinancialReportAnnexExcelHeader.ACCOUNT_LABEL.getColumnIndexCode()));
            accountLabelCell.setCellValue(reportLine.getLabelAccount());
            accountLabelCell.setCellStyle(verticalBorderStyle);

            Cell debitCell = row.getCell(CellReference
                    .convertColStringToIndex(FinancialReportAnnexExcelHeader.DEBIT.getColumnIndexCode()));
            debitCell.setCellStyle(verticalBorderStyle);
            setUpMonetaryCellValue(debitCell,
                    getBigDecimalValueFromFormattedString(reportLine.getTotalDebit()));

            Cell creditCell = row.getCell(CellReference
                    .convertColStringToIndex(FinancialReportAnnexExcelHeader.CREDIT.getColumnIndexCode()));
            creditCell.setCellStyle(verticalBorderStyle);
            setUpMonetaryCellValue(creditCell,
                    getBigDecimalValueFromFormattedString(reportLine.getTotalCredit()));
            indexRowIndex++;
            if (!iterator.hasNext()) {
                annexCell.setCellStyle(verticalAndBottomBorderStyle);
                labelCell.setCellStyle(verticalAndBottomBorderStyle);
                accountCodeCell.setCellStyle(verticalAndBottomBorderStyle);
                accountLabelCell.setCellStyle(verticalAndBottomBorderStyle);
                debitCell.setCellStyle(verticalAndBottomBorderStyle);
                creditCell.setCellStyle(verticalAndBottomBorderStyle);
                break;
            }
        }
        setupReportFooter(sheet, indexRowIndex, FinancialReportAnnexExcelHeader.ANNEX.getColumnIndexCode(),
                reportParams);
    }

    private static void setupTrialBalanceReportData(ReportTemplateDefaultParameters reportParams, Workbook workbook,
                                                    Sheet sheet, List<TrialBalanceReportLineDto> trialBalanceReportLines) {
        int indexRowIndex = TRIAL_BALANCE_TABLE_HEADER_ROW_INDEX;
        CellStyle cellDefaultStyle = getDefaultCellStyle(workbook);
        CellStyle grayedCellStyle = getGrayedCellStyle(workbook, cellDefaultStyle);
        Iterator<TrialBalanceReportLineDto> iterator = trialBalanceReportLines.iterator();
        while (iterator.hasNext()) {
            TrialBalanceReportLineDto trialBalanceReportLine = iterator.next();
            Row row = sheet.createRow(indexRowIndex);
            for (TrialBalanceReportExcelHeader trialBalanceReportExcelHeader : TrialBalanceReportExcelHeader
                    .values()) {
                row.createCell(CellReference
                        .convertColStringToIndex(trialBalanceReportExcelHeader.getColumnIndexCode()));
            }

            Cell accountCodeCell = row.getCell(CellReference.convertColStringToIndex(
                    TrialBalanceReportExcelHeader.ACCOUNT_CODE.getColumnIndexCode()));
            accountCodeCell.setCellStyle(cellDefaultStyle);

            Cell accountLabelCell = row.getCell(CellReference.convertColStringToIndex(
                    TrialBalanceReportExcelHeader.ACCOUNT_LABEL.getColumnIndexCode()));
            accountLabelCell.setCellStyle(cellDefaultStyle);

            Cell cumulativeDebitCell = row.getCell(CellReference.convertColStringToIndex(
                    TrialBalanceReportExcelHeader.CUMULATIVE_PERIOD_DEBIT.getColumnIndexCode()));
            cumulativeDebitCell.setCellStyle(cellDefaultStyle);

            Cell cumulativeCreditCell = row.getCell(CellReference.convertColStringToIndex(
                    TrialBalanceReportExcelHeader.CUMULATIVE_PERIOD_CREDIT.getColumnIndexCode()));
            cumulativeCreditCell.setCellStyle(cellDefaultStyle);

            Cell periodDebitCell = row.getCell(CellReference.convertColStringToIndex(
                    TrialBalanceReportExcelHeader.PERIOD_DEBIT.getColumnIndexCode()));
            periodDebitCell.setCellStyle(cellDefaultStyle);

            Cell periodCreditCell = row.getCell(CellReference.convertColStringToIndex(
                    TrialBalanceReportExcelHeader.PERIOD_CREDIT.getColumnIndexCode()));
            periodCreditCell.setCellStyle(cellDefaultStyle);

            indexRowIndex++;
            if (isTrialBalanceReportLineHighlighted(trialBalanceReportLine.getAccountLabel())) {
                accountCodeCell.setCellStyle(grayedCellStyle);
                accountLabelCell.setCellStyle(grayedCellStyle);
                cumulativeDebitCell.setCellStyle(grayedCellStyle);
                cumulativeCreditCell.setCellStyle(grayedCellStyle);
                periodDebitCell.setCellStyle(grayedCellStyle);
                periodCreditCell.setCellStyle(grayedCellStyle);
            }

            accountCodeCell.setCellValue(trialBalanceReportLine.getCode());
            accountLabelCell.setCellValue(trialBalanceReportLine.getAccountLabel());
            setUpMonetaryCellValue(cumulativeDebitCell,
                    getBigDecimalValueFromFormattedString(trialBalanceReportLine.getAccumulatedDebit()));
            setUpMonetaryCellValue(cumulativeCreditCell,
                    getBigDecimalValueFromFormattedString(trialBalanceReportLine.getAccumulatedCredit()));
            setUpMonetaryCellValue(periodDebitCell,
                    getBigDecimalValueFromFormattedString(trialBalanceReportLine.getBalanceDebit()),
                    AccountingConstants.TRIAL_BALANCE_TOTAL
                            .equals(trialBalanceReportLine.getAccountLabel()));
            setUpMonetaryCellValue(periodCreditCell,
                    getBigDecimalValueFromFormattedString(trialBalanceReportLine.getBalanceCredit()),
                    AccountingConstants.TRIAL_BALANCE_TOTAL
                            .equals(trialBalanceReportLine.getAccountLabel()));
        }
        setupReportFooter(sheet, indexRowIndex, TrialBalanceReportExcelHeader.ACCOUNT_CODE.getColumnIndexCode(),
                reportParams);
    }

    private static void setupGeneralLedgerReportData(ReportTemplateDefaultParameters reportParams, Sheet sheet,
                                                     List<GeneralLedgerReportLineDto> generalLedgerReportLines) {
        int indexRowIndex = DEFAULT_TABLE_HEADER_ROW_INDEX;
        CellStyle cellDefaultStyle = getDefaultCellStyle(sheet.getWorkbook());
        CellStyle grayedCellStyle = getGrayedCellStyle(sheet.getWorkbook(), cellDefaultStyle);
        CellStyle boldCellStyle = getBoldCellStyle(sheet.getWorkbook(), cellDefaultStyle);
        Iterator<GeneralLedgerReportLineDto> iterator = generalLedgerReportLines.iterator();
        while (iterator.hasNext()) {
            GeneralLedgerReportLineDto generalLedgerReportLine = iterator.next();
            Row row = sheet.createRow(indexRowIndex);
            for (GeneralLedgerReportExcelHeader generalLedgerReportExcelHeader : GeneralLedgerReportExcelHeader
                    .values()) {
                row.createCell(CellReference
                        .convertColStringToIndex(generalLedgerReportExcelHeader.getColumnIndexCode()));
            }

            Cell documentDateCell = row.getCell(CellReference
                    .convertColStringToIndex(GeneralLedgerReportExcelHeader.DATE.getColumnIndexCode()));
            documentDateCell.setCellStyle(cellDefaultStyle);

            Cell journalLabelCell = row.getCell(CellReference.convertColStringToIndex(
                    GeneralLedgerReportExcelHeader.JOURNAL_LABEL.getColumnIndexCode()));
            journalLabelCell.setCellStyle(cellDefaultStyle);

            Cell documentLabelCell = row.getCell(CellReference.convertColStringToIndex(
                    GeneralLedgerReportExcelHeader.DOCUMENT_LABEL.getColumnIndexCode()));
            documentLabelCell.setCellStyle(cellDefaultStyle);

            Cell documentCodeCell = row.getCell(CellReference.convertColStringToIndex(
                    GeneralLedgerReportExcelHeader.DOCUMENT_CODE.getColumnIndexCode()));
            documentCodeCell.setCellStyle(cellDefaultStyle);

            Cell debitCell = row.getCell(CellReference
                    .convertColStringToIndex(GeneralLedgerReportExcelHeader.DEBIT.getColumnIndexCode()));
            debitCell.setCellStyle(cellDefaultStyle);

            Cell creditCell = row.getCell(CellReference
                    .convertColStringToIndex(GeneralLedgerReportExcelHeader.CREDIT.getColumnIndexCode()));
            creditCell.setCellStyle(cellDefaultStyle);

            Cell amountCell = row.getCell(CellReference
                    .convertColStringToIndex(GeneralLedgerReportExcelHeader.AMOUNT.getColumnIndexCode()));
            amountCell.setCellStyle(cellDefaultStyle);

            if (isGeneralLedgerReportLineHighlighted(generalLedgerReportLine.getDocumentCode())) {
                documentCodeCell.setCellStyle(grayedCellStyle);
                debitCell.setCellStyle(grayedCellStyle);
                creditCell.setCellStyle(grayedCellStyle);
                amountCell.setCellStyle(grayedCellStyle);
            }

            if (generalLedgerReportLine.getAccountCode().isEmpty()) {
                documentDateCell.setCellValue(generalLedgerReportLine.getDocumentDate());
                journalLabelCell.setCellValue(generalLedgerReportLine.getJournal());
                documentLabelCell.setCellValue(generalLedgerReportLine.getLabel());
                documentCodeCell.setCellValue(generalLedgerReportLine.getDocumentCode());
                setUpMonetaryCellValue(debitCell,
                        getBigDecimalValueFromFormattedString(generalLedgerReportLine.getDebit()));
                setUpMonetaryCellValue(creditCell,
                        getBigDecimalValueFromFormattedString(generalLedgerReportLine.getCredit()));
                setUpMonetaryCellValue(amountCell,
                        getBigDecimalValueFromFormattedString(generalLedgerReportLine.getBalance()),
                        AccountingConstants.GENERAL_LEDGER_TOTAL
                                .equals(generalLedgerReportLine.getDocumentCode()));
            } else {
                sheet.addMergedRegion(new CellRangeAddress(indexRowIndex, indexRowIndex, CellReference
                        .convertColStringToIndex(
                                GeneralLedgerReportExcelHeader.DATE.getColumnIndexCode()), CellReference
                        .convertColStringToIndex(
                                GeneralLedgerReportExcelHeader.AMOUNT.getColumnIndexCode())));
                Row accountCodeRow = sheet.getRow(indexRowIndex);
                Cell accountCodeCell = accountCodeRow.getCell(CellReference.convertColStringToIndex(
                        GeneralLedgerReportExcelHeader.DATE.getColumnIndexCode()));
                accountCodeCell.setCellStyle(boldCellStyle);
                accountCodeCell.setCellValue(generalLedgerReportLine.getAccountCode());
            }
            indexRowIndex++;
        }
        setupReportFooter(sheet, indexRowIndex, GeneralLedgerReportExcelHeader.DATE.getColumnIndexCode(),
                reportParams);
    }

    private static void setupAuxiliaryJournalReportData(ReportTemplateDefaultParameters reportParams, Sheet sheet,
                                                        List<AuxiliaryJournalLineDto> auxiliaryJournalReportLines) {
        int indexRowIndex = DEFAULT_TABLE_HEADER_ROW_INDEX;
        CellStyle cellDefaultStyle = getDefaultCellStyle(sheet.getWorkbook());
        CellStyle grayedCellStyle = getGrayedCellStyle(sheet.getWorkbook(), cellDefaultStyle);
        CellStyle boldCellStyle = getBoldCellStyle(sheet.getWorkbook(), cellDefaultStyle);
        Iterator<AuxiliaryJournalLineDto> iterator = auxiliaryJournalReportLines.iterator();
        while (iterator.hasNext()) {
            AuxiliaryJournalLineDto auxiliaryJournalLine = iterator.next();
            Row row = sheet.createRow(indexRowIndex);
            for (AuxiliaryJournalReportExcelHeader auxiliaryJournalReportExcelHeader : AuxiliaryJournalReportExcelHeader
                    .values()) {
                row.createCell(CellReference.convertColStringToIndex(
                        auxiliaryJournalReportExcelHeader.getColumnIndexCode()));
            }

            Cell documentDateCell = row.getCell(CellReference.convertColStringToIndex(
                    AuxiliaryJournalReportExcelHeader.DOCUMENT_DATE.getColumnIndexCode()));
            documentDateCell.setCellStyle(cellDefaultStyle);

            Cell documentCodeCell = row.getCell(CellReference.convertColStringToIndex(
                    AuxiliaryJournalReportExcelHeader.DOCUMENT_CODE.getColumnIndexCode()));
            documentCodeCell.setCellStyle(cellDefaultStyle);

            Cell documentLineDateCell = row.getCell(CellReference.convertColStringToIndex(
                    AuxiliaryJournalReportExcelHeader.DOCUMENT_LINE_DATE.getColumnIndexCode()));
            documentLineDateCell.setCellStyle(cellDefaultStyle);

            Cell accountCodeCell = row.getCell(CellReference.convertColStringToIndex(
                    AuxiliaryJournalReportExcelHeader.ACCOUNT_CODE.getColumnIndexCode()));
            accountCodeCell.setCellStyle(cellDefaultStyle);

            Cell labelCell = row.getCell(CellReference
                    .convertColStringToIndex(AuxiliaryJournalReportExcelHeader.LABEL.getColumnIndexCode()));
            labelCell.setCellStyle(cellDefaultStyle);

            Cell debitCell = row.getCell(CellReference
                    .convertColStringToIndex(AuxiliaryJournalReportExcelHeader.DEBIT.getColumnIndexCode()));
            debitCell.setCellStyle(cellDefaultStyle);

            Cell creditCell = row.getCell(CellReference.convertColStringToIndex(
                    AuxiliaryJournalReportExcelHeader.CREDIT.getColumnIndexCode()));
            creditCell.setCellStyle(cellDefaultStyle);

            if (isJournalReportLineHighlighted(auxiliaryJournalLine.getLabel())) {
                labelCell.setCellStyle(grayedCellStyle);
                debitCell.setCellStyle(grayedCellStyle);
                creditCell.setCellStyle(grayedCellStyle);
            }

            if (auxiliaryJournalLine.getJournal().isEmpty()) {
                documentDateCell.setCellValue(auxiliaryJournalLine.getDocumentDate());
                documentCodeCell.setCellValue(auxiliaryJournalLine.getCode());
                documentLineDateCell.setCellValue(auxiliaryJournalLine.getDateDocumentLineDate());
                accountCodeCell.setCellValue(auxiliaryJournalLine.getAccountCode());
                labelCell.setCellValue(auxiliaryJournalLine.getLabel());
                setUpMonetaryCellValue(debitCell,
                        getBigDecimalValueFromFormattedString(auxiliaryJournalLine.getDebit()),
                        isJournalReportLineHighlighted(auxiliaryJournalLine.getLabel()));
                setUpMonetaryCellValue(creditCell,
                        getBigDecimalValueFromFormattedString(auxiliaryJournalLine.getCredit()),
                        isJournalReportLineHighlighted(auxiliaryJournalLine.getLabel()));
            } else {
                sheet.addMergedRegion(new CellRangeAddress(indexRowIndex, indexRowIndex, CellReference
                        .convertColStringToIndex(
                                AuxiliaryJournalReportExcelHeader.DOCUMENT_DATE.getColumnIndexCode()),
                        CellReference.convertColStringToIndex(
                                AuxiliaryJournalReportExcelHeader.CREDIT.getColumnIndexCode())));
                Row journalCodeRow = sheet.getRow(indexRowIndex);
                Cell journalCodeRowCell = journalCodeRow.getCell(CellReference.convertColStringToIndex(
                        AuxiliaryJournalReportExcelHeader.DOCUMENT_DATE.getColumnIndexCode()));
                journalCodeRowCell.setCellStyle(boldCellStyle);
                journalCodeRowCell.setCellValue(auxiliaryJournalLine.getJournal());
            }
            indexRowIndex++;
        }
        setupReportFooter(sheet, indexRowIndex,
                AuxiliaryJournalReportExcelHeader.DOCUMENT_DATE.getColumnIndexCode(), reportParams);
    }

    private static void setupCentralizingJournalReportData(ReportTemplateDefaultParameters reportParams,
                                                           Sheet sheet, List<CentralizingJournalReportLineDto> centralizingJournalReportLines) {
        int indexRowIndex = DEFAULT_TABLE_HEADER_ROW_INDEX;
        CellStyle cellDefaultStyle = getDefaultCellStyle(sheet.getWorkbook());
        CellStyle grayedCellStyle = getGrayedCellStyle(sheet.getWorkbook(), cellDefaultStyle);
        CellStyle boldCellStyle = getBoldCellStyle(sheet.getWorkbook(), cellDefaultStyle);
        Iterator<CentralizingJournalReportLineDto> iterator = centralizingJournalReportLines.iterator();
        while (iterator.hasNext()) {
            CentralizingJournalReportLineDto centralizingJournalReportLine = iterator.next();
            Row row = sheet.createRow(indexRowIndex);
            for (CentilizingJournalReportExcelHeader centilizingJournalReportExcelHeader : CentilizingJournalReportExcelHeader
                    .values()) {
                row.createCell(CellReference.convertColStringToIndex(
                        centilizingJournalReportExcelHeader.getColumnIndexCode()));
            }

            Cell accountCodeCell = row.getCell(CellReference.convertColStringToIndex(
                    CentilizingJournalReportExcelHeader.ACCOUNT_CODE.getColumnIndexCode()));
            accountCodeCell.setCellStyle(cellDefaultStyle);

            Cell accountLabelCell = row.getCell(CellReference.convertColStringToIndex(
                    CentilizingJournalReportExcelHeader.ACCOUNT_LABEL.getColumnIndexCode()));
            accountLabelCell.setCellStyle(cellDefaultStyle);

            Cell debitCell = row.getCell(CellReference.convertColStringToIndex(
                    CentilizingJournalReportExcelHeader.DEBIT.getColumnIndexCode()));
            debitCell.setCellStyle(cellDefaultStyle);

            Cell creditCell = row.getCell(CellReference.convertColStringToIndex(
                    CentilizingJournalReportExcelHeader.CREDIT.getColumnIndexCode()));
            creditCell.setCellStyle(cellDefaultStyle);

            if (isJournalReportLineHighlighted(centralizingJournalReportLine.getAccountLabel())) {
                accountLabelCell.setCellStyle(grayedCellStyle);
                debitCell.setCellStyle(grayedCellStyle);
                creditCell.setCellStyle(grayedCellStyle);
            }

            if (centralizingJournalReportLine.getJournalCode().isEmpty()) {
                accountCodeCell.setCellValue(centralizingJournalReportLine.getAccountCode());
                accountLabelCell.setCellValue(centralizingJournalReportLine.getAccountLabel());
                setUpMonetaryCellValue(debitCell, getBigDecimalValueFromFormattedString(
                        centralizingJournalReportLine.getTotalDebitAmount()),
                        isJournalReportLineHighlighted(
                                centralizingJournalReportLine.getAccountLabel()));
                setUpMonetaryCellValue(creditCell, getBigDecimalValueFromFormattedString(
                        centralizingJournalReportLine.getTotalCreditAmount()),
                        isJournalReportLineHighlighted(
                                centralizingJournalReportLine.getAccountLabel()));
            } else {
                sheet.addMergedRegion(new CellRangeAddress(indexRowIndex, indexRowIndex, CellReference
                        .convertColStringToIndex(
                                CentilizingJournalReportExcelHeader.ACCOUNT_CODE.getColumnIndexCode()),
                        CellReference.convertColStringToIndex(
                                CentilizingJournalReportExcelHeader.CREDIT.getColumnIndexCode())));
                Row journalCodeRow = sheet.getRow(indexRowIndex);
                Cell journalCodeRowCell = journalCodeRow.getCell(CellReference.convertColStringToIndex(
                        CentilizingJournalReportExcelHeader.ACCOUNT_CODE.getColumnIndexCode()));
                journalCodeRowCell.setCellStyle(boldCellStyle);
                journalCodeRowCell.setCellValue(centralizingJournalReportLine.getJournalCode());
            }
            indexRowIndex++;
        }
        setupReportFooter(sheet, indexRowIndex,
                CentilizingJournalReportExcelHeader.ACCOUNT_CODE.getColumnIndexCode(), reportParams);
    }

    private static void setupCentralizingJournalByMonthReportData(ReportTemplateDefaultParameters reportParams,
                                                                  Sheet sheet, List<CentralizingJournalByMonthReportLineDto> centralizingJournalByMonthReportLines) {
        int indexRowIndex = DEFAULT_TABLE_HEADER_ROW_INDEX;
        CellStyle cellDefaultStyle = getDefaultCellStyle(sheet.getWorkbook());
        CellStyle grayedCellStyle = getGrayedCellStyle(sheet.getWorkbook(), cellDefaultStyle);
        CellStyle grayedDarkerCellStyle = getGrayedDarkerCellStyle(sheet.getWorkbook(), cellDefaultStyle);
        Iterator<CentralizingJournalByMonthReportLineDto> iterator = centralizingJournalByMonthReportLines
                .iterator();
        while (iterator.hasNext()) {
            CentralizingJournalByMonthReportLineDto centralizingJournalByMonthReportLine = iterator.next();
            Row row = sheet.createRow(indexRowIndex);
            for (CentralizingJournalByMonthReportExcelHeader centralizingJournalByMonthReportExcelHeader : CentralizingJournalByMonthReportExcelHeader
                    .values()) {
                row.createCell(CellReference.convertColStringToIndex(
                        centralizingJournalByMonthReportExcelHeader.getColumnIndexCode()));
            }

            Cell journalCodeCell = row.getCell(CellReference.convertColStringToIndex(
                    CentralizingJournalByMonthReportExcelHeader.JOURNAL_CODE.getColumnIndexCode()));
            journalCodeCell.setCellStyle(cellDefaultStyle);

            Cell journalLabelCell = row.getCell(CellReference.convertColStringToIndex(
                    CentralizingJournalByMonthReportExcelHeader.JOURNAL_LABEL.getColumnIndexCode()));
            journalLabelCell.setCellStyle(cellDefaultStyle);

            Cell monthCell = row.getCell(CellReference.convertColStringToIndex(
                    CentralizingJournalByMonthReportExcelHeader.MONTH.getColumnIndexCode()));
            monthCell.setCellStyle(cellDefaultStyle);

            Cell accountCodeCell = row.getCell(CellReference.convertColStringToIndex(
                    CentralizingJournalByMonthReportExcelHeader.ACCOUNT_CODE.getColumnIndexCode()));
            accountCodeCell.setCellStyle(cellDefaultStyle);

            Cell accountLabelCell = row.getCell(CellReference.convertColStringToIndex(
                    CentralizingJournalByMonthReportExcelHeader.ACCOUNT_LABEL.getColumnIndexCode()));
            accountLabelCell.setCellStyle(cellDefaultStyle);

            Cell debitCell = row.getCell(CellReference.convertColStringToIndex(
                    CentralizingJournalByMonthReportExcelHeader.DEBIT.getColumnIndexCode()));
            debitCell.setCellStyle(cellDefaultStyle);

            Cell creditCell = row.getCell(CellReference.convertColStringToIndex(
                    CentralizingJournalByMonthReportExcelHeader.CREDIT.getColumnIndexCode()));
            creditCell.setCellStyle(cellDefaultStyle);

            if (centralizingJournalByMonthReportLine.getJournalLabel()
                    .startsWith(LanguageConstants.GENERAL_TOTAL) || centralizingJournalByMonthReportLine
                    .getJournalLabel().startsWith(LanguageConstants.TOTAL_JOURNAL)
                    || centralizingJournalByMonthReportLine.getJournalLabel()
                    .startsWith(LanguageConstants.JOURNAL)) {
                journalCodeCell.setCellStyle(grayedDarkerCellStyle);
                journalLabelCell.setCellStyle(grayedDarkerCellStyle);
                monthCell.setCellStyle(grayedDarkerCellStyle);
                accountCodeCell.setCellStyle(grayedDarkerCellStyle);
                accountLabelCell.setCellStyle(grayedDarkerCellStyle);
                debitCell.setCellStyle(grayedDarkerCellStyle);
                creditCell.setCellStyle(grayedDarkerCellStyle);
            }
            if (centralizingJournalByMonthReportLine.getJournalLabel()
                    .startsWith(LanguageConstants.TOTAL_MONTH)) {
                journalCodeCell.setCellStyle(grayedCellStyle);
                journalLabelCell.setCellStyle(grayedCellStyle);
                monthCell.setCellStyle(grayedCellStyle);
                accountCodeCell.setCellStyle(grayedCellStyle);
                accountLabelCell.setCellStyle(grayedCellStyle);
                debitCell.setCellStyle(grayedCellStyle);
                creditCell.setCellStyle(grayedCellStyle);
            }
            journalCodeCell.setCellValue(centralizingJournalByMonthReportLine.getJournalCode());
            journalLabelCell.setCellValue(centralizingJournalByMonthReportLine.getJournalLabel());
            monthCell.setCellValue(centralizingJournalByMonthReportLine.getMonth());
            accountCodeCell.setCellValue(centralizingJournalByMonthReportLine.getAccountCode());
            accountLabelCell.setCellValue(centralizingJournalByMonthReportLine.getAccountLabel());
            setUpMonetaryCellValue(debitCell, getBigDecimalValueFromFormattedString(
                    centralizingJournalByMonthReportLine.getTotalDebitAmount()),
                    centralizingJournalByMonthReportLine.getJournalLabel()
                            .startsWith(LanguageConstants.TOTAL));
            setUpMonetaryCellValue(creditCell, getBigDecimalValueFromFormattedString(
                    centralizingJournalByMonthReportLine.getTotalCreditAmount()),
                    centralizingJournalByMonthReportLine.getJournalLabel()
                            .startsWith(LanguageConstants.TOTAL));
            if (centralizingJournalByMonthReportLine.getJournalLabel()
                    .startsWith(LanguageConstants.TOTAL_MONTH)) {
                sheet.addMergedRegion(new CellRangeAddress(indexRowIndex, indexRowIndex, CellReference
                        .convertColStringToIndex(
                                CentralizingJournalByMonthReportExcelHeader.JOURNAL_LABEL
                                        .getColumnIndexCode()), CellReference.convertColStringToIndex(
                        CentralizingJournalByMonthReportExcelHeader.ACCOUNT_LABEL
                                .getColumnIndexCode())));
                Row journalCodeRow = sheet.getRow(indexRowIndex);
                Cell journalCodeRowCell = journalCodeRow.getCell(CellReference.convertColStringToIndex(
                        CentralizingJournalByMonthReportExcelHeader.ACCOUNT_CODE.getColumnIndexCode()));
                journalCodeRowCell.setCellValue(centralizingJournalByMonthReportLine.getJournalLabel());
            }
            if (centralizingJournalByMonthReportLine.getJournalLabel()
                    .equals(LanguageConstants.GENERAL_TOTAL)) {
                sheet.addMergedRegion(new CellRangeAddress(indexRowIndex, indexRowIndex, CellReference
                        .convertColStringToIndex(
                                CentralizingJournalByMonthReportExcelHeader.JOURNAL_CODE
                                        .getColumnIndexCode()), CellReference.convertColStringToIndex(
                        CentralizingJournalByMonthReportExcelHeader.ACCOUNT_LABEL
                                .getColumnIndexCode())));
                Row journalCodeRow = sheet.getRow(indexRowIndex);
                Cell journalCodeRowCell = journalCodeRow.getCell(CellReference.convertColStringToIndex(
                        CentralizingJournalByMonthReportExcelHeader.JOURNAL_CODE.getColumnIndexCode()));
                journalCodeRowCell.setCellValue(centralizingJournalByMonthReportLine.getJournalLabel());
            }
            indexRowIndex++;
        }
        setupReportFooter(sheet, indexRowIndex,
                CentralizingJournalByMonthReportExcelHeader.JOURNAL_CODE.getColumnIndexCode(), reportParams);
    }

    private static void setupAmortizationTableReportData(ReportTemplateDefaultParameters reportParams, Sheet sheet,
                                                         List<AmortizationTableReportDto> amortizationTableReportLines) {
        int indexRowIndex = DEFAULT_TABLE_HEADER_ROW_INDEX;
        CellStyle cellDefaultStyle = getDefaultCellStyle(sheet.getWorkbook());
        CellStyle grayedCellStyle = getGrayedCellStyle(sheet.getWorkbook(), cellDefaultStyle);
        CellStyle boldCellStyle = getBoldCellStyle(sheet.getWorkbook(), cellDefaultStyle);
        Iterator<AmortizationTableReportDto> iterator = amortizationTableReportLines.iterator();
        while (iterator.hasNext()) {
            AmortizationTableReportDto amortizationTableReportLine = iterator.next();
            Row row = sheet.createRow(indexRowIndex);
            for (AmortizationTableReportExcelHeader amortizationTableReportExcelHeader : AmortizationTableReportExcelHeader
                    .values()) {
                row.createCell(CellReference.convertColStringToIndex(
                        amortizationTableReportExcelHeader.getColumnIndexCode()));
            }

            Cell designationCell = row.getCell(CellReference.convertColStringToIndex(
                    AmortizationTableReportExcelHeader.DESIGNATION.getColumnIndexCode()));
            designationCell.setCellStyle(cellDefaultStyle);

            Cell rateCell = row.getCell(CellReference
                    .convertColStringToIndex(AmortizationTableReportExcelHeader.RATE.getColumnIndexCode()));
            rateCell.setCellStyle(cellDefaultStyle);

            Cell dateOfCommissioningCell = row.getCell(CellReference.convertColStringToIndex(
                    AmortizationTableReportExcelHeader.DATE_OF_COMMISSIONING.getColumnIndexCode()));
            dateOfCommissioningCell.setCellStyle(cellDefaultStyle);

            Cell acquisitionValueCell = row.getCell(CellReference.convertColStringToIndex(
                    AmortizationTableReportExcelHeader.ACQUISITION_VALUE.getColumnIndexCode()));
            acquisitionValueCell.setCellStyle(cellDefaultStyle);

            Cell previousDepreciationCell = row.getCell(CellReference.convertColStringToIndex(
                    AmortizationTableReportExcelHeader.PREVIOUS_DEPRECIATION.getColumnIndexCode()));
            previousDepreciationCell.setCellStyle(cellDefaultStyle);

            Cell annuityExerciseCode = row.getCell(CellReference.convertColStringToIndex(
                    AmortizationTableReportExcelHeader.ANNUITY_EXERCISE.getColumnIndexCode()));
            annuityExerciseCode.setCellStyle(cellDefaultStyle);

            Cell vcnCell = row.getCell(CellReference
                    .convertColStringToIndex(AmortizationTableReportExcelHeader.VCN.getColumnIndexCode()));
            vcnCell.setCellStyle(cellDefaultStyle);

            if (isAmortizationTableReportLineHighlighted(
                    amortizationTableReportLine.getDateOfCommissioning())) {
                dateOfCommissioningCell.setCellStyle(grayedCellStyle);
                acquisitionValueCell.setCellStyle(grayedCellStyle);
                previousDepreciationCell.setCellStyle(grayedCellStyle);
                annuityExerciseCode.setCellStyle(grayedCellStyle);
                vcnCell.setCellStyle(grayedCellStyle);
            }

            if (amortizationTableReportLine.getAccountLabel().isEmpty()) {
                designationCell.setCellValue(amortizationTableReportLine.getDesignation());
                rateCell.setCellValue(amortizationTableReportLine.getRate());
                dateOfCommissioningCell
                        .setCellValue(amortizationTableReportLine.getDateOfCommissioning());
                setUpMonetaryCellValue(acquisitionValueCell, getBigDecimalValueFromFormattedString(
                        amortizationTableReportLine.getAcquisitionValue()),
                        isAmortizationTableReportLineHighlighted(
                                amortizationTableReportLine.getDateOfCommissioning()));
                setUpMonetaryCellValue(previousDepreciationCell, getBigDecimalValueFromFormattedString(
                        amortizationTableReportLine.getPreviousDepreciation()),
                        isAmortizationTableReportLineHighlighted(
                                amortizationTableReportLine.getDateOfCommissioning()));
                setUpMonetaryCellValue(annuityExerciseCode, getBigDecimalValueFromFormattedString(
                        amortizationTableReportLine.getAnnuityExercise()),
                        isAmortizationTableReportLineHighlighted(
                                amortizationTableReportLine.getDateOfCommissioning()));
                setUpMonetaryCellValue(vcnCell,
                        getBigDecimalValueFromFormattedString(amortizationTableReportLine.getVcn()),
                        isAmortizationTableReportLineHighlighted(
                                amortizationTableReportLine.getDateOfCommissioning()));
            } else {
                sheet.addMergedRegion(new CellRangeAddress(indexRowIndex, indexRowIndex, CellReference
                        .convertColStringToIndex(
                                AmortizationTableReportExcelHeader.DESIGNATION.getColumnIndexCode()),
                        CellReference.convertColStringToIndex(
                                AmortizationTableReportExcelHeader.VCN.getColumnIndexCode())));
                Row accountLabelRow = sheet.getRow(indexRowIndex);
                Cell accountLabelRowCell = accountLabelRow.getCell(CellReference.convertColStringToIndex(
                        AmortizationTableReportExcelHeader.DESIGNATION.getColumnIndexCode()));
                accountLabelRowCell.setCellStyle(boldCellStyle);
                accountLabelRowCell.setCellValue(amortizationTableReportLine.getAccountLabel());
            }
            indexRowIndex++;
        }
        setupReportFooter(sheet, indexRowIndex,
                AmortizationTableReportExcelHeader.DESIGNATION.getColumnIndexCode(), reportParams);
    }

    private static boolean isAmortizationTableReportLineHighlighted(String label) {
        return label.equals(AccountingConstants.SUB_TOTAL) || label.equals(LanguageConstants.TOTAL) || label
                .contains(AccountingConstants.SUB_TOTAL);
    }

    private static void setupStateOfJournalsReportData(ReportTemplateDefaultParameters reportParams, Sheet sheet,
                                                       List<JournalStateReportLineDto> journalStateReportLines) {
        int indexRowIndex = DEFAULT_TABLE_HEADER_ROW_INDEX;
        CellStyle cellDefaultStyle = getDefaultCellStyle(sheet.getWorkbook());
        CellStyle grayedCellStyle = getGrayedCellStyle(sheet.getWorkbook(), cellDefaultStyle);
        CellStyle boldCellStyle = getBoldCellStyle(sheet.getWorkbook(), cellDefaultStyle);
        Iterator<JournalStateReportLineDto> iterator = journalStateReportLines.iterator();
        while (iterator.hasNext()) {
            JournalStateReportLineDto journalStateReportLine = iterator.next();
            Row row = sheet.createRow(indexRowIndex);
            for (StateOfJournalsReportExcelHeader stateOfJournalsReportExcelHeader : StateOfJournalsReportExcelHeader
                    .values()) {
                row.createCell(CellReference.convertColStringToIndex(
                        stateOfJournalsReportExcelHeader.getColumnIndexCode()));
            }

            Cell documentCodeCell = row.getCell(CellReference.convertColStringToIndex(
                    StateOfJournalsReportExcelHeader.DOCUMENT_CODE.getColumnIndexCode()));
            documentCodeCell.setCellStyle(cellDefaultStyle);

            Cell documentDateCell = row.getCell(CellReference.convertColStringToIndex(
                    StateOfJournalsReportExcelHeader.DOCUMENT_DATE.getColumnIndexCode()));
            documentDateCell.setCellStyle(cellDefaultStyle);

            Cell documentLabelCell = row.getCell(CellReference
                    .convertColStringToIndex(StateOfJournalsReportExcelHeader.LABEL.getColumnIndexCode()));
            documentLabelCell.setCellStyle(cellDefaultStyle);

            Cell amountCell = row.getCell(CellReference
                    .convertColStringToIndex(StateOfJournalsReportExcelHeader.AMOUNT.getColumnIndexCode()));
            amountCell.setCellStyle(cellDefaultStyle);

            if (isJournalReportLineHighlighted(journalStateReportLine.getReference())) {
                documentLabelCell.setCellStyle(grayedCellStyle);
                amountCell.setCellStyle(grayedCellStyle);
            }

            if (journalStateReportLine.getJournalLabel().isEmpty()) {
                documentCodeCell.setCellValue(journalStateReportLine.getCode());
                documentDateCell.setCellValue(journalStateReportLine.getDate());
                documentLabelCell.setCellValue(journalStateReportLine.getReference());
                setUpMonetaryCellValue(amountCell,
                        getBigDecimalValueFromFormattedString(journalStateReportLine.getTotalAmount()),
                        isJournalReportLineHighlighted(journalStateReportLine.getReference()));
            } else {
                sheet.addMergedRegion(new CellRangeAddress(indexRowIndex, indexRowIndex, CellReference
                        .convertColStringToIndex(
                                StateOfJournalsReportExcelHeader.DOCUMENT_CODE.getColumnIndexCode()),
                        CellReference.convertColStringToIndex(
                                StateOfJournalsReportExcelHeader.AMOUNT.getColumnIndexCode())));
                Row journalCodeRow = sheet.getRow(indexRowIndex);
                Cell journalCodeRowCell = journalCodeRow.getCell(CellReference.convertColStringToIndex(
                        StateOfJournalsReportExcelHeader.DOCUMENT_CODE.getColumnIndexCode()));
                journalCodeRowCell.setCellStyle(boldCellStyle);
                journalCodeRowCell.setCellValue(journalStateReportLine.getJournalLabel());
            }
            indexRowIndex++;
        }
        setupReportFooter(sheet, indexRowIndex,
                StateOfJournalsReportExcelHeader.DOCUMENT_CODE.getColumnIndexCode(), reportParams);
    }

    private static void setUpMonetaryCellValue(Cell cell, BigDecimal value) {
        setUpMonetaryCellValue(cell, value, false);
    }

    private static void checkWorkBookNotEmpty(Workbook workbook) {
        if (workbook.getNumberOfSheets() == 0) {
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_EMPTY_FILE);
        }
    }

    private static void setUpMonetaryCellValue(Cell cell, BigDecimal value, boolean printZeros) {
        if (value.compareTo(BigDecimal.ZERO) != 0 || printZeros) {
            CellStyle cellStyle = cell.getSheet().getWorkbook().createCellStyle();
            cellStyle.cloneStyleFrom(cell.getCellStyle());
            cell.setCellStyle(cellStyle);
            cell.getCellStyle().setDataFormat(cell.getSheet().getWorkbook().createDataFormat()
                    .getFormat(AccountingConstants.EXCEL_EXPORT_FORMAT));
            cell.setCellValue(value.doubleValue());
        } else {
            cell.setCellValue(StringUtils.EMPTY);
        }
    }

    private static boolean isTrialBalanceReportLineHighlighted(String accountLabel) {
        return accountLabel.contains(AccountingConstants.TOTAL_CLASS) || accountLabel
                .equals(AccountingConstants.TRIAL_BALANCE_TOTAL);
    }

    private static boolean isGeneralLedgerReportLineHighlighted(String documentCode) {
        return documentCode.equals(AccountingConstants.SUB_TOTAL) || documentCode
                .equals(AccountingConstants.GENERAL_LEDGER_TOTAL);
    }

    private static boolean isJournalReportLineHighlighted(String label) {
        return label.equals(AccountingConstants.SUB_TOTAL) || label.equals(LanguageConstants.TOTAL);
    }

    private static CellStyle getDefaultCellStyle(Workbook workbook) {
        CellStyle cellDefaultStyle = workbook.createCellStyle();
        cellDefaultStyle.setBorderLeft(BorderStyle.MEDIUM);
        cellDefaultStyle.setBorderRight(BorderStyle.MEDIUM);
        cellDefaultStyle.setBorderTop(BorderStyle.MEDIUM);
        cellDefaultStyle.setBorderBottom(BorderStyle.MEDIUM);
        return cellDefaultStyle;
    }

    private static CellStyle getBoldCellStyle(Workbook workbook, CellStyle cellDefaultStyle) {
        CellStyle boldCellStyle = workbook.createCellStyle();
        boldCellStyle.cloneStyleFrom(cellDefaultStyle);
        Font font = workbook.createFont();
        font.setBold(true);
        boldCellStyle.setFont(font);
        return boldCellStyle;
    }

    private static CellStyle getGrayedCellStyle(Workbook workbook, CellStyle cellDefaultStyle) {
        CellStyle grayedCellStyle = workbook.createCellStyle();
        grayedCellStyle.cloneStyleFrom(cellDefaultStyle);
        grayedCellStyle.setFillForegroundColor(HSSFColor.HSSFColorPredefined.GREY_25_PERCENT.getIndex());
        grayedCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return grayedCellStyle;
    }

    private static CellStyle getGrayedDarkerCellStyle(Workbook workbook, CellStyle cellDefaultStyle) {
        CellStyle grayedDarkerCellStyle = workbook.createCellStyle();
        grayedDarkerCellStyle.cloneStyleFrom(cellDefaultStyle);
        grayedDarkerCellStyle.setFillForegroundColor(HSSFColor.HSSFColorPredefined.GREY_50_PERCENT.getIndex());
        grayedDarkerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return grayedDarkerCellStyle;
    }

    @Override
    public FileUploadDto exportAnnualReportAsExcelFile(Long fiscalYearId, ReportType reportType,
                                                       ReportTemplateDefaultParameters reportParams) {
        FileUploadDto fileUploadDto;
        if (ReportType.IIB.equals(reportType)) {
            fileUploadDto = getFileUploadByReportName(AccountingConstants.INTERMEDIARY_BALANCE);
        } else {
            fileUploadDto = getFileUploadByReportName(reportType.toString());
        }
        if (ReportType.BSAS.equals(reportType) || ReportType.BSEL.equals(reportType)) {
            reportType = ReportType.BS;
        }
        try (Workbook workbook = GenericExcelPOIHelper
                .createWorkBookFromBase64String(fileUploadDto.getBase64Content())) {
            checkWorkBookNotEmpty(workbook);
            Sheet sheet = workbook.getSheetAt(0);
            FiscalYearDto fiscalYear = fiscalYearService.findById(fiscalYearId);
            Optional<FiscalYear> previousFiscalYear = fiscalYearService
                    .findPreviousFiscalYear(fiscalYearId);
            List<ReportLineDto> reportLines = getReportLines(fiscalYearId, reportType);
            List<String> keys = new ArrayList<>();
            reportLines.forEach((ReportLineDto reportLine) -> keys
                    .add(reportLine.getLineIndex() + " " + reportLine.getReportType()));
            for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                fillRowAnnualReport(reportParams, fiscalYear, previousFiscalYear.orElse(null),
                        reportLines, keys, row);
            }
            setupReportFooter(sheet, sheet.getLastRowNum() + 1,
                    TrialBalanceReportExcelHeader.ACCOUNT_CODE.getColumnIndexCode(), reportParams);
            for (int i = 0; i < sheet.getRow(0).getLastCellNum(); i++) {
                sheet.autoSizeColumn(i, false);
            }
            return GenericExcelPOIHelper
                    .getFileUploadDtoFromWorkbook(workbook, excelStoragePath.toFile(), reportType + XLSX);

        } catch (IOException e) {
            log.error(AccountingConstants.ERROR_CREATING_FILE, e);
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_FILE_CREATION_FAIL);
        }
    }

    private List<ReportLineDto> getReportLines(Long fiscalYearId, ReportType reportType) {
        List<ReportLineDto> reportLines;
        if (ReportType.IIB.equals(reportType)) {
            reportLines = reportLineService.getAnnualReport(ReportType.CIB, fiscalYearId);
            reportLines.addAll(reportLineService.getAnnualReport(ReportType.IIB, fiscalYearId));
        } else {
            reportLines = reportLineService.getAnnualReport(reportType, fiscalYearId);
        }
        return reportLines;
    }

    @Override
    public FileUploadDto exportAnnualReportAnnexAsExcelFile(Long fiscalYearId, ReportType reportType,
                                                            ReportTemplateDefaultParameters reportParams) {

        FileUploadDto fileUploadDto = getFileUploadByReportName(reportType.toString() + ANNEX_SUFFIX);
        try (Workbook workbook = GenericExcelPOIHelper
                .createWorkBookFromBase64String(fileUploadDto.getBase64Content())) {
            checkWorkBookNotEmpty(workbook);
            Sheet sheet = workbook.getSheetAt(0);
            FiscalYearDto fiscalYear = fiscalYearService.findById(fiscalYearId);
            Optional<FiscalYear> previousFiscalYear = fiscalYearService
                    .findPreviousFiscalYear(fiscalYearId);

            List<String> headerColumnCodes = Arrays.stream(GeneralLedgerReportExcelHeader.values())
                    .map(GeneralLedgerReportExcelHeader::getColumnIndexCode).collect(Collectors.toList());
            setupTemplatePlaceholders(reportParams, fiscalYear.getStartDate(), fiscalYear.getEndDate(),
                    fiscalYear, FiscalYearConvertor.modelToDto(previousFiscalYear.orElse(null)), sheet,
                    headerColumnCodes);
            List<AnnexeReportDto> annexReportLines = reportLineService
                    .generateAnnualReportAnnex(reportType, fiscalYearId);
            setupFinancialReportAnnexData(reportParams, sheet, annexReportLines);
            return GenericExcelPOIHelper.getFileUploadDtoFromWorkbook(workbook, excelStoragePath.toFile(),
                    reportType + ANNEX_SUFFIX + XLSX);
        } catch (IOException e) {
            log.error(AccountingConstants.ERROR_CREATING_FILE, e);
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_FILE_CREATION_FAIL);
        }
    }

    @Override
    public FileUploadDto exportTrialBalanceReportAsExcelFile(LocalDateTime startDate,
                                                             LocalDateTime endDate, String beginAccountCode, String endAccountCode,
                                                             ReportTemplateDefaultParameters reportParams) {
        final String TRIAL_BALANCE = "TrialBalance";
        FileUploadDto fileUploadDto = getFileUploadByReportName(TRIAL_BALANCE);
        try (Workbook workbook = GenericExcelPOIHelper
                .createWorkBookFromBase64String(fileUploadDto.getBase64Content())) {
            checkWorkBookNotEmpty(workbook);
            Sheet sheet = workbook.getSheetAt(0);
            List<String> headerColumnCodes = Arrays.stream(GeneralLedgerReportExcelHeader.values())
                    .map(GeneralLedgerReportExcelHeader::getColumnIndexCode).collect(Collectors.toList());
            setupTemplatePlaceholders(reportParams, startDate, endDate, new FiscalYearDto(),
                    new FiscalYearDto(), sheet, headerColumnCodes);
            List<TrialBalanceReportLineDto> trialBalanceReportLines = trialBalanceService
                    .generateTrialBalanceTelerikReport(startDate, endDate, beginAccountCode,
                            endAccountCode);
            setupTrialBalanceReportData(reportParams, workbook, sheet, trialBalanceReportLines);
            return GenericExcelPOIHelper.getFileUploadDtoFromWorkbook(workbook, excelStoragePath.toFile(),
                    TRIAL_BALANCE + XLSX);
        } catch (IOException e) {
            log.error(AccountingConstants.ERROR_CREATING_FILE, e);
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_FILE_CREATION_FAIL);
        }
    }

    @Override
    public FileUploadDto exportGeneralLedgerReportAsExcelFile(LocalDateTime startDate,
                                                              LocalDateTime endDate, String beginAccountCode, String endAccountCode, String beginAmount,
                                                              String endAmount, String accountType, ReportTemplateDefaultParameters reportParams,
                                                              String letteringOperationType, String field, String direction) {
        final String GENERAL_LEDGER = "GeneralLedger";
        FileUploadDto fileUploadDto = getFileUploadByReportName(GENERAL_LEDGER);
        try (Workbook workbook = GenericExcelPOIHelper
                .createWorkBookFromBase64String(fileUploadDto.getBase64Content())) {
            checkWorkBookNotEmpty(workbook);
            Sheet sheet = workbook.getSheetAt(0);
            List<String> headerColumnCodes = Arrays.stream(GeneralLedgerReportExcelHeader.values())
                    .map(GeneralLedgerReportExcelHeader::getColumnIndexCode).collect(Collectors.toList());
            setupTemplatePlaceholders(reportParams, startDate, endDate, new FiscalYearDto(),
                    new FiscalYearDto(), sheet, headerColumnCodes);
            List<GeneralLedgerReportLineDto> generalLedgerReportLines = generalLedgerService
                    .generateGeneralLedgerTelerikReport(startDate, endDate, beginAccountCode,
                            endAccountCode, beginAmount, endAmount, accountType, letteringOperationType,
                            field, direction);
            setupGeneralLedgerReportData(reportParams, sheet, generalLedgerReportLines);
            return GenericExcelPOIHelper.getFileUploadDtoFromWorkbook(workbook, excelStoragePath.toFile(),
                    GENERAL_LEDGER + XLSX);
        } catch (IOException e) {
            log.error(AccountingConstants.ERROR_CREATING_FILE, e);
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_FILE_CREATION_FAIL);
        }
    }

    @Override
    public FileUploadDto exportAuxiliaryJournalReportAsExcelFile(LocalDateTime startDate,
                                                                 LocalDateTime endDate, List<Long> journalIds, ReportTemplateDefaultParameters reportParams) {
        final String AUXILIARY_JOURNAL = "AuxiliaryJournal";
        FileUploadDto fileUploadDto = getFileUploadByReportName(AUXILIARY_JOURNAL);
        try (Workbook workbook = GenericExcelPOIHelper
                .createWorkBookFromBase64String(fileUploadDto.getBase64Content())) {
            checkWorkBookNotEmpty(workbook);
            Sheet sheet = workbook.getSheetAt(0);
            List<String> headerColumnCodes = Arrays.stream(GeneralLedgerReportExcelHeader.values())
                    .map(GeneralLedgerReportExcelHeader::getColumnIndexCode).collect(Collectors.toList());
            setupTemplatePlaceholders(reportParams, startDate, endDate, new FiscalYearDto(),
                    new FiscalYearDto(), sheet, headerColumnCodes);
            List<AuxiliaryJournalLineDto> auxiliaryJournalReportLines = stateOfAuxiliaryJournalService
                    .generateAuxiliaryJournalsTelerikReport(startDate, endDate, journalIds);
            setupAuxiliaryJournalReportData(reportParams, sheet, auxiliaryJournalReportLines);
            return GenericExcelPOIHelper.getFileUploadDtoFromWorkbook(workbook, excelStoragePath.toFile(),
                    AUXILIARY_JOURNAL + XLSX);
        } catch (IOException e) {
            log.error(AccountingConstants.ERROR_CREATING_FILE, e);
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_FILE_CREATION_FAIL);
        }
    }

    @Override
    public FileUploadDto exportCentralizingJournalReportAsExcelFile(LocalDateTime startDate,
                                                                    LocalDateTime endDate, int breakingAccount, int breakingCustomerAccount, int breakingSupplierAccount,
                                                                    List<Long> journalIds, ReportTemplateDefaultParameters reportParams) {
        final String CENTRALIZING_JOURNAL = "CentralizingJournal";
        FileUploadDto fileUploadDto = getFileUploadByReportName(CENTRALIZING_JOURNAL);
        try (Workbook workbook = GenericExcelPOIHelper
                .createWorkBookFromBase64String(fileUploadDto.getBase64Content())) {
            checkWorkBookNotEmpty(workbook);
            Sheet sheet = workbook.getSheetAt(0);
            List<String> headerColumnCodes = Arrays.stream(CentilizingJournalReportExcelHeader.values())
                    .map(CentilizingJournalReportExcelHeader::getColumnIndexCode)
                    .collect(Collectors.toList());
            setupTemplatePlaceholders(reportParams, startDate, endDate, new FiscalYearDto(),
                    new FiscalYearDto(), sheet, headerColumnCodes);

            List<CentralizingJournalReportLineDto> centralizingJournalReportLineDtos = journalService
                    .generateCentralizingJournalTelerikReportLines(journalIds, startDate, endDate,
                            breakingAccount, breakingCustomerAccount, breakingSupplierAccount);
            setupCentralizingJournalReportData(reportParams, sheet, centralizingJournalReportLineDtos);
            return GenericExcelPOIHelper.getFileUploadDtoFromWorkbook(workbook, excelStoragePath.toFile(),
                    CENTRALIZING_JOURNAL + XLSX);
        } catch (IOException e) {
            log.error(AccountingConstants.ERROR_CREATING_FILE, e);
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_FILE_CREATION_FAIL);
        }
    }

    @Override
    public FileUploadDto exportCentralizingJournalReportByDateAsExcelFile(LocalDateTime startDate,
                                                                          LocalDateTime endDate, int breakingAccount, int breakingCustomerAccount, int breakingSupplierAccount,
                                                                          List<Long> journalIds, ReportTemplateDefaultParameters reportParams) {
        final String CENTRALIZING_JOURNAL_BY_DATE = "CentralizingJournalByDate";
        FileUploadDto fileUploadDto = getFileUploadByReportName(CENTRALIZING_JOURNAL_BY_DATE);
        try (Workbook workbook = GenericExcelPOIHelper
                .createWorkBookFromBase64String(fileUploadDto.getBase64Content())) {
            checkWorkBookNotEmpty(workbook);
            Sheet sheet = workbook.getSheetAt(0);
            List<String> headerColumnCodes = Arrays
                    .stream(CentralizingJournalByMonthReportExcelHeader.values())
                    .map(CentralizingJournalByMonthReportExcelHeader::getColumnIndexCode)
                    .collect(Collectors.toList());
            setupTemplatePlaceholders(reportParams, startDate, endDate, new FiscalYearDto(),
                    new FiscalYearDto(), sheet, headerColumnCodes);

            List<CentralizingJournalByMonthReportLineDto> centralizingJournalByMonthReportLineDtos = journalService
                    .generateCentralizingJournalByMonthReportLines(journalIds, startDate, endDate,
                            breakingAccount, breakingCustomerAccount, breakingSupplierAccount);
            setupCentralizingJournalByMonthReportData(reportParams, sheet,
                    centralizingJournalByMonthReportLineDtos);

            return GenericExcelPOIHelper.getFileUploadDtoFromWorkbook(workbook, excelStoragePath.toFile(),
                    CENTRALIZING_JOURNAL_BY_DATE + XLSX);
        } catch (IOException e) {
            log.error(AccountingConstants.ERROR_CREATING_FILE, e);
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_FILE_CREATION_FAIL);
        }
    }

    @Override
    public FileUploadDto exportStateOfJournalsReportAsExcelFile(LocalDateTime startDate,
                                                                LocalDateTime endDate, ReportTemplateDefaultParameters reportParams) {
        final String STATE_OF_JOURNALS = "StateOfJournals";
        FileUploadDto fileUploadDto = getFileUploadByReportName(STATE_OF_JOURNALS);
        try (Workbook workbook = GenericExcelPOIHelper
                .createWorkBookFromBase64String(fileUploadDto.getBase64Content())) {
            checkWorkBookNotEmpty(workbook);
            Sheet sheet = workbook.getSheetAt(0);
            List<String> headerColumnCodes = Arrays.stream(CentilizingJournalReportExcelHeader.values())
                    .map(CentilizingJournalReportExcelHeader::getColumnIndexCode)
                    .collect(Collectors.toList());
            setupTemplatePlaceholders(reportParams, startDate, endDate, new FiscalYearDto(),
                    new FiscalYearDto(), sheet, headerColumnCodes);
            List<JournalStateReportLineDto> journalStateReportLineDtos = journalStateService
                    .findJournalsStateReport(startDate, endDate);
            setupStateOfJournalsReportData(reportParams, sheet, journalStateReportLineDtos);
            return GenericExcelPOIHelper.getFileUploadDtoFromWorkbook(workbook, excelStoragePath.toFile(),
                    STATE_OF_JOURNALS + XLSX);
        } catch (IOException e) {
            log.error(AccountingConstants.ERROR_CREATING_FILE, e);
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_FILE_CREATION_FAIL);
        }
    }

    @Override
    public FileUploadDto exportAmortizationReportAsExcelFile(Long fiscalYearId, String user,
                                                             String contentType, String authorization, ReportTemplateDefaultParameters reportParams) {
        final String AMORTIZATION_TABLE = "AmortizationTable";
        FileUploadDto fileUploadDto = getFileUploadByReportName(AMORTIZATION_TABLE);
        FiscalYearDto fiscalYear = fiscalYearService.findById(fiscalYearId);
        try (Workbook workbook = GenericExcelPOIHelper
                .createWorkBookFromBase64String(fileUploadDto.getBase64Content())) {
            checkWorkBookNotEmpty(workbook);
            Sheet sheet = workbook.getSheetAt(0);
            List<String> headerColumnCodes = Arrays.stream(AmortizationTableReportExcelHeader.values())
                    .map(AmortizationTableReportExcelHeader::getColumnIndexCode)
                    .collect(Collectors.toList());
            setupTemplatePlaceholders(reportParams, fiscalYear.getStartDate(), fiscalYear.getEndDate(),
                    new FiscalYearDto(), new FiscalYearDto(), sheet, headerColumnCodes);
            List<AmortizationTableReportDto> amortizationTableReportLines = reportLineService
                    .generateAmortizationReport(fiscalYearId, user, contentType, authorization);
            setupAmortizationTableReportData(reportParams, sheet, amortizationTableReportLines);
            return GenericExcelPOIHelper.getFileUploadDtoFromWorkbook(workbook, excelStoragePath.toFile(),
                    AMORTIZATION_TABLE + XLSX);
        } catch (IOException e) {
            log.error(AccountingConstants.ERROR_CREATING_FILE, e);
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_FILE_CREATION_FAIL);
        }
    }

    private FileUploadDto getFileUploadByReportName(String reportName) {
        FileUploadDto fileUploadDto = new FileUploadDto();
        try {
            URL reportTemplateURL = getClass()
                    .getResource(EXCEL_REPORT_TEMPLATES_FOLDER + reportName + XLSX);
            if (reportTemplateURL != null) {
                File file = new File(reportTemplateURL.getPath());
                fileUploadDto.setName(file.getName());
                fileUploadDto.setBase64Content(
                        Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath())));
            } else {
                log.error("The report template file was not found in {}",
                        EXCEL_REPORT_TEMPLATES_FOLDER + reportName + XLSX);
                throw new HttpCustomException(ApiErrors.Accounting.EXCEL_REPORT_TEMPLATES_NOT_FOUND);
            }
        } catch (IOException e) {
            log.error("There was a problem parsing the report template file", e);
            throw new HttpCustomException(ApiErrors.Accounting.EXCEL_FILE_CREATION_FAIL);
        }
        return fileUploadDto;
    }

}
