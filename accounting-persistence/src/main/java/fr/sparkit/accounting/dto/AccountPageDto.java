package fr.sparkit.accounting.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AccountPageDto {

    private List<AccountDto> listAccountDto;
    private Long total;
}
