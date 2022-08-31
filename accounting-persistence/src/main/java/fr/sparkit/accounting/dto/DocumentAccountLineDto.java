package fr.sparkit.accounting.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentAccountLineDto implements Serializable {
    private static final long serialVersionUID = -467635119312915289L;
    private Long id;
    @JsonFormat(pattern = "yyyy/MM/dd HH:mm:ss")
    private LocalDateTime documentLineDate;
    private String label;
    private String reference;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private Long accountId;
    private boolean isClose;
    private String letter;
    @JsonFormat(pattern = "yyyy/MM/dd")
    private LocalDate reconciliationDate;

    public DocumentAccountLineDto(BigDecimal debitAmount, BigDecimal creditAmount) {
        super();
        this.debitAmount = debitAmount;
        this.creditAmount = creditAmount;
    }

    public DocumentAccountLineDto(LocalDateTime documentLineDate, String label, String reference,
            BigDecimal debitAmount, BigDecimal creditAmount, Long accountId) {
        super();
        this.documentLineDate = documentLineDate;
        this.label = label;
        this.reference = reference;
        this.debitAmount = debitAmount;
        this.creditAmount = creditAmount;
        this.accountId = accountId;
    }

}
