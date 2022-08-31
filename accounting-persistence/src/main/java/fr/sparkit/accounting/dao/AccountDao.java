package fr.sparkit.accounting.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.sparkit.accounting.dto.AccountDto;
import fr.sparkit.accounting.entities.Account;

@Repository
public interface AccountDao extends BaseRepository<Account, Long> {

    Optional<Account> findByCodeAndIsDeletedFalse(int code);

    List<Account> findAllByIsDeletedFalseOrderByCode();

    List<Account> findAllByIsDeletedFalseAndReconcilableTrueOrderByCode();

    boolean existsByCodeAndPlanIdAndIsDeletedFalse(int code, Long id);

    boolean existsByCodeAndIsDeletedFalse(int code);

    @Query("SELECT account FROM Account account where isDeleted=false and code = ?1 order by code")
    List<Account> findAccountsByCode(int code);

    boolean existsByIdAndIsDeletedFalse(Long id);

    @Query("SELECT a FROM Account a "
            + "where (a.label LIKE %?1% or a.code LIKE concat(?2,'%') or a.plan.code LIKE concat(?3,'%') ) and a.isDeleted=false")
    Page<Account> findByLabelContainsIgnoreCaseOrCodeLikeOrPlanCodeLikeAndIsDeletedFalse(String label, String code,
            String planCode, Pageable pageble);

    @Query("SELECT a.code FROM Account a where a.plan.id LIKE ?1 and a.isDeleted=false ORDER BY a.code")
    List<Integer> findCodeAccountByPlanIdAndIsDeletedFalseOrderByCode(Long planId);

    @Query("SELECT a FROM Account a WHERE a.isDeleted=false AND a.plan.code LIKE CONCAT(:chartAccountCode,'%') ORDER BY a.code")
    List<Account> findByPlanCode(@Param("chartAccountCode") String chartAccountCode);

    @Query("SELECT NEW fr.sparkit.accounting.dto.AccountDto(id, code, label) FROM Account "
            + "where isDeleted=false and code in ?1 order by code")
    List<AccountDto> findAllByCodes(List<Long> codes);

    @Query("SELECT MIN(a.code) FROM Account a where a.isDeleted=false and a.code LIKE CONCAT( ?1,'%')")
    Optional<Integer> findMinCode(String code);

    @Query("SELECT MAX(a.code) FROM Account a where a.isDeleted=false and a.code LIKE CONCAT( ?1,'%')")
    Optional<Integer> findMaxCode(String code);

    @Query("SELECT a FROM Account a WHERE a.isDeleted=false AND a.code=:accountCode AND CAST(a.plan.code as text) = :chartAccountCode")
    Account findByCodeAndPlanCode(@Param("accountCode") int accountCode,
            @Param("chartAccountCode") String chartAccountCode);

}
