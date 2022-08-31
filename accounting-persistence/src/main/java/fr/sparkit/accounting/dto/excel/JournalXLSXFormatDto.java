package fr.sparkit.accounting.dto.excel;

import static fr.sparkit.accounting.constants.LanguageConstants.BOOLEAN_OPTIONS_TOOLTIP_COMMENT;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import fr.sparkit.accounting.auditing.JournalExcelCell;
import fr.sparkit.accounting.constants.LanguageConstants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class JournalXLSXFormatDto {

    @JournalExcelCell(headerName = LanguageConstants.XLSXHeaders.JOURNAL_CODE_HEADER_NAME)
    private String code;

    @JournalExcelCell(headerName = LanguageConstants.XLSXHeaders.LABEL_HEADER_NAME)
    private String label;

    @JournalExcelCell(headerName = LanguageConstants.XLSXHeaders.RECONCILABLE_HEADER_NAME, tooltipMessage = BOOLEAN_OPTIONS_TOOLTIP_COMMENT)
    private String reconcilable;

    @JournalExcelCell(headerName = LanguageConstants.XLSXHeaders.CASH_FLOW_HEADER_NAME, tooltipMessage = BOOLEAN_OPTIONS_TOOLTIP_COMMENT)
    private String cashFlow;

    private String error = "";

    public void setError(String error) {
        if (this.error.isEmpty()) {
            this.error = error;
        } else {
            this.error = this.error + " ; " + error;
        }
    }

    public static List<Field> getJournalExcelHeaderFields() {
        return Arrays.stream(JournalXLSXFormatDto.class.getDeclaredFields())
                .filter(field -> field.getAnnotation(JournalExcelCell.class) != null).collect(Collectors.toList());
    }

}
