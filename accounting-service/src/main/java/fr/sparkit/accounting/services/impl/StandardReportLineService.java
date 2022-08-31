package fr.sparkit.accounting.services.impl;

import static fr.sparkit.accounting.constants.AccountingConstants.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import fr.sparkit.accounting.convertor.StandardReportLineConverter;
import fr.sparkit.accounting.dao.ReportLineDao;
import fr.sparkit.accounting.dao.StandardReportLineDao;
import fr.sparkit.accounting.dto.FiscalYearDto;
import fr.sparkit.accounting.dto.StandardReportLineDto;
import fr.sparkit.accounting.entities.ReportLine;
import fr.sparkit.accounting.entities.StandardReportLine;
import fr.sparkit.accounting.enumuration.ReportType;
import fr.sparkit.accounting.services.IFiscalYearService;
import fr.sparkit.accounting.services.IStandardReportLineService;
import fr.sparkit.accounting.util.CalculationUtil;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.errors.ErrorsResponse;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class StandardReportLineService extends GenericService<StandardReportLine, Long>
        implements IStandardReportLineService {

    private static final String LINE_INDEX_FORMAT = "(%d)";
    private final StandardReportLineDao standardReportLineDao;
    private final ReportLineService reportLineService;
    private final IFiscalYearService fiscalYearService;
    private final ReportLineDao reportLineDao;

    @Autowired
    public StandardReportLineService(StandardReportLineDao standardReportLineDao,
            @Lazy ReportLineService reportLineService, IFiscalYearService fiscalYearService,
            ReportLineDao reportLineDao) {
        this.standardReportLineDao = standardReportLineDao;
        this.reportLineService = reportLineService;
        this.fiscalYearService = fiscalYearService;
        this.reportLineDao = reportLineDao;
    }

    @Override
    public List<StandardReportLineDto> getReportLinesForReportType(ReportType reportType) {
        return StandardReportLineConverter
                .modelsToDtos(standardReportLineDao.findByReportTypeAndIsDeletedFalseOrderById(reportType));
    }

    @Override
    public StandardReportLine findByReportTypeAndLineIndexAndIsDeletedFalse(ReportType reportType, String index) {
        return standardReportLineDao.findByReportTypeAndLineIndexAndIsDeletedFalse(reportType, index);
    }

    @Override
    public List<StandardReportLine> findAllStandardReportLines() {
        List<StandardReportLine> standardReportLines = findAll();
        if (!standardReportLines.isEmpty()) {
            return standardReportLines;
        } else {
            log.error(NO_STANDARD_CONFIG_OF_REPORT);
            throw new HttpCustomException(ApiErrors.Accounting.REPORT_LINE_NO_DEFAULT_REPORT_CONFIGURATION);
        }
    }

    @Override
    public StandardReportLineDto updateStandardReport(StandardReportLineDto standardReportLine) {
        Optional<StandardReportLine> reportLineToBeSaved = standardReportLineDao.findById(standardReportLine.getId());
        if (reportLineToBeSaved.isPresent()) {
            if (standardReportLine.getAnnexCode() != null) {
                Optional<StandardReportLine> reportLineWithSameAnnexCode = standardReportLineDao
                        .findFirstByAnnexCodeAndReportType(standardReportLine.getAnnexCode(),
                                reportLineToBeSaved.get().getReportType());
                if (reportLineWithSameAnnexCode.isPresent()
                        && !reportLineWithSameAnnexCode.get().getId().equals(reportLineToBeSaved.get().getId())) {
                    log.error(ANNEX_CODE_ALREADY_USED, standardReportLine.getAnnexCode());
                    throw new HttpCustomException(ApiErrors.Accounting.REPORT_LINE_ANNEX_ALREADY_EXISTS,
                            new ErrorsResponse().error(standardReportLine.getAnnexCode()));
                }
                reportLineToBeSaved.get().setAnnexCode(standardReportLine.getAnnexCode());
            } else {
                reportLineToBeSaved.get().setAnnexCode(null);
            }
            reportLineToBeSaved.get().setFormula(standardReportLine.getFormula());
            reportLineToBeSaved.get().setNegative(standardReportLine.isNegative());
            validateFormula(reportLineToBeSaved.get());
            StandardReportLineDto savedStandardReportLine = StandardReportLineConverter
                    .modelToDto(saveAndFlush(reportLineToBeSaved.get()));
            List<FiscalYearDto> openedFiscalYears = fiscalYearService.findAllFiscalYearsNotClosed();
            for (FiscalYearDto openedFiscalYear : openedFiscalYears) {
                Optional<ReportLine> reportLineWithSameIndex = reportLineDao
                        .findByReportTypeAndLineIndexAndFiscalYearIdAndIsDeletedFalse(
                                savedStandardReportLine.getReportType(), savedStandardReportLine.getLineIndex(),
                                openedFiscalYear.getId());
                if (reportLineWithSameIndex.isPresent() && !reportLineWithSameIndex.get().isManuallyChanged()) {
                    reportLineWithSameIndex.get().setFormula(savedStandardReportLine.getFormula());
                    reportLineWithSameIndex.get().setAnnexCode(savedStandardReportLine.getAnnexCode());
                    reportLineWithSameIndex.get().setNegative(savedStandardReportLine.isNegative());
                    reportLineService.saveAndFlush(reportLineWithSameIndex.get());
                    log.info(LOG_ENTITY_UPDATED, reportLineWithSameIndex.get());
                }
            }
            return savedStandardReportLine;
        } else {
            log.error(REPORT_LINE_NO_EXIST_TO_SAVE);
            throw new HttpCustomException(ApiErrors.Accounting.REPORT_LINE_INEXISTANT_REPORT_LINE,
                    new ErrorsResponse().error(standardReportLine.getId()));
        }
    }

    @Override
    public StandardReportLine findById(Long id) {
        return Optional.ofNullable(standardReportLineDao.findOne(id))
                .orElseThrow(() -> new HttpCustomException(ApiErrors.Accounting.REPORT_LINE_INEXISTANT_REPORT_LINE,
                        new ErrorsResponse().error(id)));
    }

    @Override
    public void validateFormula(StandardReportLine standardReportLine) {
        log.info(VALID_REPORT_LINE_FORMULA, standardReportLine.getFormula());
        if (!CalculationUtil.isFormulaComplex(standardReportLine.getFormula())) {
            Collection<String> formulaElements = CalculationUtil
                    .divideSimpleStringFormulaToStringElements(standardReportLine.getFormula());
            checkForRepetitionsInFormula(Collections.singleton(formulaElements), standardReportLine.getFormula());
        } else {
            Collection<Integer> lineIndexes = CalculationUtil
                    .complexStringFormulaToCollection(standardReportLine.getFormula());
            for (Integer lineIndex : lineIndexes) {
                StandardReportLine reportLineWithIndex = standardReportLineDao
                        .findByReportTypeAndLineIndexAndIsDeletedFalse(standardReportLine.getReportType(),
                                String.format(LINE_INDEX_FORMAT, Math.abs(lineIndex)));
                if (reportLineWithIndex == null) {
                    log.error(INDEX_NO_EXIST_FOR_REPORT);
                    throw new HttpCustomException(ApiErrors.Accounting.REPORT_LINE_INDEX_LINE_NOT_FOUND,
                            new ErrorsResponse().error(String.format(LINE_INDEX_FORMAT, Math.abs(lineIndex))));
                } else if (reportLineWithIndex.getId() > standardReportLine.getId()) {
                    log.error(REPORT_LINE_INDEX_ORDER_INVALID);
                    throw new HttpCustomException(ApiErrors.Accounting.REPORT_LINE_INDEX_LINE_ORDER_INVALID,
                            new ErrorsResponse().error(String.format(LINE_INDEX_FORMAT, Math.abs(lineIndex))));
                }
            }
            checkForRepetitionsInFormula(Collections.singleton(lineIndexes), standardReportLine.getFormula());
        }
    }

    @Override
    public void checkForRepetitionsInFormula(Collection<Object> formulaElements, String formula) {
        if (formulaElements.size() != new HashSet<>(formulaElements).size()) {
            log.error(FORMULA_CONTAINS_DUPLICATE_ELEMENT, formula);
            throw new HttpCustomException(ApiErrors.Accounting.REPORT_LINE_FORMULA_CONTAINS_REPETITION,
                    new ErrorsResponse().error(formula));
        }
    }
}
