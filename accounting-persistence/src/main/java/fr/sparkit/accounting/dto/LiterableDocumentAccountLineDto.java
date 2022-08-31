package fr.sparkit.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.validation.constraints.Size;

import fr.sparkit.accounting.constants.AccountingConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor

public class LiterableDocumentAccountLineDto {

    private Long id;
    private String account;
    private LocalDateTime documentAccountDate;
    private String documentAccountCode;
    private String journal;
    private String reference;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal balance;
    @Size(min = AccountingConstants.ENTITY_DEFAULT_LABEL_MIN_LENGTH, max = AccountingConstants.ENTITY_DEFAULT_LABEL_MAX_LENGTH)
    private String letter;
    private Long documentAccount;
    private String label;
    private Long accountId;

    public LiterableDocumentAccountLineDto(String letter) {
        super();
        this.letter = letter;
    }

}
