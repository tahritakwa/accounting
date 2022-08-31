package fr.sparkit.accounting.dto;

import java.math.BigDecimal;

import javax.validation.constraints.Size;

import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.constraint.validator.NotNullField;
import fr.sparkit.accounting.util.errors.ApiErrors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class AccountDto {

    private Long id;
    private int code;
    @NotNullField(errorCode = ApiErrors.Accounting.ACCOUNT_MISSING_PARAMETERS)
    @Size(min = AccountingConstants.ENTITY_DEFAULT_LABEL_MIN_LENGTH, max = AccountingConstants.ENTITY_DEFAULT_LABEL_MAX_LENGTH)
    private String label;
    @NotNullField(errorCode = ApiErrors.Accounting.ACCOUNT_MISSING_PARAMETERS)
    private Long planId;
    private int planCode;
    @NotNullField(errorCode = ApiErrors.Accounting.ACCOUNT_MISSING_PARAMETERS)
    private BigDecimal debitOpening;
    @NotNullField(errorCode = ApiErrors.Accounting.ACCOUNT_MISSING_PARAMETERS)
    private BigDecimal creditOpening;
    private boolean literable;
    private boolean reconcilable;
    private Long tiersId;
    private boolean isDeleted;

    public AccountDto(Long id, int code, String label) {
        super();
        this.id = id;
        this.code = code;
        this.label = label;
    }

    public AccountDto(int code, String label) {
        super();
        this.code = code;
        this.label = label;
    }

}
