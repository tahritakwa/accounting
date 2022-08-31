package fr.sparkit.accounting.services;

import org.springframework.stereotype.Service;

import fr.sparkit.accounting.dto.FiscalYearDto;
import fr.sparkit.accounting.dto.UserPreferenceDto;
import fr.sparkit.accounting.entities.UserPreference;

@Service
public interface IUserPreferenceService extends IGenericService<UserPreference, Long> {

    UserPreference findUserPreference();

    FiscalYearDto getCurrentFiscalYear();

    FiscalYearDto saveCurrentFiscalYear(Long fiscalYearId);

    UserPreference saveOrUpdateByUser(UserPreferenceDto userPreferenceDto);

    boolean deleteByUser(Long jourUserEmail);
}
