package fr.sparkit.accounting.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportMultipleBillDto {

    List<BillSuccessDto> billSuccessList;
    List<LocalDateTime> billIdNotInCurrentFiscalYear;
    List<String> listBillImported;
    List<BillDto> billFailedDtos;
    List<Integer> httpErrorCodes;
}