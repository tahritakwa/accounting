package fr.sparkit.accounting.dto.excel;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import fr.sparkit.accounting.auditing.ChartAccountExcelCell;
import fr.sparkit.accounting.constants.LanguageConstants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChartAccountXLSXFormatDto {

    @ChartAccountExcelCell(headerName = LanguageConstants.XLSXHeaders.PARENT_PLAN_CODE_HEADER_NAME)
    private Integer parentPlanCode;

    @ChartAccountExcelCell(headerName = LanguageConstants.XLSXHeaders.PLAN_CODE_HEADER_NAME)
    private Integer planCode;

    @ChartAccountExcelCell(headerName = LanguageConstants.XLSXHeaders.LABEL_HEADER_NAME)
    private String label;

    private String error = "";

    public void setError(String error) {
        if (this.error.isEmpty()) {
            this.error = error;
        } else {
            this.error = this.error + " ; " + error;
        }
    }

    public static List<Field> getChartAccountExcelHeaderFields() {
        return Arrays.stream(ChartAccountXLSXFormatDto.class.getDeclaredFields())
                .filter(field -> field.getAnnotation(ChartAccountExcelCell.class) != null).collect(Collectors.toList());
    }

}
