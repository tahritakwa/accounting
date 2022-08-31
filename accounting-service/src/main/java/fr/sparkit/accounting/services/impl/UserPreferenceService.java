package fr.sparkit.accounting.services.impl;

import static fr.sparkit.accounting.constants.AccountingConstants.LOG_ENTITY_CREATED;
import static fr.sparkit.accounting.constants.AccountingConstants.LOG_ENTITY_DELETED;
import static fr.sparkit.accounting.constants.AccountingConstants.LOG_ENTITY_UPDATED;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.sparkit.accounting.convertor.FiscalYearConvertor;
import fr.sparkit.accounting.convertor.UserPreferenceConvertor;
import fr.sparkit.accounting.dao.UserPreferenceDao;
import fr.sparkit.accounting.dto.FiscalYearDto;
import fr.sparkit.accounting.dto.UserPreferenceDto;
import fr.sparkit.accounting.entities.Account;
import fr.sparkit.accounting.entities.FiscalYear;
import fr.sparkit.accounting.entities.Journal;
import fr.sparkit.accounting.entities.UserPreference;
import fr.sparkit.accounting.services.IFiscalYearService;
import fr.sparkit.accounting.services.IJournalService;
import fr.sparkit.accounting.services.IUserPreferenceService;
import fr.sparkit.accounting.util.UserContextHolder;
import fr.sparkit.accounting.util.errors.ApiErrors;
import fr.sparkit.accounting.util.http.HttpCustomException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserPreferenceService extends GenericService<UserPreference, Long> implements IUserPreferenceService {
    private final IFiscalYearService fiscalYearService;
    private final UserPreferenceDao userPreferenceDao;
    private final IJournalService journalService;

    @Autowired
    public UserPreferenceService(IFiscalYearService fiscalYearService, UserPreferenceDao userPreferenceDao,
            IJournalService journalService) {
        this.fiscalYearService = fiscalYearService;
        this.userPreferenceDao = userPreferenceDao;
        this.journalService = journalService;
    }

    @Override
    public UserPreference findUserPreference() {
        String currentUserEmail = UserContextHolder.getUserContext();
        Optional<UserPreference> userPreferenceOpt = userPreferenceDao
                .findByUserEmailAndIsDeletedFalse(currentUserEmail);
        if (!userPreferenceOpt.isPresent()) {
            UserPreference userPreference = new UserPreference(0L, currentUserEmail, null, null, false, null);
            return saveAndFlush(userPreference);
        }
        return userPreferenceOpt.get();
    }

    @Override
    public FiscalYearDto getCurrentFiscalYear() {
        UserPreference userPreference = findUserPreference();
        FiscalYear currentFiscalYear = userPreference.getCurrentFiscalYear();
        if (currentFiscalYear == null) {
            currentFiscalYear = fiscalYearService.findLastFiscalYearByEndDate();
            if (currentFiscalYear != null) {
                userPreference.setCurrentFiscalYear(currentFiscalYear);
                saveAndFlush(userPreference);
            } else {
                throw new HttpCustomException(ApiErrors.Accounting.NO_FISCAL_YEARS_FOUND);
            }
        }
        return FiscalYearConvertor.modelToDto(currentFiscalYear);
    }

    @Override
    public FiscalYearDto saveCurrentFiscalYear(Long fiscalYearId) {
        FiscalYearDto fiscalYear = fiscalYearService.findById(fiscalYearId);
        UserPreference userPreference = findUserPreference();
        userPreference.setCurrentFiscalYear(FiscalYearConvertor.dtoToModel(fiscalYear));
        saveAndFlush(userPreference);
        return fiscalYear;
    }

    @Override
    public UserPreference saveOrUpdateByUser(UserPreferenceDto userPreferenceDto) {
        Journal journal = Optional.ofNullable(journalService.findOne(userPreferenceDto.getJournalId()))
                .orElseThrow(() -> new HttpCustomException(ApiErrors.Accounting.JOURNAL_NOT_FOUND));
        FiscalYear fiscalYear = Optional
                .ofNullable(fiscalYearService.findOne(userPreferenceDto.getCurrentFiscalYearId()))
                .orElseThrow(() -> new HttpCustomException(ApiErrors.Accounting.JOURNAL_NOT_FOUND));
        if (userPreferenceDto.getId() == 0) {
            if (checkIfUserHasAJournal(userPreferenceDto.getUserEmail())) {
                throw new HttpCustomException(ApiErrors.Accounting.USER_HAS_A_JOURNAL);
            }
            UserPreference userPreferenceToSave = UserPreferenceConvertor.dtoToModel(userPreferenceDto, journal,
                    fiscalYear);
            saveAndFlush(userPreferenceToSave);
            log.info(LOG_ENTITY_CREATED, userPreferenceToSave);
            return userPreferenceToSave;
        } else {
            UserPreference userJournalToUpdate = findUserPreference();
            userJournalToUpdate.setJournal(journal);
            saveAndFlush(userJournalToUpdate);
            log.info(LOG_ENTITY_UPDATED, userJournalToUpdate);
            return userJournalToUpdate;
        }
    }

    @Override
    public boolean deleteByUser(Long id) {
        log.info(LOG_ENTITY_DELETED, "UserJournal", id);
        UserPreference userToDelete = findUserPreference();
        return isDynamicSoftDelete(id, Account.class.getName(), String.valueOf(userToDelete.getId()),
                "MESSAGE_USER_TO_DELETE");
    }

    private boolean checkIfUserHasAJournal(String userEmail) {
        return userPreferenceDao.findByUserEmailAndIsDeletedFalse(userEmail).isPresent();
    }
}
