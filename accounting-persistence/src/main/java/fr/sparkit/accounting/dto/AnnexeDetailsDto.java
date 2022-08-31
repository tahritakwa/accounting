package fr.sparkit.accounting.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AnnexeDetailsDto {

    private List<AccountBalanceDto> accounts;

    private String label;

    private String annexe;

    private BigDecimal totalAmount;
}
