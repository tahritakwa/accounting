package fr.sparkit.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class GeneralLedgerAccountDetailsDto {

    private Long documentAccountLineId;
    
    private Long documentAccountId;

    private String documentAccountCode;

    @JsonFormat(pattern = "yyyy/MM/dd HH:mm:ss")
    private LocalDateTime documentAccountDate;

    private String label;

    private String documentAccountJournal;

    @JsonSerialize(using = MoneySerializer.class)
    private BigDecimal debit;

    @JsonSerialize(using = MoneySerializer.class)
    private BigDecimal credit;

    @JsonSerialize(using = MoneySerializer.class)
    private BigDecimal balance;

}
