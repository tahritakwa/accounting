package fr.sparkit.accounting.services.impl;

import static fr.sparkit.accounting.constants.AccountingConstants.*;
import static fr.sparkit.accounting.services.impl.AccountService.MAX_ACCOUNT_CODE_LENGTH;
import static fr.sparkit.accounting.services.utils.excel.ExcelCellStyleHelper.setInvalidCell;
import static fr.sparkit.accounting.services.utils.excel.GenericExcelPOIHelper.isLabelInCellValid;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import fr.sparkit.accounting.auditing.AccountingTemplateExcelCell;
import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.constants.LanguageConstants;
import fr.sparkit.accounting.constants.NumberConstant;
import fr.sparkit.accounting.constants.XLSXErrors;
import fr.sparkit.accounting.convertor.TemplateAccountingDetailsConverter;
import fr.sparkit.accounting.dao.TemplateAccountingDetailsDao;
import fr.sparkit.accounting.dto.TemplateAccountingDetailsDto;
import fr.sparkit.accounting.entities.TemplateAccounting;
import fr.sparkit.accounting.entities.TemplateAccountingDetails;
import fr.sparkit.accounting.services.IAccountService;
import fr.sparkit.accounting.services.IDocumentAccountLineService;
import fr.sparkit.accounting.services.ITempalteAccountingService;
import fr.sparkit.accounting.services.ITemplateAccountingDetailsService;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TemplateAccountingDetailsService extends GenericService<TemplateAccountingDetails, Long>
        implements ITemplateAccountingDetailsService {

    private static final DataFormatter dataFormatter = new DataFormatter();
    private final IAccountService accountService;
    private final IDocumentAccountLineService documentAccountLineService;
    private final TemplateAccountingDetailsDao templateAccountingDetailsDao;
    private final ITempalteAccountingService templateAccountingService;

    @Autowired
    public TemplateAccountingDetailsService(IAccountService accountService,
            IDocumentAccountLineService documentAccountLineService,
            TemplateAccountingDetailsDao templateAccountingDetailsDao,
            @Lazy ITempalteAccountingService templateAccountingService) {
        super();
        this.accountService = accountService;
        this.documentAccountLineService = documentAccountLineService;
        this.templateAccountingDetailsDao = templateAccountingDetailsDao;
        this.templateAccountingService = templateAccountingService;
    }

    @Override
    public List<TemplateAccountingDetailsDto> getAllTemplateAccountingDetails() {
        return TemplateAccountingDetailsConverter.modelsToDtos(findAll());
    }

    @Override
    public TemplateAccountingDetailsDto getTemplateAccountingDetailsById(Long id) {
        return TemplateAccountingDetailsConverter.modelToDto(findOne(id));
    }

    @Override
    public void save(TemplateAccountingDetailsDto templateAccountingDetail, TemplateAccounting templateAccounting) {
        if (templateAccountingDetail.getCreditAmount() != null && templateAccountingDetail.getDebitAmount() != null
                && templateAccountingDetail.getCreditAmount().multiply(templateAccountingDetail.getDebitAmount())
                        .compareTo(BigDecimal.ZERO.setScale(
                                AccountingConstants.DEFAULT_SCALE_FOR_BIG_DECIMAL * NumberConstant.TWO,
                                RoundingMode.HALF_UP)) != 0) {
            templateAccountingService.delete(templateAccounting.getId());
            log.error(DOCUMENT_ACCOUNT_LINE_BOTH_DEBIT_CREDIT_AMOUNT);
            throw new HttpCustomException(ApiErrors.Accounting.DOCUMENT_ACCOUNT_LINE_WITH_BOTH_DEBIT_AND_CREDIT);
        }
        TemplateAccountingDetails templateAccountingDetailModel = TemplateAccountingDetailsConverter
                .dtoToModel(templateAccountingDetail);
        templateAccountingDetailModel.setId(templateAccountingDetail.getId());
        templateAccountingDetailModel.setCreditAmount(templateAccountingDetail.getCreditAmount());
        templateAccountingDetailModel.setDebitAmount(templateAccountingDetail.getDebitAmount());
        templateAccountingDetailModel.setAccount(accountService.findOne(templateAccountingDetail.getAccountId()));
        templateAccountingDetailModel.setTemplateAccounting(templateAccounting);
        templateAccountingDetailsDao.saveAndFlush(templateAccountingDetailModel);
        log.info(LOG_ENTITY_UPDATED, templateAccountingDetailModel);
    }

    @Override
    public List<TemplateAccountingDetails> findByTemplateAccountingIdAndIsDeletedFalse(Long id) {
        return templateAccountingDetailsDao.findBytemplateAccountingIdAndIsDeletedFalse(id);
    }

    @Override
    public boolean isAccountingTemplateDetailValuesAddedToRow(TemplateAccountingDetailsDto accountingTemplateDetail,
            Row row, List<Field> excelHeaderFields, List<String> acceptedHeaders) {
        boolean isValid = true;
        for (int i = 0; i < excelHeaderFields.size(); i++) {
            Cell cell = row.getCell(i);
            if (cell == null) {
                cell = row.createCell(i);
            }
            if (!excelHeaderFields.get(i).getAnnotation(AccountingTemplateExcelCell.class)
                    .isAccountingTemplateField()) {
                switch (excelHeaderFields.get(i).getAnnotation(AccountingTemplateExcelCell.class).headerName()) {
                case LanguageConstants.XLSXHeaders.ACCOUNT_CODE_HEADER_NAME:
                    isValid &= isAccountCodeSet(cell, accountingTemplateDetail);
                    break;
                case LanguageConstants.XLSXHeaders.LINE_LABEL_HEADER_NAME:
                    isValid &= isAccountingTemplateDetailLabelSet(cell, accountingTemplateDetail);
                    break;
                case LanguageConstants.XLSXHeaders.DEBIT_HEADER_NAME:
                    isValid &= isAccountingTemplateDetailDebitSet(cell, accountingTemplateDetail);
                    break;
                case LanguageConstants.XLSXHeaders.CREDIT_HEADER_NAME:
                    isValid &= isAccountTemplateDetailCreditSet(cell, accountingTemplateDetail);
                    break;
                default:
                    isValid = false;
                }
            }
        }
        return isValid;
    }

    private static boolean isAccountingTemplateDetailLabelSet(Cell cell,
            TemplateAccountingDetailsDto accountingTemplateDetail) {
        if (isLabelInCellValid(cell)) {
            accountingTemplateDetail.setLabel(dataFormatter.formatCellValue(cell).trim());
            return true;
        }
        return false;
    }

    private boolean isAccountingTemplateDetailDebitSet(Cell cell,
            TemplateAccountingDetailsDto accountingTemplateDetail) {
        if (documentAccountLineService.isMonetaryValueInCellValid(cell)) {
            BigDecimal debit = BigDecimal.valueOf(Double.parseDouble(dataFormatter.formatCellValue(cell).trim()
                    .replace(String.valueOf(THOUSANDS_SEPARATOR), StringUtils.EMPTY)));
            accountingTemplateDetail.setDebitAmount(debit);
            return true;
        }
        return false;
    }

    private boolean isAccountTemplateDetailCreditSet(Cell cell, TemplateAccountingDetailsDto accountingTemplateDetail) {
        if (documentAccountLineService.isMonetaryValueInCellValid(cell)) {
            BigDecimal debit = BigDecimal.valueOf(Double.parseDouble(dataFormatter.formatCellValue(cell).trim()
                    .replace(String.valueOf(THOUSANDS_SEPARATOR), StringUtils.EMPTY)));
            accountingTemplateDetail.setCreditAmount(debit);
            return true;
        }
        return false;
    }

    private boolean isAccountCodeSet(Cell cell, TemplateAccountingDetailsDto accountingTemplateDetail) {
        if (isAccountCodeInCellValid(cell)) {
            String cellValue = dataFormatter.formatCellValue(cell).trim();
            accountingTemplateDetail.setAccountId(accountService.findByCode(Integer.parseInt(cellValue)).getId());
            return true;
        }
        return false;
    }

    private boolean isAccountCodeInCellValid(Cell cell) {
        String cellValue = dataFormatter.formatCellValue(cell).trim();
        if (!cellValue.isEmpty()) {
            try {
                return validateAccountCodeCell(cell, cellValue);
            } catch (NumberFormatException e) {
                setInvalidCell(cell, XLSXErrors.ACCOUNT_ACCOUNT_CODE_CELL_SHOULD_BE_OF_TYPE_NUMBER);
            }
        } else {
            setInvalidCell(cell, XLSXErrors.REQUIRED_FIELD);
        }
        return false;
    }

    private boolean validateAccountCodeCell(Cell cell, String cellValue) {
        int accountCode = Integer.parseInt(cellValue);
        if (accountCode < 0 || cellValue.length() != MAX_ACCOUNT_CODE_LENGTH) {
            setInvalidCell(cell,
                    String.format(XLSXErrors.AccountXLSXErrors.ACCOUNT_CODE_INVALID_FORMAT, MAX_ACCOUNT_CODE_LENGTH));
        } else {
            if (accountService.isAccountCodeUsedInMultipleAccounts(accountCode)) {
                setInvalidCell(cell,
                        String.format(XLSXErrors.DUPLICATE_ACCOUNTS_FOUND_WITH_THE_SAME_CODE, accountCode));
            } else if (!accountService.findAccountByCode(accountCode).isPresent()) {
                setInvalidCell(cell, String.format(XLSXErrors.NO_ACCOUNT_WITH_CODE, accountCode));
            } else {
                return true;
            }
        }
        return false;
    }
}
