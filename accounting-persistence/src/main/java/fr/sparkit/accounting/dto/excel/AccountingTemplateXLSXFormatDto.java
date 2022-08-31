package fr.sparkit.accounting.dto.excel;

import static fr.sparkit.accounting.constants.LanguageConstants.ACCOUNTING_TEMPLATE_HEADERS_TOOLTIP_COMMENT;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import fr.sparkit.accounting.auditing.AccountingTemplateExcelCell;
import fr.sparkit.accounting.constants.LanguageConstants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AccountingTemplateXLSXFormatDto {

    @AccountingTemplateExcelCell(headerName = LanguageConstants.XLSXHeaders.LABEL_HEADER_NAME,
     isAccountingTemplateField = true, isDarker = true, tooltipMessage = ACCOUNTING_TEMPLATE_HEADERS_TOOLTIP_COMMENT)
    private String templateLabel;

    @AccountingTemplateExcelCell(headerName = LanguageConstants.XLSXHeaders.JOURNAL_HEADER_NAME,
     isAccountingTemplateField = true, isDarker = true, tooltipMessage = ACCOUNTING_TEMPLATE_HEADERS_TOOLTIP_COMMENT)
    private String journal;

    @AccountingTemplateExcelCell(headerName = LanguageConstants.XLSXHeaders.ACCOUNT_CODE_HEADER_NAME)
    private Integer accountCode;

    @AccountingTemplateExcelCell(headerName = LanguageConstants.XLSXHeaders.LINE_LABEL_HEADER_NAME)
    private String accountingTemplateDetailLabel;

    @AccountingTemplateExcelCell(headerName = LanguageConstants.XLSXHeaders.DEBIT_HEADER_NAME)
    private BigDecimal accountingTemplateDetailDebitValue;

    @AccountingTemplateExcelCell(headerName = LanguageConstants.XLSXHeaders.CREDIT_HEADER_NAME)
    private BigDecimal accountingTemplateDetailCreditValue;

    private String error = "";

    public void setError(String error) {
        if (this.error.isEmpty()) {
            this.error = error;
        } else {
            this.error = this.error + " ; " + error;
        }
    }

    public static List<Field> getAccountingTemplateExcelHeaderFields() {
        return Arrays.stream(AccountingTemplateXLSXFormatDto.class.getDeclaredFields())
                .filter(field -> field.getAnnotation(AccountingTemplateExcelCell.class) != null)
                .collect(Collectors.toList());
    }

}
