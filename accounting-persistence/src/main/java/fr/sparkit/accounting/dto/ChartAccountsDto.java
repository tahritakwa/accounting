package fr.sparkit.accounting.dto;

import java.util.List;

import javax.validation.constraints.Size;

import fr.sparkit.accounting.constants.AccountingConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChartAccountsDto {

    private Long id;
    private Long parentId;
    private int code;
    @Size(min = AccountingConstants.ENTITY_DEFAULT_LABEL_MIN_LENGTH, max = AccountingConstants.ENTITY_DEFAULT_LABEL_MAX_LENGTH)
    private String label;
    private List<ChartAccountsDto> children;

    public ChartAccountsDto(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
