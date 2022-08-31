package fr.sparkit.accounting.convertor;

import static fr.sparkit.accounting.constants.AccountingConstants.EMPTY_PREVIOUS_FISCAL_YEAR;
import static fr.sparkit.accounting.util.CalculationUtil.getAccountingDecimalFormat;
import static fr.sparkit.accounting.util.CalculationUtil.getFormattedBigDecimalValueOrEmptyStringIfZero;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import fr.sparkit.accounting.dto.PrintableReportLineDto;
import fr.sparkit.accounting.dto.ReportLineDto;
import fr.sparkit.accounting.entities.ReportLine;

public final class ReportLineConverter {
    private ReportLineConverter() {
        super();
    }

    public static ReportLineDto modelToDto(ReportLine reportLine) {
        if (reportLine == null) {
            return null;
        }
        return new ReportLineDto(reportLine.getId(), reportLine.getLabel(), reportLine.getFormula(),
                reportLine.getReportType(), reportLine.getLineIndex(), reportLine.getAnnexCode(),
                EMPTY_PREVIOUS_FISCAL_YEAR, BigDecimal.ZERO, FiscalYearConvertor.modelToDto(reportLine.getFiscalYear()),
                reportLine.getAmount(), reportLine.getUser(), reportLine.getLastUpdated(), reportLine.isNegative(),
                reportLine.isManuallyChanged(), reportLine.isTotal());
    }

    public static ReportLine dtoToModel(ReportLineDto reportLineDto) {
        return new ReportLine(reportLineDto.getId(), reportLineDto.getLabel(), reportLineDto.getFormula(),
                reportLineDto.getReportType(), reportLineDto.getLineIndex(), reportLineDto.getAnnexCode(),
                FiscalYearConvertor.dtoToModel(reportLineDto.getFiscalYear()), reportLineDto.getAmount(),
                reportLineDto.getUser(), reportLineDto.getLastUpdated(), reportLineDto.isNegative(),
                reportLineDto.isManuallyChanged(), reportLineDto.isTotal(), false, null);
    }

    public static PrintableReportLineDto dtoToPrintable(ReportLineDto reportLineDto) {
        String currentFiscalYearAmount = getAccountingDecimalFormat().format(BigDecimal.ZERO);

        String previousFiscalYearAmount = getAccountingDecimalFormat().format(BigDecimal.ZERO);

        if (reportLineDto.isTotal()) {
            if (reportLineDto.getAmount() != null) {
                currentFiscalYearAmount = getAccountingDecimalFormat().format(reportLineDto.getAmount());
            }
            if (reportLineDto.getPreviousFiscalYearAmount() != null) {
                previousFiscalYearAmount = getAccountingDecimalFormat()
                        .format(reportLineDto.getPreviousFiscalYearAmount());
            }
        } else {
            currentFiscalYearAmount = getFormattedBigDecimalValueOrEmptyStringIfZero(reportLineDto.getAmount());
            previousFiscalYearAmount = getFormattedBigDecimalValueOrEmptyStringIfZero(
                    reportLineDto.getPreviousFiscalYearAmount());
        }
        return new PrintableReportLineDto(reportLineDto.getId(), reportLineDto.getLabel(),
                reportLineDto.getReportType(), reportLineDto.getLineIndex(), reportLineDto.getAnnexCode(),
                reportLineDto.getPreviousFiscalYear(), previousFiscalYearAmount, reportLineDto.getFiscalYear(),
                currentFiscalYearAmount);
    }

    public static List<ReportLineDto> modelsToDtos(Collection<ReportLine> reportLines) {
        return reportLines.stream().filter(Objects::nonNull).map(ReportLineConverter::modelToDto)
                .collect(Collectors.toList());
    }

    public static List<ReportLine> dtosToModels(Collection<ReportLineDto> reportLines) {
        return reportLines.stream().filter(Objects::nonNull).map(ReportLineConverter::dtoToModel)
                .collect(Collectors.toList());
    }

    public static Map<String, PrintableReportLineDto> annualReportToMap(Iterable<ReportLineDto> generateAnnualReport) {
        Map<String, PrintableReportLineDto> generateAnnualReportMap = new HashMap<>();
        for (ReportLineDto reportLine : generateAnnualReport) {
            if (reportLine.getAmount().compareTo(BigDecimal.ZERO) == 0) {
                reportLine.setAmount(null);
            }
            if (reportLine.getPreviousFiscalYearAmount().compareTo(BigDecimal.ZERO) == 0) {
                reportLine.setPreviousFiscalYearAmount(null);
            }
        }
        generateAnnualReport.forEach((ReportLineDto reportLine) -> generateAnnualReportMap
                .put(reportLine.getLineIndex() + " " + reportLine.getReportType(), dtoToPrintable(reportLine)));
        return generateAnnualReportMap;
    }

    public static List<PrintableReportLineDto> dtosToPrintables(Iterable<ReportLineDto> reportLines) {
        List<PrintableReportLineDto> printableReportLines = new ArrayList<>();
        reportLines.forEach((ReportLineDto line) -> {
            PrintableReportLineDto printableReportLineDto = dtoToPrintable(line);
            printableReportLines.add(printableReportLineDto);
        });
        return printableReportLines;
    }
}
