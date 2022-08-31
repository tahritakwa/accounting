package fr.sparkit.accounting.dto.excel;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import fr.sparkit.accounting.auditing.DocumentAccountExcelCell;
import fr.sparkit.accounting.constants.LanguageConstants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static fr.sparkit.accounting.constants.LanguageConstants.DOCUMENT_ACCOUNT_HEADERS_TOOLTIP_COMMENT;

@Getter
@Setter
@NoArgsConstructor
public class DocumentAccountXLSXFormatDto {

    @DocumentAccountExcelCell(headerName = LanguageConstants.XLSXHeaders.DOCUMENT_DATE_HEADER_NAME,
     isDocumentAccountField = true, isDarker = true, tooltipMessage = DOCUMENT_ACCOUNT_HEADERS_TOOLTIP_COMMENT)
    private LocalDateTime documentDate;

    @DocumentAccountExcelCell(headerName = LanguageConstants.XLSXHeaders.DOCUMENT_LABEL_HEADER_NAME,
     isDocumentAccountField = true, isDarker = true, tooltipMessage = DOCUMENT_ACCOUNT_HEADERS_TOOLTIP_COMMENT)
    private String documentLabel;

    @DocumentAccountExcelCell(headerName = LanguageConstants.XLSXHeaders.JOURNAL_HEADER_NAME,
     isDocumentAccountField = true, isDarker = true, tooltipMessage = DOCUMENT_ACCOUNT_HEADERS_TOOLTIP_COMMENT)
    private String journal;

    @DocumentAccountExcelCell(headerName = LanguageConstants.XLSXHeaders.ACCOUNT_CODE_HEADER_NAME)
    private Integer accountCode;

    @DocumentAccountExcelCell(headerName = LanguageConstants.XLSXHeaders.LINE_DATE)
    private LocalDateTime documentAccountLineDate;

    @DocumentAccountExcelCell(headerName = LanguageConstants.XLSXHeaders.REFERENCE_HEADER_NAME)
    private String documentAccountLineReference;

    @DocumentAccountExcelCell(headerName = LanguageConstants.XLSXHeaders.LINE_LABEL_HEADER_NAME)
    private String documentAccountLineLabel;

    @DocumentAccountExcelCell(headerName = LanguageConstants.XLSXHeaders.DEBIT_HEADER_NAME)
    private BigDecimal documentAccountLineDebitValue;

    @DocumentAccountExcelCell(headerName = LanguageConstants.XLSXHeaders.CREDIT_HEADER_NAME)
    private BigDecimal documentAccountLineCreditValue;

    private String error = "";

    public void setError(String error) {
        if (this.error.isEmpty()) {
            this.error = error;
        } else {
            this.error = this.error + " ; " + error;
        }
    }

    public static List<Field> getDocumentAccountExcelHeaderFields() {
        return Arrays.stream(DocumentAccountXLSXFormatDto.class.getDeclaredFields())
                .filter(field -> field.getAnnotation(DocumentAccountExcelCell.class) != null)
                .collect(Collectors.toList());
    }

}
