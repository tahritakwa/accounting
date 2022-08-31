package fr.sparkit.accounting.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import fr.sparkit.accounting.entities.FiscalYear;

@Repository
public interface FiscalYearDao extends BaseRepository<FiscalYear, Long> {

    @Query(value = "SELECT TOP 1 * FROM T_FISCAL_YEAR fy WHERE fy.FY_START_DATE<=CAST(?1 as DATE) "
            + "AND fy.FY_END_DATE>=CAST(?1 as DATE) AND fy.FY_IS_DELETED=0", nativeQuery = true)
    Optional<FiscalYear> findFiscalYearOfDate(LocalDateTime date);

    FiscalYear findByNameAndIsDeletedFalse(String name);

    List<FiscalYear> findAllByIsDeletedFalseOrderByStartDateDesc();

    @Query(value = "SELECT TOP 1 * FROM T_FISCAL_YEAR previousfy"
            + " WHERE previousfy.FY_END_DATE<(SELECT FY.FY_START_DATE FROM T_FISCAL_YEAR FY WHERE FY.FY_ID = ?1) "
            + "AND FY_IS_DELETED = 0 ORDER BY FY_END_DATE DESC", nativeQuery = true)
    Optional<FiscalYear> findPreviousFiscalYear(Long fiscalYearId);

    @Query("SELECT MAX(endDate) FROM FiscalYear WHERE  isDeleted = 0")
    Optional<LocalDateTime> startDateFiscalYear();

    List<FiscalYear> findAllByClosingStateInAndIsDeletedFalse(List<Integer> closingStatus);

    List<FiscalYear> findAllByClosingStateNotOrderByStartDate(int closingState);

    @Query(value = "SELECT * FROM T_FISCAL_YEAR where T_FISCAL_YEAR.FY_START_DATE > ?1 AND FY_IS_DELETED = 0", nativeQuery = true)
    List<FiscalYear> findAllFiscalYearsAfterDate(LocalDateTime startDate);

    @Query(value = "SELECT * FROM T_FISCAL_YEAR where T_FISCAL_YEAR.FY_END_DATE < ?1  AND FY_IS_DELETED = 0", nativeQuery = true)
    List<FiscalYear> findAllFiscalYearsBeforeCurrentFiscalYear(LocalDateTime startDate);

    FiscalYear findTopByIsDeletedFalseOrderByEndDateDesc();

    List<FiscalYear> findByClosingStateAndIsDeletedFalse(int closingState);

    boolean existsByIdAndIsDeletedFalse(Long fiscalYearId);

    @Query("SELECT Min(startDate) FROM FiscalYear WHERE  isDeleted = 0")
    LocalDateTime getStartDateOfFirstFiscalYear();

    @Query(value = "SELECT TOP 1 * FROM T_FISCAL_YEAR f"
            + " WHERE f.FY_START_DATE>(SELECT FY.FY_END_DATE FROM T_FISCAL_YEAR FY WHERE FY.FY_ID = ?1) "
            + "AND FY_IS_DELETED = 0 ORDER BY FY_START_DATE", nativeQuery = true)
    Optional<FiscalYear> findNextFiscalYear(Long fiscalYearId);

}
