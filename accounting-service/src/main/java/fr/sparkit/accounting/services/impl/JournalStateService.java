package fr.sparkit.accounting.services.impl;

import static fr.sparkit.accounting.constants.AccountingConstants.*;
import static fr.sparkit.accounting.services.utils.AccountingServiceUtil.isDateBeforeOrEquals;
import static fr.sparkit.accounting.util.CalculationUtil.getFormattedBigDecimalValue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import fr.sparkit.accounting.constants.AccountingConstants;
import fr.sparkit.accounting.dao.DocumentAccountLineDao;
import fr.sparkit.accounting.dto.DepreciationAssetsDto;
import fr.sparkit.accounting.dto.JournalStateDetailsDto;
import fr.sparkit.accounting.dto.JournalStateDto;
import fr.sparkit.accounting.dto.JournalStateReportLineDto;
import fr.sparkit.accounting.entities.DocumentAccount;
import fr.sparkit.accounting.entities.Journal;
import fr.sparkit.accounting.services.IDocumentAccountService;
import fr.sparkit.accounting.services.IJournalStateService;
import fr.sparkit.accounting.services.utils.AccountingServiceUtil;
import fr.sparkit.accounting.services.utils.DepreciationAssetsUtil;
import fr.sparkit.accounting.services.utils.TraductionServiceUtil;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JournalStateService implements IJournalStateService {

    private final DocumentAccountLineDao documentAccountLineDao;
    private final IDocumentAccountService documentAccountService;

    @Autowired
    public JournalStateService(IDocumentAccountService documentAccountService,
            DocumentAccountLineDao documentAccountLineDao) {
        this.documentAccountService = documentAccountService;
        this.documentAccountLineDao = documentAccountLineDao;
    }

    @Override
    public Page<JournalStateDto> findJournalsState(int page, int size, LocalDateTime startDate, LocalDateTime endDate) {
        if (isDateBeforeOrEquals(startDate, endDate)) {
            Pageable pageable = PageRequest.of(page, size);
            List<JournalStateDto> journalsStates = getJournalsStateDto(startDate, endDate);

            BigDecimal totalAmount = calculateTotalAmountForJournalState(journalsStates);

            int beginIndex = Math.toIntExact(pageable.getOffset());
            int endIndex = beginIndex + Math.toIntExact(pageable.getPageSize());
            if (beginIndex + pageable.getPageSize() > journalsStates.size()) {
                endIndex = journalsStates.size();
            }
            List<JournalStateDto> journalsStatePages = new ArrayList<>(journalsStates.subList(beginIndex, endIndex));
            journalsStatePages.add(new JournalStateDto(
                    totalAmount.setScale(AccountingConstants.DEFAULT_SCALE_FOR_BIG_DECIMAL, RoundingMode.HALF_UP)));
            return new PageImpl<>(journalsStatePages, pageable, journalsStates.size());
        } else {
            log.error(DATES_ORDER_INVALID);
            throw new HttpCustomException(ApiErrors.Accounting.START_DATE_IS_AFTER_END_DATE);
        }
    }

    public BigDecimal calculateTotalAmountForJournalState(Collection<JournalStateDto> journalsStateDto) {
        return journalsStateDto.stream().map(JournalStateDto::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<JournalStateDto> getJournalsStateDto(LocalDateTime startDate, LocalDateTime endDate) {
        List<JournalStateDto> journalsState = new ArrayList<>();
        List<DocumentAccount> documentAccounting = documentAccountService.findAll();
        documentAccounting.stream().map(DocumentAccount::getJournal).distinct()
                .sorted(Comparator.comparing(Journal::getCode))
                .forEach((Journal journal) -> addToJournalsState(journal, journalsState, startDate, endDate));
        return journalsState;
    }

    private void addToJournalsState(Journal journal, Collection<JournalStateDto> journalsState, LocalDateTime startDate,
            LocalDateTime endDate) {

        BigDecimal totalAmount = documentAccountLineDao
                .totalAmountForJournalInBetween(journal.getId(), startDate, endDate).orElse(BigDecimal.ZERO);

        journalsState.add(new JournalStateDto(journal.getId(), journal.getCode(), journal.getLabel(),
                totalAmount.setScale(AccountingConstants.DEFAULT_SCALE_FOR_BIG_DECIMAL, RoundingMode.HALF_UP)));
    }

    @Override
    public Page<JournalStateDetailsDto> findJournalStateDetails(Long journalId,
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        List<JournalStateDetailsDto> journalStateDetailsDtos = new ArrayList<>();
        List<DocumentAccount> documentsInJournal = documentAccountService.findByJournal(journalId, startDate, endDate);

        documentsInJournal.forEach((DocumentAccount documentAccount) -> addToJournalStateJournalDetails(documentAccount,
                journalStateDetailsDtos, startDate, endDate));

        sortStateJournals(journalStateDetailsDtos, pageable);
        int beginIndex = Math.toIntExact(pageable.getOffset());
        int endIndex = beginIndex + Math.toIntExact(pageable.getPageSize());

        if (beginIndex + pageable.getPageSize() > journalStateDetailsDtos.size()) {
            endIndex = journalStateDetailsDtos.size();
        }


        List<JournalStateDetailsDto> journalStateDetailsPage = journalStateDetailsDtos.subList(beginIndex, endIndex);
        return new PageImpl<>(journalStateDetailsPage, pageable, journalStateDetailsDtos.size());
    }

    private Comparator<JournalStateDetailsDto> getDefaultStateJournalSort() {
        return Comparator.comparing((JournalStateDetailsDto journalStateDetailsDto)->journalStateDetailsDto.getDocumentAccountDate().toLocalDate(), Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(JournalStateDetailsDto::getDocumentAccountCode, Comparator.nullsFirst(Comparator.naturalOrder()));
    }
    void sortStateJournals(List<JournalStateDetailsDto> journalStateDetailsDtos, Pageable pageable){
        Comparator<JournalStateDetailsDto> defaultSort = getDefaultStateJournalSort();
        Comparator<JournalStateDetailsDto> sort;
        Optional<Sort.Order> sortOrder = pageable.getSort().get().findFirst();
        Sort.Direction direction;
        if (!sortOrder.isPresent()) {
            sort = defaultSort;
            direction = Sort.Direction.ASC;
        } else {
            direction = sortOrder.get().getDirection();
            switch (sortOrder.get().getProperty()){
            case "documentAccountCode":
                sort = Comparator.comparing(JournalStateDetailsDto::getDocumentAccountCode, Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            case "documentAccountDate":
                sort = defaultSort;
                break;
            case "documentAccountReference":
                sort = Comparator.comparing(JournalStateDetailsDto::getDocumentAccountReference, Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            case "totalAmount":
                sort = Comparator.comparing(JournalStateDetailsDto::getTotalAmount, Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            default:
                sort = defaultSort;
            }
        }
        journalStateDetailsDtos.sort(sort);
        if(Sort.Direction.DESC.equals(direction)){
            Collections.reverse(journalStateDetailsDtos);
        }
    }
    @Override
    public void addToJournalStateJournalDetails(DocumentAccount documentAccount,
            List<JournalStateDetailsDto> journalStateDetailsDtos, LocalDateTime startDate, LocalDateTime endDate) {
        journalStateDetailsDtos.add(new JournalStateDetailsDto(documentAccount.getCodeDocument(),
                documentAccount.getDocumentDate(), documentAccount.getLabel(), documentAccountLineDao
                .totalDebitAmountDocumentAccount(documentAccount.getId()).orElse(BigDecimal.ZERO)));
    }

    @Override
    public void testDotnetUrl(String contentType, String user, String authorization,
            String dotnetRessource) {
        RestTemplate restTemplate = new RestTemplate();
        String fooResourceUrl = dotnetRessource.concat(AccountingConstants.GET_IMOBILIZATION_URL_DOTNET);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Module", "Immobilisation");
        headers.set("TModel", "Active");
        headers.set("User", user);
        headers.set("Content-Type", contentType);
        headers.set("Authorization", authorization);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        log.info("Dotnet ressource : {}" + dotnetRessource);
        List<DepreciationAssetsDto> depreciationAssetsDtos = new ArrayList<>();
        try {

            List<LinkedHashMap<String, Object>> result = restTemplate
                    .exchange(fooResourceUrl, HttpMethod.GET, entity, List.class).getBody();
            if (result != null) {
                for (LinkedHashMap<String, Object> objectLinkedHashMap : result) {
                    DepreciationAssetsDto depreciationAssetsDto = new DepreciationAssetsDto();
                    objectLinkedHashMap.entrySet().forEach(stringObjectEntry -> DepreciationAssetsUtil
                            .setImobilizationField(depreciationAssetsDto, stringObjectEntry));
                    depreciationAssetsDtos.add(depreciationAssetsDto);
                    log.info("Assets return from dotnet ressource : {}", depreciationAssetsDto.toString());
                }
            }
        } catch (RestClientException e) {
            log.error(CANNOT_CONNECT_TO_DOT_NET, e);
            throw new HttpCustomException(ApiErrors.Accounting.RESOURCE_NOT_FOUND);
        }
    }

    @Override
    public List<JournalStateReportLineDto> findJournalsStateReport(LocalDateTime startDate, LocalDateTime endDate) {
        List<JournalStateReportLineDto> journalStateReportLineDtos = new ArrayList<>();

        List<JournalStateDto> journalsStates = getJournalsStateDto(startDate, endDate);
        for (JournalStateDto journalStateDto : journalsStates) {
            insertJournalNameAndCodeIntoLineReport(journalStateDto, journalStateReportLineDtos);
            addStateJournalDetailsToReportLineDtos(journalStateReportLineDtos, journalStateDto, startDate, endDate);
            insertSubTotalAmountLineForStateJournal(journalStateReportLineDtos, journalStateDto);
        }
        insertTotalAmountLineForStateJournal(journalStateReportLineDtos, journalsStates);
        return journalStateReportLineDtos;
    }

    public void insertJournalNameAndCodeIntoLineReport(JournalStateDto journalStateDto,
            Collection<JournalStateReportLineDto> journalStateReportLineDtos) {
        journalStateReportLineDtos.add(new JournalStateReportLineDto(AccountingServiceUtil
                .getConcatinationCodeWithLabel(journalStateDto.getJournalCode(), journalStateDto.getJournalLabel())));
    }

    public void addStateJournalDetailsToReportLineDtos(List<JournalStateReportLineDto> journalStateReportLineDtos,
            JournalStateDto journalStateDto, LocalDateTime startDate, LocalDateTime endDate) {
        List<JournalStateDetailsDto> journalStateDetailsDtos = new ArrayList<>();
        List<DocumentAccount> documentsInJournal = documentAccountService.findByJournal(journalStateDto.getJournalId(),
                startDate, endDate);
        documentsInJournal.forEach((DocumentAccount documentAccount) -> addToJournalStateJournalDetails(documentAccount,
                journalStateDetailsDtos, startDate, endDate));
        journalStateDetailsDtos.sort(Comparator.comparing(JournalStateDetailsDto::getDocumentAccountDate).thenComparing(JournalStateDetailsDto::getDocumentAccountCode));
        insertStateJournalDetailsToReportLineDtos(journalStateReportLineDtos, journalStateDetailsDtos);
    }

    public void insertStateJournalDetailsToReportLineDtos(
            Collection<JournalStateReportLineDto> journalStateReportLineDtos,
            Iterable<JournalStateDetailsDto> journalStateDetailsDtos) {
        for (JournalStateDetailsDto journalStateDetailsDto : journalStateDetailsDtos) {
            journalStateReportLineDtos.add(
                    new JournalStateReportLineDto(EMPTY_STRING, journalStateDetailsDto.getDocumentAccountReference(),
                            journalStateDetailsDto.getDocumentAccountCode(),
                            journalStateDetailsDto.getDocumentAccountDate()
                                    .format(DateTimeFormatter.ofPattern(DD_MM_YYYY)),
                            getFormattedBigDecimalValue(journalStateDetailsDto.getTotalAmount())));
        }
    }

    public void insertSubTotalAmountLineForStateJournal(
            Collection<JournalStateReportLineDto> journalStateReportLineDtos, JournalStateDto journalStateDto) {
        journalStateReportLineDtos.add(
                new JournalStateReportLineDto(TraductionServiceUtil.getI18nResourceBundle().getString(SUB_TOTAL_PARAM),
                        getFormattedBigDecimalValue(journalStateDto.getTotalAmount())));

    }

    public void insertTotalAmountLineForStateJournal(Collection<JournalStateReportLineDto> journalStateReportLineDto,
            List<JournalStateDto> journalsStates) {
        BigDecimal totalAmount = calculateTotalAmountForJournalState(journalsStates);
        journalStateReportLineDto
                .add(new JournalStateReportLineDto(TraductionServiceUtil.getI18nResourceBundle().getString(TOTAL_PARAM),
                        getFormattedBigDecimalValue(totalAmount)));
    }
}
