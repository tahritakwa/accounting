package fr.sparkit.accounting.dto.excel;

import static fr.sparkit.accounting.constants.LanguageConstants.BOOLEAN_OPTIONS_TOOLTIP_COMMENT;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import fr.sparkit.accounting.auditing.AccountExcelCell;
import fr.sparkit.accounting.constants.LanguageConstants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AccountXLSXFormatDto {

    @AccountExcelCell(headerName = LanguageConstants.XLSXHeaders.PARENT_PLAN_CODE_HEADER_NAME)
    private Integer parentPlanCode;
    @AccountExcelCell(headerName = LanguageConstants.XLSXHeaders.CODE_HEADER_NAME)
    private Integer accountCode;

    @AccountExcelCell(headerName = LanguageConstants.XLSXHeaders.LABEL_HEADER_NAME)
    private String label;

    @AccountExcelCell(headerName = LanguageConstants.XLSXHeaders.ACCOUNT_OPENING_DEBIT_HEADER_NAME)
    private BigDecimal accountOpeningDebitAmount;

    @AccountExcelCell(headerName = LanguageConstants.XLSXHeaders.ACCOUNT_OPENING_CREDIT_HEADER_NAME)
    private BigDecimal accountOpeningCreditAmount;

    @AccountExcelCell(headerName = LanguageConstants.XLSXHeaders.LITERABLE_HEADER_NAME, tooltipMessage = BOOLEAN_OPTIONS_TOOLTIP_COMMENT)
    private String literable;

    @AccountExcelCell(headerName = LanguageConstants.XLSXHeaders.RECONCILABLE_HEADER_NAME, tooltipMessage = BOOLEAN_OPTIONS_TOOLTIP_COMMENT)
    private String reconcilable;

    private String error = "";

    public void setError(String error) {
        if (this.error.isEmpty()) {
            this.error = error;
        } else {
            this.error = this.error + " ; " + error;
        }
    }

    public static List<Field> getAccountExcelHeaderFields() {
        return Arrays.stream(AccountXLSXFormatDto.class.getDeclaredFields())
                .filter(field -> field.getAnnotation(AccountExcelCell.class) != null).collect(Collectors.toList());
    }

}
