package fr.sparkit.accounting.dao;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import fr.sparkit.accounting.dto.ChartAccountsDto;
import fr.sparkit.accounting.entities.ChartAccounts;

public interface ChartAccountsDao extends BaseRepository<ChartAccounts, Long> {

    List<ChartAccounts> findByAccountParentIdAndIsDeletedFalse(Long parentId);

    Optional<ChartAccounts> findByCodeAndIsDeletedFalse(int code);

    List<ChartAccounts> findByLabelContainsIgnoreCaseAndIsDeletedFalseOrCodeLikeAndIsDeletedFalse(String label,
            int code);

    boolean existsByCodeAndIsDeletedFalse(int code);

    @Query("SELECT new fr.sparkit.accounting.dto.ChartAccountsDto(a.code,a.label) FROM ChartAccounts a "
            + "where a.isDeleted=false and a.code LIKE CONCAT(:code,'%')")
    List<ChartAccountsDto> findByCodes(@Param("code") String code);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE T_CHART_ACCOUNTS SET CA_TO_BALANCED = 1 where CA_ID in ?1", nativeQuery = true)
    void updateChartAccountsToBalanced(List<Long> chartAccounts);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE T_CHART_ACCOUNTS SET CA_TO_BALANCED = 0", nativeQuery = true)
    void updateAllChartAccountsToNotBalanced();

    @Query(value = "SELECT CA_ID FROM T_CHART_ACCOUNTS where "
            + "CA_IS_DELETED = 0 and CA_TO_BALANCED = 1", nativeQuery = true)
    List<BigInteger> findChartAccountsToBalanced();

    @Query("SELECT a.code FROM ChartAccounts a " + "where a.isDeleted=false and a.toBalanced=true")
    List<Integer> findChartAccountsCodeToBalanced();

    @Query(value = "SELECT * FROM T_CHART_ACCOUNTS"
            + " where CA_IS_DELETED=0 and LEN(CONVERT(varchar(150),CA_CODE)) LIKE :length"
            + " and CONVERT(varchar(150),CA_CODE) NOT LIKE CONCAT(CONVERT(varchar(150), :customerCode), '%')"
            + " and CONVERT(varchar(150),CA_CODE) NOT LIKE CONCAT(CONVERT(varchar(150), :supplierCode), '%')"
            + " ORDER BY CA_CODE", nativeQuery = true)
    List<ChartAccounts> findChartAccountByLength(@Param("length") int length, @Param("customerCode") int customerCode,
            @Param("supplierCode") int supplierCode);

    @Query(value = "SELECT * FROM T_CHART_ACCOUNTS"
            + " where CA_IS_DELETED=0 and LEN(CONVERT(varchar(150),CA_CODE)) LIKE :length"
            + " and CONVERT(varchar(150),CA_CODE) LIKE CONCAT(CONVERT(varchar(150), :tierCode), '%')"
            + " ORDER BY CA_CODE", nativeQuery = true)
    List<ChartAccounts> findChartAccountTierByLength(@Param("length") int length, @Param("tierCode") int tierCode);

    List<ChartAccounts> findByIsDeletedFalse();

}
