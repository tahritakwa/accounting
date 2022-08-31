package fr.sparkit.accounting.convertor;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import fr.sparkit.accounting.dto.StandardReportLineDto;
import fr.sparkit.accounting.entities.StandardReportLine;

public final class StandardReportLineConverter {

    private StandardReportLineConverter() {
        super();
    }

    public static StandardReportLineDto modelToDto(StandardReportLine standardReportLine) {
        if (standardReportLine == null) {
            return null;
        }
        return new StandardReportLineDto(standardReportLine.getId(), standardReportLine.getLabel(),
                standardReportLine.getFormula(), standardReportLine.getReportType(), standardReportLine.getLineIndex(),
                standardReportLine.getAnnexCode(), standardReportLine.isNegative(), standardReportLine.isTotal());
    }

    public static List<StandardReportLineDto> modelsToDtos(Collection<StandardReportLine> standardReportLines) {
        return standardReportLines.stream().filter(Objects::nonNull).map(StandardReportLineConverter::modelToDto)
                .collect(Collectors.toList());
    }
}
