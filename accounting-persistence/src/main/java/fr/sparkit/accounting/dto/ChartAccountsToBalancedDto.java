package fr.sparkit.accounting.dto;

import java.math.BigInteger;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChartAccountsToBalancedDto {

    private List<BigInteger> chartAccountsToBalanced;

}
