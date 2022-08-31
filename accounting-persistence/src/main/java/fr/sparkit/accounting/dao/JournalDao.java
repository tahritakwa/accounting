package fr.sparkit.accounting.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.sparkit.accounting.entities.Journal;

@Repository
public interface JournalDao extends BaseRepository<Journal, Long> {

    boolean existsByCodeAndIsDeletedFalse(String code);

    boolean existsByLabelAndIsDeletedFalse(String label);

    Optional<Journal> findByIdAndIsDeletedFalse(Long id);

    Journal findByLabelAndIsDeletedFalseOrderByIdDesc(String label);

    Journal findByCodeAndIsDeletedFalseOrderByIdDesc(String code);

    @Query("FROM Journal j " + "WHERE "
            + "(j.code LIKE CONCAT('%',:value,'%') OR LOWER(j.label) LIKE LOWER(CONCAT('%',:value,'%')))"
            + " AND j.isDeleted=FALSE")
    Page<Journal> search(@Param("value") String value, Pageable pageable);

    @Query("FROM Journal j WHERE  j.id in :journalIds AND j.isDeleted=FALSE")
    List<Journal> findByIds(@Param("journalIds") List<Long> journalIds);

}
