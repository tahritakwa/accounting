package fr.sparkit.accounting.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JournalAmountDto {

    private Long journalId;
    private BigDecimal journalDebitAmount;
    private BigDecimal journalCreditAmount;

    public JournalAmountDto(BigDecimal journalDebitAmount, BigDecimal journalCreditAmount) {
        super();
        this.journalDebitAmount = journalDebitAmount;
        this.journalCreditAmount = journalCreditAmount;
    }

}
