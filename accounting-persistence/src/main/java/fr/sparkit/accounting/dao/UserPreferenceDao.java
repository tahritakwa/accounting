package fr.sparkit.accounting.dao;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import fr.sparkit.accounting.entities.UserPreference;

@Repository
public interface UserPreferenceDao extends BaseRepository<UserPreference, Long> {

    Optional<UserPreference> findByUserEmailAndIsDeletedFalse(String email);

}
