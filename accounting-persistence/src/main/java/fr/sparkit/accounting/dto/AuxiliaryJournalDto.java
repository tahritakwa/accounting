package fr.sparkit.accounting.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import fr.sparkit.accounting.auditing.MoneySerializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuxiliaryJournalDto {

    private Long id;
    private String code;
    private String label;

    @JsonSerialize(using = MoneySerializer.class)
    private BigDecimal totalDebit;

    @JsonSerialize(using = MoneySerializer.class)
    private BigDecimal totalCredit;

    public AuxiliaryJournalDto(BigDecimal totalDebit, BigDecimal totalCredit) {
        super();
        this.totalDebit = totalDebit;
        this.totalCredit = totalCredit;
    }
}
