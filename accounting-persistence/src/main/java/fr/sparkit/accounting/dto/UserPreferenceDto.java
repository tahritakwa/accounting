package fr.sparkit.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UserPreferenceDto {
    private Long id;
    private String userEmail;
    private long journalId;
    private long currentFiscalYearId;

}
