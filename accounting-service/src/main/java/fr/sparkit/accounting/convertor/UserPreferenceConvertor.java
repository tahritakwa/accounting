package fr.sparkit.accounting.convertor;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import fr.sparkit.accounting.dto.UserPreferenceDto;
import fr.sparkit.accounting.entities.FiscalYear;
import fr.sparkit.accounting.entities.Journal;
import fr.sparkit.accounting.entities.UserPreference;

public final class UserPreferenceConvertor implements Serializable {

    private static final long serialVersionUID = 1L;

    private UserPreferenceConvertor() {
        super();
    }

    public static UserPreferenceDto modelToDto(UserPreference userPreference) {
        if (userPreference == null) {
            return null;
        }
        return new UserPreferenceDto(userPreference.getId(), userPreference.getUserEmail(),
                userPreference.getJournal() != null ? userPreference.getJournal().getId() : 0,
                userPreference.getCurrentFiscalYear().getId());
    }

    public static UserPreference dtoToModel(UserPreferenceDto userPreferenceDto, Journal journal,
            FiscalYear fiscalYear) {
        return new UserPreference(userPreferenceDto.getId(), userPreferenceDto.getUserEmail(), fiscalYear, journal,
                false, null);
    }

    public static List<UserPreferenceDto> modelsToDtos(Collection<UserPreference> accounts) {
        return accounts.stream().filter(Objects::nonNull).map(UserPreferenceConvertor::modelToDto)
                .collect(Collectors.toList());
    }

}
