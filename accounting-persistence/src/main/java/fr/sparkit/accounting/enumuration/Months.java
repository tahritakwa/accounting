package fr.sparkit.accounting.enumuration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Months {
    JANUARY("Janvier"), FEBRUARY("Février"), MARCH("Mars"), APRIL("Avril"), MAY("MAY"), JUNE("Juin"), JULY(
            "Juillet"), AUGUST(
                    "Aout"), SEPTEMBER("Septembre"), OCTOBER("Octobre"), NOVEMBER("Novembre"), DECEMBER("Decembre");

    private String value;
}
