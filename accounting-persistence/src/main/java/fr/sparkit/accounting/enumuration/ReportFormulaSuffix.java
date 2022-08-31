package fr.sparkit.accounting.enumuration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ReportFormulaSuffix {
    CREDITOR('c'), DEBTOR('d'), RESULT('r');

    private final Character value;

    public static boolean contains(Character suffixCharacter) {
        for (ReportFormulaSuffix suffix : ReportFormulaSuffix.values()) {
            if (suffix.getValue().equals(suffixCharacter)) {
                return true;
            }
        }
        return false;
    }
}
