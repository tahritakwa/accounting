package fr.sparkit.accounting.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import fr.sparkit.accounting.entities.Historic;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IHistoricDao extends BaseRepository<Historic, Long> {
    @Query("SELECT  h FROM  Historic h WHERE h.entity=?1 and h.entityId = ?2 and h.createdDate >=?3 and  h.isDeleted ="
            + " false")
    Page<Historic> searchByStartDate(String entityName, Long entityId, LocalDateTime startDate, Pageable pageable);

    @Query("SELECT  h FROM  Historic h WHERE h.entity=?1 and h.entityId = ?2 and h.createdDate <=?3 and  h.isDeleted ="
            + " false ")
    Page<Historic> searchByEndDate(String entityName, Long entityId, LocalDateTime endDate, Pageable pageable);

    @Query("SELECT  h FROM  Historic h WHERE h.entity=?1 and h.entityId = ?2 and h.createdDate >=?3 "
            + " and h.createdDate<=?4 and  h.isDeleted=false")
    Page<Historic> searchByStartDateAndEndDate(String entityName, Long entityId, LocalDateTime startDate,
            LocalDateTime endDate, Pageable pageable);

    @Query("SELECT  h FROM  Historic h WHERE h.entity=?1 and h.entityId = ?2 and (lower (h.entityField) like"
            + " %?3% or lower (h.action) like concat('%', ?3,'%') ) and "
            + "  h.createdDate >=?4 and h.createdDate <=?5 and  h.isDeleted = false ")
    Page<Historic> searchByStartDateAndEndDateAndSearchValue(String entityName, Long entityId, String searchValue,
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    @Query("SELECT  h FROM  Historic h WHERE h.entity=?1 and h.entityId = ?2 and (lower (h.entityField) like"
            + " %?3% or lower (h.action) like concat('%', ?3,'%') ) and "
            + "  h.createdDate >=?4 and h.isDeleted = false ")
    Page<Historic> searchByStartDateAndSearchValue(String entityName, Long entityId, String searchValue,
            LocalDateTime startDate, Pageable pageable);

    @Query("SELECT  h FROM  Historic h WHERE h.entity=?1 and h.entityId = ?2 and (lower (h.entityField) like"
            + " %?3% or lower (h.action) like concat('%', ?3,'%') ) and "
            + "  h.createdDate <=?4 and h.isDeleted = false")
    Page<Historic> searchByEndDateAndSearchValue(String entityName, Long entityId, String searchValue,
            LocalDateTime startDate, Pageable pageable);

    @Query("SELECT  h FROM  Historic h WHERE h.entity=?1 and h.entityId = ?2 and (lower(h.entityField)"
            + " like %?3% or lower (h.action) like  %?3%) and h.isDeleted = false ")
    Page<Historic> searchBySearchValue(String entityName, Long entityId, String searchValue, Pageable pageable);

    /* search historic without filtre */
    Page<Historic> findByEntityAndEntityIdAndIsDeletedFalse(String entityName, Long entityId,
            Pageable pageable);

    @Query("SELECT  h FROM  Historic h WHERE ((h.entity =?1 and h.entityId = ?3)  or (h.entity like ?2"
            + " and h.entityId in (select docL.id from DocumentAccountLine docL where docL.documentAccount.id =?3 ))) and "
            + "h.isDeleted = false")
    Page<Historic> findHistoricDocumentAccountAndIsDeletedFalse(String documentAccountEntityName,
            String documentAccountLineEntityName, Long id, Pageable pageable);

    @Query(value = " SELECT  CAST(historic.CREATED_DATE AS DATE) as createdDate, historic.HIS_ACTION as action, historic.HIS_ENTITY as entity, historic.CREATED_BY as  createdBy "
            + " ,entityIds = STUFF((SELECT distinct ',' + CAST(historicCollection.HIS_ENTITY_ID AS VARCHAR(255)) "
            + "  FROM T_HISTORIC historicCollection "
            + "  WHERE CAST(historic.CREATED_DATE AS DATE)  = CAST(historicCollection.CREATED_DATE AS DATE)  "
            + "  and historic.CREATED_BY = historicCollection.CREATED_BY and historic.HIS_ACTION = historicCollection.HIS_ACTION "
            + "  and historic.HIS_ENTITY = historicCollection.HIS_ENTITY "
            + "  and COALESCE(historic.HIS_ENTITY_FIELD_NEW_VALUE,'') =  COALESCE(historicCollection.HIS_ENTITY_FIELD_NEW_VALUE,'') "
            + "  FOR XML PATH(''), TYPE).value('.', 'VARCHAR(MAX)'), 1, 1, '') , "
            + "  COALESCE(historic.HIS_ENTITY_FIELD_NEW_VALUE,'') as fieldNewValue "
            + "  FROM  T_HISTORIC historic WHERE historic.HIS_ENTITY like 'DocumentAccountLine' and historic.HIS_ENTITY_ID in ?1 and historic.HIS_ENTITY_FIELD like 'letter' "
            + "  and historic.HIS_IS_DELETED = 0 GROUP BY  CAST(historic.CREATED_DATE AS DATE) , HIS_ACTION, CREATED_BY, HIS_ENTITY,  COALESCE(historic.HIS_ENTITY_FIELD_NEW_VALUE,'') ", nativeQuery = true)
    Page<Object[]> getHistoriqueLetteringIn(List<Long> ids, Pageable pageable);

    @Query(value = " SELECT  CAST(historic.CREATED_DATE AS DATE) as createdDate, historic.HIS_ACTION as action, historic.HIS_ENTITY as entity, historic.CREATED_BY as  createdBy "
            + " ,entityIds = STUFF((SELECT distinct ',' + CAST(historicCollection.HIS_ENTITY_ID AS VARCHAR(255)) "
            + "  FROM T_HISTORIC historicCollection "
            + "  WHERE CAST(historic.CREATED_DATE AS DATE)  = CAST(historicCollection.CREATED_DATE AS DATE)  "
            + "  and historic.CREATED_BY = historicCollection.CREATED_BY and historic.HIS_ACTION = historicCollection.HIS_ACTION "
            + "  and historic.HIS_ENTITY = historicCollection.HIS_ENTITY "
            + "  FOR XML PATH(''), TYPE).value('.', 'VARCHAR(MAX)'), 1, 1, '') "
            + " ,idsList = STUFF((SELECT distinct ',' + CAST(historicCollection.HIS_ID AS VARCHAR(255)) "
            + "  FROM T_HISTORIC historicCollection "
            + "  WHERE CAST(historic.CREATED_DATE AS DATE)  = CAST(historicCollection.CREATED_DATE AS DATE)  "
            + "  and historic.CREATED_BY = historicCollection.CREATED_BY and historic.HIS_ACTION = historicCollection.HIS_ACTION "
            + "  and historic.HIS_ENTITY = historicCollection.HIS_ENTITY and ((historicCollection.HIS_ENTITY like 'DocumentAccountLine' and historicCollection.HIS_ENTITY_ID in ?1) or"
            + "  (historicCollection.HIS_ENTITY like 'DocumentAccount' and historicCollection.HIS_ENTITY_ID= ?2))"
            + "  FOR XML PATH(''), TYPE).value('.', 'VARCHAR(MAX)'), 1, 1, '') "
            + "  FROM  T_HISTORIC historic WHERE ((historic.HIS_ENTITY like 'DocumentAccountLine' and historic.HIS_ENTITY_ID in ?1) "
            + "  OR  (historic.HIS_ENTITY like 'DocumentAccount' and historic.HIS_ENTITY_ID= ?2) )"
            + "  and historic.HIS_IS_DELETED = 0 GROUP BY  CAST(historic.CREATED_DATE AS DATE) , HIS_ACTION, CREATED_BY, HIS_ENTITY", nativeQuery = true)
    Page<Object[]> getHistoriqueDocumentAccount(List<Long> ids, Long id, Pageable pageable);

    @Query("SELECT distinct h.entityId FROM  Historic h WHERE h.entity=?1 and h.isDeleted = false ")
    List<Long> getEntityIdsList(String entityName);

    @Query(value = " SELECT  CAST(historic.CREATED_DATE AS DATE) as createdDate, historic.HIS_ACTION as action, historic.HIS_ENTITY as entity, historic.CREATED_BY as  createdBy "
            + " , STUFF((SELECT distinct ',' + CAST(historicCollection.HIS_ENTITY_ID AS VARCHAR(255)) "
            + "  FROM T_HISTORIC historicCollection "
            + "  WHERE CAST(historic.CREATED_DATE AS DATE)  = CAST(historicCollection.CREATED_DATE AS DATE)  "
            + "  and historic.CREATED_BY = historicCollection.CREATED_BY and historic.HIS_ACTION = historicCollection.HIS_ACTION "
            + "  and historic.HIS_ENTITY = historicCollection.HIS_ENTITY "
            + "  and COALESCE(historic.HIS_ENTITY_FIELD_NEW_VALUE,'') =  COALESCE(historicCollection.HIS_ENTITY_FIELD_NEW_VALUE,'') "
            + "  FOR XML PATH(''), TYPE).value('.', 'VARCHAR(MAX)'), 1, 1, '') as entityIds, "
            + "  COALESCE(historic.HIS_ENTITY_FIELD_NEW_VALUE,'') as fieldNewValue "
            + "  FROM  T_HISTORIC historic WHERE historic.HIS_ENTITY like 'DocumentAccountLine' and historic.HIS_ENTITY_ID in ?1 and historic.HIS_ENTITY_FIELD like 'reconciliationDate'  "
            + "  and historic.HIS_IS_DELETED = 0 GROUP BY  CAST(historic.CREATED_DATE AS DATE) , HIS_ACTION, CREATED_BY, HIS_ENTITY,  COALESCE(historic.HIS_ENTITY_FIELD_NEW_VALUE,'')",
            countQuery = " SELECT  count(*)"
                    +"  FROM  T_HISTORIC historic WHERE historic.HIS_ENTITY like 'DocumentAccountLine' and historic.HIS_ENTITY_ID in ?1 and historic.HIS_ENTITY_FIELD like 'reconciliationDate'"
                    +"  and historic.HIS_IS_DELETED = 0 GROUP BY  CAST(historic.CREATED_DATE AS DATE) , HIS_ACTION, CREATED_BY, HIS_ENTITY,  COALESCE(historic.HIS_ENTITY_FIELD_NEW_VALUE,'')",
            nativeQuery = true)
    Page<Object[]> getHistoriqueReconciliationIn(List<Long> ids, Pageable pageable);

    Page<Historic> findByEntityAndIsDeletedFalse(String entityName, Pageable pageable);

}
