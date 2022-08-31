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
public class AccountStateDto {
    private Long id;
    private int code;
    private String label;
    private BigDecimal sumCredit;
    private BigDecimal sumDebit;

}
