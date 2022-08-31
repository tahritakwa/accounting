package fr.sparkit.accounting.dto;

import org.apache.commons.lang3.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrintableCloseDocumentAccountLineDto {
    private String label = StringUtils.EMPTY;
    private String reference = StringUtils.EMPTY;
    private String debitAmount = StringUtils.EMPTY;
    private String creditAmount = StringUtils.EMPTY;
    private String journalLabel = StringUtils.EMPTY;
    private LocalDateTime documentDate;
    private String codeDocument = StringUtils.EMPTY;
}
