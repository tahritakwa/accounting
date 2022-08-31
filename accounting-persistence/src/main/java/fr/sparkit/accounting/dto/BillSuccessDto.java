package fr.sparkit.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillSuccessDto {

    private Long billId;
    private Long documentId;
}
