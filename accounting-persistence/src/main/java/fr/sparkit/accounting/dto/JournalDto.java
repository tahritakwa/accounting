package fr.sparkit.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonFormat;

import fr.sparkit.accounting.constants.AccountingConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JournalDto {

    private Long id;
    @Size(min = AccountingConstants.ENTITY_DEFAULT_LABEL_MIN_LENGTH, max = AccountingConstants.ENTITY_DEFAULT_LABEL_MAX_LENGTH)
    private String code;
    @Size(min = AccountingConstants.ENTITY_DEFAULT_LABEL_MIN_LENGTH, max = AccountingConstants.ENTITY_DEFAULT_LABEL_MAX_LENGTH)
    private String label;
    @JsonFormat(pattern = "yyyy/MM/dd HH:mm:ss")
    private LocalDateTime createdDate;
    private boolean reconcilable;
    private boolean cashFlow;
    private BigDecimal journalDebitAmount;
    private BigDecimal journalCreditAmount;

    public JournalDto(Long id, @Size(min = 2, max = 255) String code, @Size(min = 2, max = 255) String label,
            LocalDateTime createdDate, boolean reconcilable, boolean cashFlow) {
        super();
        this.id = id;
        this.code = code;
        this.label = label;
        this.createdDate = createdDate;
        this.reconcilable = reconcilable;
        this.cashFlow = cashFlow;
    }

    public JournalDto(Long id, @Size(min = 2, max = 255) String code, @Size(min = 2, max = 255) String label,
            BigDecimal journalDebitAmount, BigDecimal journalCreditAmount) {
        super();
        this.id = id;
        this.code = code;
        this.label = label;
        this.journalDebitAmount = journalDebitAmount;
        this.journalCreditAmount = journalCreditAmount;
    }

}
