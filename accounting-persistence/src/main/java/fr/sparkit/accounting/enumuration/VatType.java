package fr.sparkit.accounting.enumuration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum VatType {
    TVA(1), FODEC(2);

    private int index;
}
