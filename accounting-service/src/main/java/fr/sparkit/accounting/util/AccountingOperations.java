package fr.sparkit.accounting.util;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Objects;

public final class AccountingOperations {
    private AccountingOperations(){
        super();
    }
    public static BigDecimal sumList(Collection<BigDecimal> values){
        if(values.isEmpty()){
            return BigDecimal.ZERO;
        }
        return values.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

