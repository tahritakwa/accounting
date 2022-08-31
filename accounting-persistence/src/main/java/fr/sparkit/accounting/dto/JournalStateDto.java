package fr.sparkit.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JournalStateDto {
    private Long journalId;
    private String journalCode;
    private String journalLabel;
    private BigDecimal totalAmount;

    public JournalStateDto(BigDecimal totalAmount) {
        super();
        this.totalAmount = totalAmount;
    }
}
