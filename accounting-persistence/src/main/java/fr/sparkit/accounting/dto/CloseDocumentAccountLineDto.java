package fr.sparkit.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CloseDocumentAccountLineDto {

    private Long id;
    private String label;
    private String reference;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private String journalLabel;
    private boolean isClose;
    private Long documentId;
    private LocalDateTime documentDate;
    private String codeDocument;

    public CloseDocumentAccountLineDto(String label, BigDecimal debitAmount, BigDecimal creditAmount) {
        super();
        this.label = label;
        this.debitAmount = debitAmount;
        this.creditAmount = creditAmount;
    }

}
