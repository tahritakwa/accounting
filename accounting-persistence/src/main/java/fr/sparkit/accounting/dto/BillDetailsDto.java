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
public class BillDetailsDto {

    private BigDecimal pretaxAmount;
    private Long idTax;
    private String itemName;
    private BigDecimal vatAmount;
    private int vatType;

}
