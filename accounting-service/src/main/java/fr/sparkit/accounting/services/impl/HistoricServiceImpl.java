package fr.sparkit.accounting.services.impl;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import fr.sparkit.accounting.auditing.HistoricActionEnum;
import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.constants.DocumentAccountFieldsConstants;
import fr.sparkit.accounting.constants.FiscalYearFieldsConstants;
import fr.sparkit.accounting.constants.ReportLineFieldsConstants;
import fr.sparkit.accounting.convertor.DocumentAccountingLineConvertor;
import fr.sparkit.accounting.convertor.HistoricConverter;
import fr.sparkit.accounting.convertor.ReportLineConverter;
import fr.sparkit.accounting.dao.AccountDao;
import fr.sparkit.accounting.dao.DocumentAccountDao;
import fr.sparkit.accounting.dao.DocumentAccountLineDao;
import fr.sparkit.accounting.dao.FiscalYearDao;
import fr.sparkit.accounting.dao.IHistoricDao;
import fr.sparkit.accounting.dao.JournalDao;
import fr.sparkit.accounting.dao.ReportLineDao;
import fr.sparkit.accounting.dto.DocumentAccountLineDto;
import fr.sparkit.accounting.dto.HistoricDto;
import fr.sparkit.accounting.dto.HistoricDtoPage;
import fr.sparkit.accounting.dto.HistoricSearchFieldDto;
import fr.sparkit.accounting.dto.ReportLineDto;
import fr.sparkit.accounting.dto.SortDto;
import fr.sparkit.accounting.entities.Account;
import fr.sparkit.accounting.entities.DocumentAccount;
import fr.sparkit.accounting.entities.DocumentAccountLine;
import fr.sparkit.accounting.entities.FiscalYear;
import fr.sparkit.accounting.entities.Historic;
import fr.sparkit.accounting.entities.Journal;
import fr.sparkit.accounting.entities.ReportLine;
import fr.sparkit.accounting.enumuration.FiscalYearClosingState;
import fr.sparkit.accounting.enumuration.ReportType;
import fr.sparkit.accounting.services.IAccountingConfigurationService;
import fr.sparkit.accounting.services.IHistoricService;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.http.HttpCustomException;

@Service
public class HistoricServiceImpl extends GenericService<Historic, Long> implements IHistoricService {

    public static final String CREATED_DATE = "createdDate";
    public static final String ENTITY_FIELD = "entityField";
    public static final String ACTION = "action";
    public static final String ENTITY = "entity";
    public static final String ENTITY_ID = "entityId";
    public static final String IS_DELETED = "isDeleted";
    public static final String UPDATED_ACTION = "UPDATED";
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    private final EntityManager em;
    private final IHistoricDao historicDao;
    private final DocumentAccountLineDao documentAccountLineDao;
    private final JournalDao journalDao;
    private final AccountDao accountDao;
    private final ReportLineDao reportLineDao;
    private final FiscalYearDao fiscalYearDao;
    private final IAccountingConfigurationService accountingConfigurationService;
    public static final String DOCUMENT_ACCOUNT_LINE = "DocumentAccountLine";

    @Autowired
    public HistoricServiceImpl(EntityManager em, IHistoricDao historicDao, JournalDao journalDao,
            DocumentAccountLineDao documentAccountLineDao, AccountDao accountDao, ReportLineDao reportLineDao,
            IAccountingConfigurationService accountingConfigurationService, DocumentAccountDao documentAccountDao,
            FiscalYearDao fiscalYearDao) {
        this.em = em;
        this.historicDao = historicDao;
        this.documentAccountLineDao = documentAccountLineDao;
        this.journalDao = journalDao;
        this.accountDao = accountDao;
        this.reportLineDao = reportLineDao;
        this.accountingConfigurationService = accountingConfigurationService;
        this.fiscalYearDao = fiscalYearDao;
    }

    @Override
    public Page<Historic> getHistoricByEntity(String entityName, Long entityId, String startDate, String endDate,
            String searchValue, Pageable pageable, SortDto sortDto) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Historic> criteria = builder.createQuery(Historic.class);
        Root<Historic> historicRoot = criteria.from(Historic.class);
        List<Predicate> predicateList = initDefaultPredicates(builder, historicRoot);
        setEntityPredicate(entityName, entityId, searchValue, builder, historicRoot, predicateList);
        if (!startDate.isEmpty() && endDate.isEmpty() && searchValue.isEmpty()) {
            setStartDatePredicate(startDate, builder, historicRoot, predicateList);
            return getHistoricsByFiltre(builder, criteria, historicRoot, pageable, predicateList, sortDto);
        } else if (!endDate.isEmpty() && startDate.isEmpty() && searchValue.isEmpty()) {
            setEndDatePredicate(endDate, builder, historicRoot, predicateList);
            return getHistoricsByFiltre(builder, criteria, historicRoot, pageable, predicateList, sortDto);
        } else if (!endDate.isEmpty() && !startDate.isEmpty() && searchValue.isEmpty()) {
            setStartAndEndDatePredicate(startDate, endDate, builder, historicRoot, predicateList);
            return getHistoricsByFiltre(builder, criteria, historicRoot, pageable, predicateList, sortDto);
        } else if (!endDate.isEmpty() && !startDate.isEmpty()) {
            setStartAndEndDatePredicate(startDate, endDate, builder, historicRoot, predicateList);
            setEntityFieldPredicate(searchValue, builder, historicRoot, predicateList);
            return getHistoricsByFiltre(builder, criteria, historicRoot, pageable, predicateList, sortDto);
        } else if (!startDate.isEmpty()) {
            setStartDatePredicate(startDate, builder, historicRoot, predicateList);
            setEntityFieldPredicate(searchValue, builder, historicRoot, predicateList);
            return getHistoricsByFiltre(builder, criteria, historicRoot, pageable, predicateList, sortDto);
        } else if (!endDate.isEmpty()) {
            setEndDatePredicate(endDate, builder, historicRoot, predicateList);
            setEntityFieldPredicate(searchValue, builder, historicRoot, predicateList);
            return getHistoricsByFiltre(builder, criteria, historicRoot, pageable, predicateList, sortDto);
        } else if (!searchValue.isEmpty()) {
            setEntityFieldPredicate(searchValue, builder, historicRoot, predicateList);
            return getHistoricsByFiltre(builder, criteria, historicRoot, pageable, predicateList, sortDto);
        } else {
            return historicEmptyFiltre(entityName, entityId, pageable, sortDto);
        }
    }

    private static void setEntityFieldPredicate(String searchValue, CriteriaBuilder builder,
            Root<Historic> historicRoot, List<Predicate> predicateList) {
        if (!DOCUMENT_ACCOUNT_LINE.equals(searchValue)) {
            predicateList.add(builder.or(builder.equal(historicRoot.get(ENTITY_FIELD), searchValue),
                    builder.equal(historicRoot.get(ACTION), searchValue)));
        }

    }

    private static void setStartAndEndDatePredicate(String startDate, String endDate, CriteriaBuilder builder,
            Root<Historic> historicRoot, List<Predicate> predicateList) {
        LocalDateTime startDateLocalDate = parseStringDateToLocalDateTime(startDate);
        LocalDateTime endDateLocalDate = parseStringDateToLocalDateTime(endDate);
        predicateList.add(builder.greaterThanOrEqualTo(historicRoot.get(CREATED_DATE), startDateLocalDate));
        predicateList.add(builder.lessThanOrEqualTo(historicRoot.get(CREATED_DATE), endDateLocalDate));

    }

    private void setEntityPredicate(String entityName, Long entityId, String searchValue, CriteriaBuilder builder,
            Root<Historic> historicRoot, List<Predicate> predicateList) {
        if (DocumentAccount.class.getSimpleName().equals(entityName)) {
            List<Long> documentAccountLineIds = documentAccountLineDao.findByDocumentAccountId(entityId).stream()
                    .map(DocumentAccountLine::getId).collect(Collectors.toList());
            if (documentAccountLineIds.isEmpty()) {
                documentAccountLineIds.add(0L);
            }
            initPredicateDocumentAccountEntity(entityId, searchValue, builder, historicRoot, predicateList,
                    documentAccountLineIds);
        } else {
            predicateList.add(builder.or(
                    builder.and(builder.equal(historicRoot.get(ENTITY), entityName),
                            builder.equal(historicRoot.get(ENTITY_ID), entityId)),
                    builder.equal(historicRoot.get(ACTION), UPDATED_ACTION)));
        }
    }

    private static void initPredicateDocumentAccountEntity(Long entityId, String searchValue, CriteriaBuilder builder,
            Root<Historic> historicRoot, List<Predicate> predicateList, List<Long> documentAccountLineIds) {
        if (DOCUMENT_ACCOUNT_LINE.equals(searchValue)) {
            predicateList.add(builder.or(
                    builder.and(builder.equal(historicRoot.get(ENTITY), DocumentAccount.class.getSimpleName()),
                            builder.equal(historicRoot.get(ENTITY_ID), entityId),
                            builder.equal(historicRoot.get(ENTITY_FIELD), searchValue)),
                    builder.and(builder.equal(historicRoot.get(ENTITY), DocumentAccountLine.class.getSimpleName()),
                            builder.in(historicRoot.get(ENTITY_ID)).value(documentAccountLineIds))));
        } else {
            predicateList.add(builder.or(
                    builder.and(builder.equal(historicRoot.get(ENTITY), DocumentAccount.class.getSimpleName()),
                            builder.equal(historicRoot.get(ENTITY_ID), entityId)),
                    builder.and(builder.equal(historicRoot.get(ENTITY), DocumentAccountLine.class.getSimpleName()),
                            builder.in(historicRoot.get(ENTITY_ID)).value(documentAccountLineIds))));
        }
    }

    private static void setEndDatePredicate(String endDate, CriteriaBuilder builder, Root<Historic> historicRoot,
            List<Predicate> predicateList) {
        LocalDateTime searchLocalDate = parseStringDateToLocalDateTime(endDate);
        predicateList.add(builder.lessThanOrEqualTo(historicRoot.get(CREATED_DATE), searchLocalDate));
    }

    private static void setStartDatePredicate(String startDate, CriteriaBuilder builder, Root<Historic> historicRoot,
            List<Predicate> predicateList) {
        LocalDateTime searchLocalDate = parseStringDateToLocalDateTime(startDate);
        predicateList.add(builder.greaterThanOrEqualTo(historicRoot.get(CREATED_DATE), searchLocalDate));
    }

    private static List<Predicate> initDefaultPredicates(CriteriaBuilder builder, Root<Historic> historicRoot) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(builder.equal(historicRoot.get(IS_DELETED), Boolean.FALSE));
        return predicates;
    }

    private Page<Historic> historicEmptyFiltre(String entityName, Long entityId, Pageable pageable, SortDto sortDto) {
        if (sortDto != null && sortDto.areAllFieldsNotNull()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    pageable.getSort().and(Sort.by(sortDto.getDirection(), sortDto.getField())));
        } else {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    pageable.getSort().and(Sort.by(Sort.Direction.DESC, "id")));
        }
        Page<Historic> historicPage;
        if (entityName.equals(DocumentAccount.class.getSimpleName())) {
            historicPage = historicDao.findHistoricDocumentAccountAndIsDeletedFalse(
                    DocumentAccount.class.getSimpleName(), DocumentAccountLine.class.getSimpleName(), entityId,
                    pageable);
        } else {
            historicPage = historicDao.findByEntityAndEntityIdAndIsDeletedFalse(entityName, entityId, pageable);
        }

        return historicPage;
    }

    private static LocalDateTime parseStringDateToLocalDateTime(String searchDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(searchDate, formatter);
    }

    Page<Historic> getHistoricsByFiltre(CriteriaBuilder builder, CriteriaQuery<Historic> criteria,
            Root<Historic> historicRoot, Pageable pageable, List<Predicate> predicateList, SortDto sortDto) {
        if (sortDto != null && sortDto.areAllFieldsNotNull()) {
            if (sortDto.getDirection() == Sort.Direction.ASC) {
                criteria.orderBy(builder.asc(historicRoot.get(sortDto.getField())));
            } else if (sortDto.getDirection() == Sort.Direction.DESC) {
                criteria.orderBy(builder.desc(historicRoot.get(sortDto.getField())));
            }
        } else {
            criteria.orderBy(builder.desc(historicRoot.get("id")));
        }

        criteria.where(builder.and(predicateList.toArray(new Predicate[0])));
        List<Historic> result = em.createQuery(criteria).setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize()).getResultList();

        CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
        Root<Historic> historicsRootCount = countQuery.from(Historic.class);
        countQuery.select(builder.count(historicsRootCount))
                .where(builder.and(predicateList.toArray(new Predicate[0])));

        Long count = em.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(result, pageable, count);
    }

    @Override
    public HistoricDtoPage getHistoricDocumentAccount(Long entityId, String startDate, String endDate,
            String searchValue, Pageable pageable, SortDto sortDto) {

        HistoricDtoPage documentAccountHistoricDtoPage = HistoricConverter.pageModelToPageDto(getHistoricByEntity(
                DocumentAccount.class.getSimpleName(), entityId, startDate, endDate, searchValue, pageable, sortDto));
        List<Long> listIdsDocumentAccountLine = documentAccountHistoricDtoPage.getHistoricDtoList().stream()
                .filter(historic -> DocumentAccountLine.class.getSimpleName().equals(historic.getEntity()))
                .map(HistoricDto::getEntityId).distinct().collect(Collectors.toList());
        List<DocumentAccountLineDto> listLine = DocumentAccountingLineConvertor
                .modelsToDtos(documentAccountLineDao.findAllByIdIn(listIdsDocumentAccountLine));

        documentAccountHistoricDtoPage.setHistoricDtoList(
                documentAccountHistoricDtoPage.getHistoricDtoList().stream().peek(this::checkHistoryField)
                        .peek(historic -> getDocumentAccountLine(historic, listLine)).collect(Collectors.toList()));

        return documentAccountHistoricDtoPage;
    }

    public void getDocumentAccountLine(HistoricDto historicDto, List<DocumentAccountLineDto> listLine) {
        if (DocumentAccountLine.class.getSimpleName().equals(historicDto.getEntity())) {
            historicDto.setDocumentAccountLineAffected(listLine.stream()
                    .filter(line -> line.getId().equals(historicDto.getEntityId())).collect(Collectors.toList()));
        }
    }
    public void checkHistoryField(HistoricDto historicDto) {
        switch (historicDto.getEntityField()) {
        case DocumentAccountFieldsConstants.DOCUMENT_DATE_FIELD:
            historicDto.setEntityField(DocumentAccountFieldsConstants.DOCUMENT_DATE_TITLE);
            break;
        case DocumentAccountFieldsConstants.CODE_DOCUMENT_FIELD:
            historicDto.setEntityField(DocumentAccountFieldsConstants.CODE_DOCUMENT_TITLE);
            break;
        case DocumentAccountFieldsConstants.LABEL_DOCUMENT_ACCOUNT_FIELD:
            historicDto.setEntityField(DocumentAccountFieldsConstants.LABEL_DOCUMENT_ACCOUNT_TITLE);
            break;
        case DocumentAccountFieldsConstants.JOURNAL_FIELD:
            if (historicDto.getFieldOldValue() != null) {
                Journal oldJournal = journalDao.findByIdEvenIfIsDeleted(Long.valueOf(historicDto.getFieldOldValue()))
                        .orElseThrow(() -> new HttpCustomException(ApiErrors.Accounting.JOURNAL_NOT_FOUND));
                historicDto.setFieldOldValue(oldJournal.getLabel());
            }
            if (historicDto.getFieldNewValue() != null) {
                Journal newJournal = journalDao.findByIdEvenIfIsDeleted(Long.valueOf(historicDto.getFieldNewValue()))
                        .orElseThrow(() -> new HttpCustomException(ApiErrors.Accounting.JOURNAL_NOT_FOUND));
                historicDto.setFieldNewValue(newJournal.getLabel());
            }
            historicDto.setEntityField(DocumentAccountFieldsConstants.JOURNAL_TITLE);
            break;
        case DocumentAccountFieldsConstants.INDEX_OF_STATUS_FIELD:
            historicDto.setEntityField(DocumentAccountFieldsConstants.INDEX_OF_STATUS_TITLE);
            break;
        case DocumentAccountFieldsConstants.DOCUMENT_LINE_DATE_FIELD:
            historicDto.setEntityField(DocumentAccountFieldsConstants.DOCUMENT_LINE_DATE_TITLE);
            break;
        case DocumentAccountFieldsConstants.REFERENCE_FIELD:
            historicDto.setEntityField(DocumentAccountFieldsConstants.REFERENCE_TITLE);
            break;
        case DocumentAccountFieldsConstants.DEBIT_AMOUNT_FIELD:
            historicDto.setEntityField(DocumentAccountFieldsConstants.DEBIT_AMOUNT_TITLE);
            break;
        case DocumentAccountFieldsConstants.CREDIT_AMOUNT_FIELD:
            historicDto.setEntityField(DocumentAccountFieldsConstants.CREDIT_AMOUNT_TITLE);
            break;
        case DocumentAccountFieldsConstants.LETTER_FIELD:
            historicDto.setEntityField(DocumentAccountFieldsConstants.LETTER_TITLE);
            break;
        case DocumentAccountFieldsConstants.DOCUMENT_ACCOUNT_FIELD:
            historicDto.setEntityField(DocumentAccountFieldsConstants.DOCUMENT_ACCOUNT_TITLE);
            break;
        case DocumentAccountFieldsConstants.ACCOUNT_FIELD:
            if (historicDto.getFieldOldValue() != null) {
                Account oldAccount = accountDao.findByIdEvenIfIsDeleted(Long.valueOf(historicDto.getFieldOldValue()))
                        .orElseThrow(() -> new HttpCustomException(ApiErrors.Accounting.ENTITY_NOT_FOUND));
                historicDto.setFieldOldValue(oldAccount.getCode() + " - " + oldAccount.getLabel());
            }
            if (historicDto.getFieldNewValue() != null) {
                Account newAccount = accountDao.findByIdEvenIfIsDeleted(Long.valueOf(historicDto.getFieldNewValue()))
                        .orElseThrow(() -> new HttpCustomException(ApiErrors.Accounting.ENTITY_NOT_FOUND));
                historicDto.setFieldNewValue(newAccount.getCode() + " - " + newAccount.getLabel());
            }
            historicDto.setEntityField(DocumentAccountFieldsConstants.ACCOUNT_TITLE);
            break;
        case DocumentAccountFieldsConstants.FISCAL_YEAR_FIELD:
            historicDto.setEntityField(DocumentAccountFieldsConstants.FISCAL_YEAR_TITLE);
            break;
        case DocumentAccountFieldsConstants.RECONCILIATION_DATE_FIELD:
            historicDto.setEntityField(DocumentAccountFieldsConstants.RECONCILIATION_DATE_TITLE);
            break;
        case DocumentAccountFieldsConstants.IS_CLOSE_STATE_FIELD:
            historicDto.setEntityField(DocumentAccountFieldsConstants.IS_CLOSE_STATE_TITLE);
            break;
        case DocumentAccountFieldsConstants.IS_DELETED_STATE_FIELD:
            historicDto.setEntityField(DocumentAccountFieldsConstants.IS_DELETED_STATE_TITLE);
            break;
        default:
            historicDto.setEntityField(historicDto.getEntityField());
            break;

        }
    }

    /**
     * filter document Accout fields for search and convert them into title
     */
    @Override
    public List<HistoricSearchFieldDto> getDocumentAccountHistorySearchFields() {
        List<HistoricSearchFieldDto> fieldsList = Arrays.stream(DocumentAccount.class.getDeclaredFields())
                .filter(this::filtreDocumentAccountFields).map(Field::getName).map(this::createHistoryFiledSearch)
                .collect(Collectors.toList());
        fieldsList.add(new HistoricSearchFieldDto(DOCUMENT_ACCOUNT_LINE, "DOCUMENT_ACCOUNT_LINE"));
        fieldsList.addAll(Arrays.stream(DocumentAccountLine.class.getDeclaredFields())
                .filter(this::filtreDocumentAccountLineFields).map(Field::getName).map(this::createHistoryFiledSearch)
                .collect(Collectors.toList()));
        return fieldsList;
    }

    private boolean filtreDocumentAccountFields(Field field) {
        return isFiltredFields(field) && !field.getType().equals(UUID.class)
                && !java.lang.reflect.Modifier.isStatic(field.getModifiers());
    }

    private boolean filtreDocumentAccountLineFields(Field field) {
        return !"id".equals(field.getName()) && !field.getType().equals(UUID.class)
                && !java.lang.reflect.Modifier.isStatic(field.getModifiers());
    }

    private boolean isFiltredFields(Field field) {
        return !IS_DELETED.equals(field.getName()) && !"id".equals(field.getName())
                && !"creationDocumentDate".equals(field.getName());
    }

    private HistoricSearchFieldDto createHistoryFiledSearch(String s) {
        return new HistoricSearchFieldDto(s, checkHistoryField(s));
    }

    private static String checkHistoryField(String field) {
        switch (field) {
        case DocumentAccountFieldsConstants.DOCUMENT_DATE_FIELD:
            return DocumentAccountFieldsConstants.DOCUMENT_DATE_TITLE;
        case DocumentAccountFieldsConstants.CODE_DOCUMENT_FIELD:
            return DocumentAccountFieldsConstants.CODE_DOCUMENT_TITLE;
        case DocumentAccountFieldsConstants.LABEL_DOCUMENT_ACCOUNT_FIELD:
            return DocumentAccountFieldsConstants.LABEL_DOCUMENT_ACCOUNT_TITLE;
        case DocumentAccountFieldsConstants.JOURNAL_FIELD:
            return DocumentAccountFieldsConstants.JOURNAL_TITLE;
        case DocumentAccountFieldsConstants.INDEX_OF_STATUS_FIELD:
            return DocumentAccountFieldsConstants.INDEX_OF_STATUS_TITLE;
        case DocumentAccountFieldsConstants.DOCUMENT_LINE_DATE_FIELD:
            return DocumentAccountFieldsConstants.DOCUMENT_LINE_DATE_TITLE;
        case DocumentAccountFieldsConstants.REFERENCE_FIELD:
            return DocumentAccountFieldsConstants.REFERENCE_TITLE;
        case DocumentAccountFieldsConstants.DEBIT_AMOUNT_FIELD:
            return DocumentAccountFieldsConstants.DEBIT_AMOUNT_TITLE;
        case DocumentAccountFieldsConstants.CREDIT_AMOUNT_FIELD:
            return DocumentAccountFieldsConstants.CREDIT_AMOUNT_TITLE;
        case DocumentAccountFieldsConstants.LETTER_FIELD:
            return DocumentAccountFieldsConstants.LETTER_TITLE;
        case DocumentAccountFieldsConstants.DOCUMENT_ACCOUNT_FIELD:
            return DocumentAccountFieldsConstants.DOCUMENT_ACCOUNT_TITLE;
        case DocumentAccountFieldsConstants.ACCOUNT_FIELD:
            return DocumentAccountFieldsConstants.ACCOUNT_TITLE;
        case DocumentAccountFieldsConstants.RECONCILIATION_DATE_FIELD:
            return DocumentAccountFieldsConstants.RECONCILIATION_DATE_TITLE;
        case DocumentAccountFieldsConstants.FISCAL_YEAR_FIELD:
            return DocumentAccountFieldsConstants.FISCAL_YEAR_TITLE;
        case DocumentAccountFieldsConstants.IS_CLOSE_STATE_FIELD:
            return DocumentAccountFieldsConstants.IS_CLOSE_STATE_TITLE;
        case DocumentAccountFieldsConstants.IS_DELETED_STATE_FIELD:
            return DocumentAccountFieldsConstants.IS_DELETED_STATE_TITLE;
        default:
            return field;
        }
    }

    @Override
    public HistoricDtoPage gethistoricDocumentAccountLine(Long entityId, Pageable pageable) {
        List<Long> documentAccountLineIds = documentAccountLineDao.findByDocumentAccountId(entityId).stream()
                .map(DocumentAccountLine::getId).collect(Collectors.toList());
        if (documentAccountLineIds.isEmpty()) {
            documentAccountLineIds.add(0L);
        }
        DateTimeFormatter df = DateTimeFormatter.ofPattern(DATE_PATTERN);
        Page<Object[]> values = historicDao.getHistoriqueDocumentAccount(documentAccountLineIds, entityId, pageable);
        HistoricDtoPage documentAccountHistoricDtoPage = new HistoricDtoPage();
        documentAccountHistoricDtoPage.setTotalElements(values.getTotalElements());
        documentAccountHistoricDtoPage.setHistoricDtoList(values.getContent().stream()
                .map(historicItem -> new HistoricDto(LocalDate.parse(historicItem[0].toString(), df).atStartOfDay(),
                        HistoricActionEnum.valueOf(historicItem[1].toString()), historicItem[2].toString(),
                        historicItem[3].toString(), historicItem[4].toString(), historicItem[5].toString()))
                .sorted(Comparator.comparing(HistoricDto::getCreatedDate, Comparator.nullsLast(Comparator.naturalOrder()))).collect(Collectors.toList()));
        List<DocumentAccountLineDto> listLine = DocumentAccountingLineConvertor
                .modelsToDtos(documentAccountLineDao.findAllByIdIn(documentAccountLineIds));
        documentAccountHistoricDtoPage.setHistoricDtoList(documentAccountHistoricDtoPage.getHistoricDtoList().stream()
                .peek(historic -> fillAllData(historic, listLine)).collect(Collectors.toList()));
        return documentAccountHistoricDtoPage;
    }

    public void fillAllData(HistoricDto historicDto, List<DocumentAccountLineDto> listLine) {
        getDocumentAccountFromEntityId(historicDto, listLine);
        if (HistoricActionEnum.UPDATED.equals(historicDto.getAction()) && historicDto.getIdsList() != null) {
            List<HistoricDto> list = HistoricConverter
                    .modelsToDtos(historicDao.findAllById(Arrays.asList(historicDto.getIdsList().trim().split(","))
                            .stream().map(Long::parseLong).collect(Collectors.toList())));
            historicDto.setHistoricList(list.stream().peek(this::checkHistoryField)
                    .peek(historic -> getDocumentAccountLine(historic, listLine)).collect(Collectors.toList()));
        }
        if (historicDto.getEntity().equals(DocumentAccount.class.getSimpleName())) {
            historicDto.setEntity(DocumentAccountFieldsConstants.DOCUMENT_ACCOUNT);
        } else if (historicDto.getEntity().equals(DocumentAccountLine.class.getSimpleName())) {
            historicDto.setEntity(DocumentAccountFieldsConstants.DOCUMENT_ACCOUNT_LINE);
        }
    }

    @Override
    public List<ReportLineDto> getReportLineFromHistoric(String reportType) {
        List<Long> reportLineIds = historicDao.getEntityIdsList(ReportLine.class.getSimpleName());
        return ReportLineConverter
                .modelsToDtos(this.reportLineDao.findByReportTypeAndFiscalYearIdAndIsDeletedFalseAndIdInOrderById(
                        ReportType.valueOf(reportType.toUpperCase(AccountingConstants.LANGUAGE)),
                        accountingConfigurationService.getCurrentFiscalYearId(), reportLineIds));
    }

    public void checkHistoryFieldReportLine(HistoricDto historicDto) {
        switch (historicDto.getEntityField()) {
        case ReportLineFieldsConstants.LABEL_FIELD:
            historicDto.setEntityField(ReportLineFieldsConstants.LABEL_TITLE);
            break;
        case ReportLineFieldsConstants.FORMULA_FIELD:
            historicDto.setEntityField(ReportLineFieldsConstants.FORMULA_TITLE);
            break;
        case ReportLineFieldsConstants.REPORT_TYPE_FIELD:
            historicDto.setEntityField(ReportLineFieldsConstants.REPORT_TYPE_TITLE);
            break;
        case ReportLineFieldsConstants.LINE_INDEX_FIELD:
            historicDto.setEntityField(ReportLineFieldsConstants.LINE_INDEX_TITLE);
            break;
        case ReportLineFieldsConstants.ANNEX_CODE_FIELD:
            historicDto.setEntityField(ReportLineFieldsConstants.ANNEX_CODE_TITLE);
            break;
        case ReportLineFieldsConstants.FISCAL_YEAR_FIELD:
            historicDto.setEntityField(ReportLineFieldsConstants.FISCAL_YEAR_TITLE);
            break;
        case ReportLineFieldsConstants.AMOUNT_FIELD:
            historicDto.setEntityField(ReportLineFieldsConstants.AMOUNT_TITLE);
            break;
        case ReportLineFieldsConstants.USER_FIELD:
            historicDto.setEntityField(ReportLineFieldsConstants.USER_TITLE);
            break;
        case ReportLineFieldsConstants.LAST_UPDATED_FIELD:
            historicDto.setEntityField(ReportLineFieldsConstants.LAST_UPDATED_TITLE);
            break;
        case ReportLineFieldsConstants.IS_NEGATIVE_FIELD:
            setFieldBoolean(historicDto, "POSITIVE", "NEGATIVE");
            historicDto.setEntityField(ReportLineFieldsConstants.IS_NEGATIVE_TITLE);
            break;
        case ReportLineFieldsConstants.IS_MANUALLY_CHANGED_FIELD:
            setFieldBoolean(historicDto, "YES", "NO");
            historicDto.setFieldOldValue(" - ");
            historicDto.setEntityField(ReportLineFieldsConstants.IS_MANUALLY_CHANGED_TITLE);
            break;
        case ReportLineFieldsConstants.IS_TOTAL_FIELD:
            setFieldBoolean(historicDto, "YES", "NO");
            historicDto.setEntityField(ReportLineFieldsConstants.IS_TOTAL_TITLE);
            break;
        case ReportLineFieldsConstants.IS_DELETED_STATE_FIELD:
            setFieldBoolean(historicDto, "YES", "NO");
            historicDto.setEntityField(ReportLineFieldsConstants.IS_DELETED_STATE_TITLE);
            break;
        default:
            historicDto.setEntityField(historicDto.getEntityField());
            break;
        }
    }

    private void setFieldBoolean(HistoricDto historicDto, String trueValue, String falseValue) {
        historicDto.setFieldNewValue(
                historicDto.getFieldNewValue().equalsIgnoreCase(Boolean.TRUE.toString()) ? trueValue : falseValue);

        historicDto.setFieldOldValue(
                historicDto.getFieldOldValue().equalsIgnoreCase(Boolean.TRUE.toString()) ? trueValue : falseValue);
    }

    @Override
    public HistoricDtoPage getHistoricReportLine(Long entityId, Pageable pageable) {
        HistoricDtoPage reportLineHistoricDtoPage = HistoricConverter.pageModelToPageDto(historicDao
                .findByEntityAndEntityIdAndIsDeletedFalse(ReportLine.class.getSimpleName(), entityId, pageable));

        reportLineHistoricDtoPage.setHistoricDtoList(reportLineHistoricDtoPage.getHistoricDtoList().stream()
                .peek(this::checkHistoryFieldReportLine).collect(Collectors.toList()));
        return reportLineHistoricDtoPage;
    }

    @Override
    public HistoricDtoPage getLettringHistoric(Long accountId, Pageable pageable) {
        List<Long> documentAccountLineIds = getDocumentAccountLineIds(accountId);
        DateTimeFormatter df = DateTimeFormatter.ofPattern(DATE_PATTERN);
        Page<Object[]> values = historicDao.getHistoriqueLetteringIn(documentAccountLineIds, pageable);
        HistoricDtoPage documentAccountHistoricDtoPage = new HistoricDtoPage();
        documentAccountHistoricDtoPage.setTotalElements(values.getTotalElements());
        documentAccountHistoricDtoPage.setHistoricDtoList(values.getContent().stream()
                .map(historicItem -> new HistoricDto(LocalDate.parse(historicItem[0].toString(), df).atStartOfDay(),
                        historicItem[5].toString(), HistoricActionEnum.valueOf(historicItem[1].toString()),
                        historicItem[2].toString(), historicItem[3].toString(), historicItem[4].toString()))
                .collect(Collectors.toList()));
        List<DocumentAccountLineDto> listLine = DocumentAccountingLineConvertor
                .modelsToDtos(documentAccountLineDao.findAllByIdIn(documentAccountLineIds));
        documentAccountHistoricDtoPage.setHistoricDtoList(documentAccountHistoricDtoPage.getHistoricDtoList().stream()
                .peek(historic -> fillLetteringData(historic, listLine)).collect(Collectors.toList()));
        return documentAccountHistoricDtoPage;
    }

    public void fillLetteringData(HistoricDto historicDto, List<DocumentAccountLineDto> listLine) {
        getDocumentAccountFromEntityId(historicDto, listLine);
        historicDto.setEntity(DocumentAccountFieldsConstants.DOCUMENT_ACCOUNT_LINE);
        historicDto
                .setEntityField(StringUtils.isNotBlank(historicDto.getFieldNewValue()) ? "LETTERING" : "DELETTERING");
        historicDto.setFieldNewValue(
                StringUtils.isNotBlank(historicDto.getFieldNewValue()) ? historicDto.getFieldNewValue() : " - ");
    }

    private void getDocumentAccountFromEntityId(HistoricDto historicDto, List<DocumentAccountLineDto> listLine) {
        if (DocumentAccountLine.class.getSimpleName().equals(historicDto.getEntity())) {
            historicDto.setDocumentAccountLineAffected(
                    listLine.stream().filter(line -> Arrays.asList(historicDto.getEntityIds().trim().split(","))
                            .contains(line.getId().toString())).collect(Collectors.toList()));
        }
    }

    private List<Long> getDocumentAccountLineIds(Long accountId) {
        List<Long> documentAccountLineIds = documentAccountLineDao.getIdsByDocumentAccountId(accountId);
        if (documentAccountLineIds.isEmpty()) {
            documentAccountLineIds.add(0L);
        }
        return documentAccountLineIds;
    }

    @Override
    public HistoricDtoPage getReconciliationHistoric(Long accountId, Pageable pageable) {
        List<Long> documentAccountLineIds = getDocumentAccountLineIds(accountId);
        DateTimeFormatter df = DateTimeFormatter.ofPattern(DATE_PATTERN);
        Page<Object[]> values = historicDao.getHistoriqueReconciliationIn(documentAccountLineIds, pageable);
        HistoricDtoPage documentAccountHistoricDtoPage = new HistoricDtoPage();
        documentAccountHistoricDtoPage.setTotalElements(values.getTotalElements());
        documentAccountHistoricDtoPage.setHistoricDtoList(values.getContent().stream()
                .map(historicItem -> new HistoricDto(LocalDate.parse(historicItem[0].toString(), df).atStartOfDay(),
                        historicItem[5].toString(), HistoricActionEnum.valueOf(historicItem[1].toString()),
                        historicItem[2].toString(), historicItem[3].toString(), historicItem[4].toString()))
                .collect(Collectors.toList()));
        List<DocumentAccountLineDto> listLine = DocumentAccountingLineConvertor
                .modelsToDtos(documentAccountLineDao.findAllByIdIn(documentAccountLineIds));
        documentAccountHistoricDtoPage.setHistoricDtoList(documentAccountHistoricDtoPage.getHistoricDtoList().stream()
                .peek(historic -> fillReconciliationData(historic, listLine)).collect(Collectors.toList()));
        return documentAccountHistoricDtoPage;
    }

    public void fillReconciliationData(HistoricDto historicDto, List<DocumentAccountLineDto> listLine) {
        getDocumentAccountFromEntityId(historicDto, listLine);
        historicDto.setEntity(DocumentAccountFieldsConstants.DOCUMENT_ACCOUNT_LINE);
        historicDto.setEntityField(
                StringUtils.isNotBlank(historicDto.getFieldNewValue()) ? "RECONCILIATION" : "LIBERATION");
    }

    @Override
    public HistoricDtoPage getFiscalYearHistoric(Pageable pageable) {
        HistoricDtoPage fiscalYearHistoricDtoPage = HistoricConverter.pageModelToPageDto(
                historicDao.findByEntityAndIsDeletedFalse(FiscalYear.class.getSimpleName(), pageable));
        fiscalYearHistoricDtoPage.setHistoricDtoList(fiscalYearHistoricDtoPage.getHistoricDtoList().stream()
                .peek(this::checkHistoryFiscalYearField).collect(Collectors.toList()));
        return fiscalYearHistoricDtoPage;
    }

    public void checkHistoryFiscalYearField(HistoricDto historicDto) {
        FiscalYear fiscalYear = fiscalYearDao.findByIdEvenIfIsDeleted(historicDto.getEntityId()).orElseThrow(
                () -> new HttpCustomException(ApiErrors.Accounting.ACCOUNTING_CONFIGURATION_FISCAL_YEAR_NOT_FOUND));
        historicDto.setEntityName(fiscalYear.getName());
        switch (historicDto.getEntityField()) {
        case FiscalYearFieldsConstants.FISCAL_YEAR_NAME_FIELD:
            historicDto.setEntityField(FiscalYearFieldsConstants.FISCAL_YEAR_NAME_TITLE);
            break;
        case FiscalYearFieldsConstants.START_DATE_FIELD:
            historicDto.setEntityField(FiscalYearFieldsConstants.START_DATE_TITLE);
            break;
        case FiscalYearFieldsConstants.END_DATE_FIELD:
            historicDto.setEntityField(FiscalYearFieldsConstants.END_DATE_TITLE);
            break;
        case FiscalYearFieldsConstants.CLOSING_DATE_FIELD:
            historicDto.setEntityField(FiscalYearFieldsConstants.CLOSING_DATE_TITLE);
            break;
        case FiscalYearFieldsConstants.CONCLUSION_DATE_FIELD:
            historicDto.setEntityField(FiscalYearFieldsConstants.CONCLUSION_DATE_TITLE);
            break;
        case FiscalYearFieldsConstants.CLOSING_STATE_FIELD:
            historicDto.setEntityField(FiscalYearFieldsConstants.CLOSING_STATE_TITLE);
            if (historicDto.getFieldOldValue() != null) {
                historicDto.setFieldOldValue(
                        FiscalYearClosingState.fromValue(Integer.parseInt(historicDto.getFieldOldValue())).toString());
            }
            if (historicDto.getFieldNewValue() != null) {
                historicDto.setFieldNewValue(
                        FiscalYearClosingState.fromValue(Integer.parseInt(historicDto.getFieldNewValue())).toString());
            }
            break;
        case FiscalYearFieldsConstants.IS_DELETED_STATE_FIELD:
            historicDto.setEntityField(FiscalYearFieldsConstants.IS_DELETED_STATE_TITLE);
            break;
        default:
            historicDto.setEntityField(historicDto.getEntityField());
            break;
        }
    }

}
