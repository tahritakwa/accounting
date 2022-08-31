package fr.sparkit.accounting.auditing;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.logging.log4j.util.Strings;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.collection.internal.PersistentBag;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import fr.sparkit.accounting.constraint.validator.AuditedEntity;
import fr.sparkit.accounting.constraint.validator.NotAuditedField;
import fr.sparkit.accounting.dao.IHistoricDao;
import fr.sparkit.accounting.entities.DocumentAccountLine;
import fr.sparkit.accounting.entities.Historic;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Configuration
@Slf4j
public class HistoricListner {

    private final IHistoricDao historicDao;
    private final Environment environment;
    private HashMap<String, Object> newEntityHashMap = new HashMap<>();
    private HashMap<String, Object> oldEntityHashMap = new HashMap<>();
    private List<Long> updatesObjectsIds = new ArrayList<>();
    private static final String FIELD_ID = "id";
    private String action = Strings.EMPTY;
    public static final String FR_SPARKIT_ACCOUNTING_ENTITIES = "fr.sparkit.accounting.entities";
    public static final String AN_EXCEPTION_IS_OCCURED = "An exception is occured :{} ";
    public static final String IS_DELETED = "isDeleted";
    public static final String USER_ID_HEADER = "userId";
    public static final String TEST_PROFILE = "test";
    public static final String USER_TEST = "user-test";
    public static final String USER_SWAGGER = "user-swagger";

    @Autowired
    public HistoricListner(IHistoricDao historicDao, Environment environment) {
        this.historicDao = historicDao;
        this.environment = environment;
    }

    @After("execution(* fr.sparkit.accounting.dao.BankReconciliationStatementDao.saveAll(*)) && args(entitiesToCreate,..)")
    public void handleAfterSaveAll(Iterable<Object> entitiesToCreate) {
        entitiesToCreate.forEach(entityCreated -> {
            if (action.equals(HistoricActionEnum.INSERTED.toString()) && checkIsAuditedEntity(entityCreated)) {
                Long idEntity = null;
                try {
                    idEntity = getIdFromEntity(entityCreated);
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    log.error(AN_EXCEPTION_IS_OCCURED + e);
                }
                if (!updatesObjectsIds.contains(idEntity)) {
                    historicDao.saveAndFlush(new Historic(HistoricActionEnum.INSERTED.toString(),
                            entityCreated.getClass().getSimpleName(), idEntity, Strings.EMPTY, Strings.EMPTY,
                            Strings.EMPTY, LocalDateTime.now(), getCurrentUser()));
                    log.info("Entity : {} with id : {}  is created ", entityCreated.getClass().getSimpleName(),
                            idEntity);
                }

            }
        });

    }
    @Before("execution(* fr.sparkit.accounting.dao.BaseRepository.saveAndFlush(..)) && args(entityToUpdate,..)")
    public void handleBeforeSaveAndFlash(Object entityToUpdate) throws NoSuchFieldException {
        if (checkIsAuditedEntity(entityToUpdate)) {
            clearHashMapsEntities();
            Long idEntity = null;
            try {
                idEntity = getIdFromEntity(entityToUpdate);
            } catch (IllegalAccessException e) {
                log.error(AN_EXCEPTION_IS_OCCURED, e);
            }
            if (idEntity != null && idEntity > 0) {
                EntityManager entityManager = BeanUtil.getBean(EntityManager.class);
                entityManager.detach(entityToUpdate);
                Object oldEntity = entityManager.find(entityToUpdate.getClass(), idEntity);
                mapAndSaveHistory(entityToUpdate, idEntity, oldEntity);
                action = HistoricActionEnum.UPDATED.toString();
            } else {
                action = HistoricActionEnum.INSERTED.toString();
            }
        }
    }

    private String getCurrentUser() {
        boolean isPresentUserId = Objects.requireNonNull(RequestContextHolder.getRequestAttributes())
                .getAttribute(USER_ID_HEADER, RequestAttributes.SCOPE_REQUEST) != null;
        if (environment.getActiveProfiles().length > 0 && environment.getActiveProfiles()[0] != null
                && environment.getActiveProfiles()[0].equals(TEST_PROFILE)) {
            return USER_TEST;
        } else if (isPresentUserId) {
            return Objects.requireNonNull(RequestContextHolder.getRequestAttributes().getAttribute(USER_ID_HEADER,
                    RequestAttributes.SCOPE_REQUEST)).toString();
        } else {
            return USER_SWAGGER;
        }
    }

    @After("execution(* fr.sparkit.accounting.dao.BaseRepository.saveAndFlush(..)) && args(entityCreated,..)")
    public void handleAfterSaveAndFlash(Object entityCreated) throws NoSuchFieldException {
        if (action.equals(HistoricActionEnum.INSERTED.toString()) && checkIsAuditedEntity(entityCreated)) {
            Long idEntity = null;
            try {
                idEntity = getIdFromEntity(entityCreated);
            } catch (IllegalAccessException e) {
                log.error(AN_EXCEPTION_IS_OCCURED + e);
            }
            historicDao.saveAndFlush(new Historic(HistoricActionEnum.INSERTED.toString(),
                    entityCreated.getClass().getSimpleName(), idEntity, Strings.EMPTY, Strings.EMPTY, Strings.EMPTY,
                    LocalDateTime.now(), getCurrentUser()));
            log.info("Entity : {} with id : {}  is created ", entityCreated.getClass().getSimpleName(), idEntity);
        }
    }

    private void clearHashMapsEntities() {
        newEntityHashMap.clear();
        oldEntityHashMap.clear();
    }

    private void mapAndSaveHistory(Object newEntity, Long idEntity, Object oldEntity) {
        if (oldEntity != null) {
            getFieldsToHistorizeAndData(oldEntity, oldEntityHashMap);
            getFieldsToHistorizeAndData(newEntity, newEntityHashMap);
            mapBigDicimalFieldData(newEntityHashMap);
            replaceObjectWithId(oldEntityHashMap);
            replaceObjectWithId(newEntityHashMap);
            replaceListObjectWithListIds(oldEntityHashMap);
            replaceListObjectWithListIds(newEntityHashMap);
            saveFieldHistoricActionUpdate(oldEntityHashMap, newEntityHashMap, newEntity.getClass().getSimpleName(),
                    idEntity);
        }
    }

    private void replaceObjectWithId(HashMap<String, Object> entityHashMap) {
        entityHashMap.forEach((String key, Object value) -> {
            if (value != null && isEntityTypeFromEntitiesPackage(value.getClass())) {
                try {
                    Long fieldId = getIdFromEntity(value);
                    entityHashMap.put(key, fieldId);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    log.error(AN_EXCEPTION_IS_OCCURED + e);
                }
            }
        });
    }

    private void saveFieldHistoricActionUpdate(Map<String, Object> oldEntity, Map<String, Object> newEntity,
            String entityName, Long idEntity) {
        oldEntity.forEach((String key, Object value) -> {
            if (isNotEqualsFieldValue(key, value, newEntity)) {
                historicDao.saveAndFlush(new Historic(HistoricActionEnum.UPDATED.toString(), entityName, idEntity, key,
                        value.toString(), newEntity.get(key).toString(), LocalDateTime.now(), getCurrentUser()));

            } else if (isOldEntityFieldNull(key, value, newEntity)) {
                historicDao.saveAndFlush(new Historic(HistoricActionEnum.UPDATED.toString(), entityName, idEntity, key,
                        null, newEntity.get(key).toString(), LocalDateTime.now(), getCurrentUser()));

            } else if (isNewEntityFieldNull(key, value, newEntity)) {
                historicDao.saveAndFlush(new Historic(HistoricActionEnum.UPDATED.toString(), entityName, idEntity, key,
                        value.toString(), null, LocalDateTime.now(), getCurrentUser()));
            }
        });
        log.info("Entity {} with id  {}  is updated ", entityName, idEntity);
    }

    private void replaceListObjectWithListIds(HashMap<String, Object> targetField) {
        targetField.forEach((String key, Object value) -> {
            if (value instanceof ArrayList) {
                value = checkTypeFromEntitiesPackageForArrayList(value);
            }
            targetField.put(key, value);
        });
    }

    private Object checkTypeFromEntitiesPackageForArrayList(Object value) {
        return ((ArrayList<?>) value).stream().map((Object fieldObject) -> {
            if (fieldObject != null && isEntityTypeFromEntitiesPackage(fieldObject.getClass())) {
                Long fieldId = null;
                try {
                    fieldId = getIdFromEntity(fieldObject);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    log.error(AN_EXCEPTION_IS_OCCURED + e);
                }
                return fieldId;
            } else {
                return fieldObject;
            }

        }).collect(Collectors.toList());
    }

    private static Long getIdFromEntity(Object target) throws NoSuchFieldException, IllegalAccessException {
        Field idField = target.getClass().getDeclaredField(FIELD_ID);
        idField.setAccessible(Boolean.TRUE);
        return (Long) idField.get(target);
    }

    public void mapBigDicimalFieldData(HashMap<String, Object> entityHashMap) {
        entityHashMap.forEach((String key, Object value) -> {
            if (value instanceof BigDecimal) {
                entityHashMap.put(key, ((BigDecimal) value).setScale(3, RoundingMode.HALF_UP));
            }
        });
    }

    private void getFieldsToHistorizeAndData(Object entityObject,
            HashMap<String, Object> hashMapFieldsToHistorizeWithData) {
        Arrays.stream(entityObject.getClass().getDeclaredFields()).forEach((Field entityField) -> {
            try {
                Field fieldByName = entityObject.getClass().getDeclaredField(entityField.getName());
                fieldByName.setAccessible(Boolean.TRUE);
                if (isAuditedField(entityObject, fieldByName)) {
                    fillHashMapWithData(entityObject, hashMapFieldsToHistorizeWithData, entityField, fieldByName);
                }
            } catch (NoSuchFieldException e) {
                log.error(AN_EXCEPTION_IS_OCCURED, e);
            }
        });
    }
    @Before("execution(* org.springframework.data.repository.CrudRepository.saveAll(*)) && args(entitiesToUpdate,..)")
    public void handleBeforeSaveAll(Iterable<Object> entitiesToUpdate) {
        updatesObjectsIds.clear();
        entitiesToUpdate.forEach(entityToUpdate -> {
            if (checkIsAuditedEntity(entityToUpdate)) {
                clearHashMapsEntities();
                Long idEntity = null;
                try {
                    idEntity = getIdFromEntity(entityToUpdate);
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    log.error(AN_EXCEPTION_IS_OCCURED, e);
                }
                if (idEntity != null && idEntity > 0) {
                    updatesObjectsIds.add(idEntity);
                    EntityManager entityManager = BeanUtil.getBean(EntityManager.class);
                    entityManager.detach(entityToUpdate);
                    Object oldEntity = entityManager.find(entityToUpdate.getClass(), idEntity);
                    mapAndSaveHistory(entityToUpdate, idEntity, oldEntity);
                    action = HistoricActionEnum.UPDATED.toString();
                } else {
                    action = HistoricActionEnum.INSERTED.toString();
                }
            }
        });
    }

    private void fillHashMapWithData(Object entityObject, HashMap<String, Object> hashMap, Field entityField,
            Field fieldByName) {
        try {
            if (fieldByName.get(entityObject) instanceof HibernateProxy) {
                hashMap.put(entityField.getName(), ((HibernateProxy) fieldByName.get(entityObject))
                        .getHibernateLazyInitializer().getImplementation());

            } else if (fieldByName.get(entityObject) instanceof PersistentBag
                    || fieldByName.get(entityObject) instanceof ArrayList) {

                List<PersistentBag> persistentBagList = (List<PersistentBag>) fieldByName.get(entityObject);
                if (persistentBagList.isEmpty()) {
                    hashMap.put(entityField.getName(), null);
                }

                replaceHibernateProxyTypeByEntityType(hashMap, entityField, persistentBagList);
            } else {
                hashMap.put(entityField.getName(), fieldByName.get(entityObject));
            }
        } catch (IllegalAccessException e) {
            log.error(AN_EXCEPTION_IS_OCCURED + e);
        }
    }

    private static void replaceHibernateProxyTypeByEntityType(HashMap<String, Object> hashMap, Field entityField,
            List<PersistentBag> persistentBagList) {
        List<Object> mappedList = new ArrayList<>();
        for (int i = 0; i < persistentBagList.size(); i++) {
            if (persistentBagList.get(i) instanceof HibernateProxy) {
                mappedList.add(
                        ((HibernateProxy) persistentBagList.get(i)).getHibernateLazyInitializer().getImplementation());
            } else {
                mappedList.add(persistentBagList.get(i));
            }
            hashMap.put(entityField.getName(), mappedList);
        }
    }

    private static boolean isEntityTypeFromEntitiesPackage(Class<?> entity) {
        return entity.getPackage().getName().equals(FR_SPARKIT_ACCOUNTING_ENTITIES);
    }

    private static boolean checkIsAuditedEntity(Object target) {
        return target.getClass().isAnnotationPresent(AuditedEntity.class);
    }

    private static boolean isAuditedField(Object entity, Field field) throws NoSuchFieldException {
        return !entity.getClass().getDeclaredField(field.getName()).isAnnotationPresent(NotAuditedField.class);
    }

    private static boolean isNotNullField(String key, Map<String, Object> entity) {
        return entity.get(key) != null;
    }

    private static boolean isNotEqualsFieldValue(String key, Object value, Map<String, Object> newEntity) {
        return (value != null && isNotNullField(key, newEntity)) && !value.equals(newEntity.get(key));
    }

    private static boolean isOldEntityFieldNull(String key, Object value, Map<String, Object> newEntity) {
        return value == null && isNotNullField(key, newEntity);
    }

    private static boolean isNewEntityFieldNull(String key, Object value, Map<String, Object> newEntity) {
        return value != null && !isNotNullField(key, newEntity);
    }

    private void saveFieldHistoricActionDelete(String entityName, Long idEntity) {
        historicDao.saveAndFlush(new Historic(HistoricActionEnum.DELETED.toString(), entityName, idEntity, IS_DELETED,
                Boolean.FALSE.toString(), Boolean.TRUE.toString(), LocalDateTime.now(), getCurrentUser()));
        log.info("Entity : {} with id : {}  is deleted ", entityName, idEntity);
    }

    @Before("execution(* fr.sparkit.accounting.dao.BaseRepository.delete(..)) && args(entityToDelete,..)")
    public void handleDelete(Object entityToDelete) throws NoSuchFieldException {
        if (entityToDelete != null && !(entityToDelete instanceof Long) && checkIsAuditedEntity(entityToDelete)) {
            Long idEntity = null;
            try {
                idEntity = getIdFromEntity(entityToDelete);
            } catch (IllegalAccessException e) {
                log.error(AN_EXCEPTION_IS_OCCURED, e);
            }
            saveFieldHistoricActionDelete(entityToDelete.getClass().getSimpleName(), idEntity);
        }
    }

    @Before("execution(* fr.sparkit.accounting.dao.BaseRepository.deleteInBatchSoft(..)) && args(entitiesToDelete,..)")
    public void handleDeleteInBatch(List<Object> entitiesToDelete) {
        entitiesToDelete.stream().forEach(entityToDelete -> {
            if (checkIsAuditedEntity(entityToDelete)) {
                Long idEntity = null;
                try {
                    idEntity = getIdFromEntity(entityToDelete);
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    log.error(AN_EXCEPTION_IS_OCCURED, e);
                }
                saveFieldHistoricActionDelete(entityToDelete.getClass().getSimpleName(), idEntity);
            }
        });
    }

    @Before("execution(* fr.sparkit.accounting.dao.DocumentAccountLineDao.setDocumentAccountLineIsClose(..)) && args(ids,reconciliationDate ,..)")
    public void handleBeforeReconciliation(List<Long> ids, LocalDate reconciliationDate) throws NoSuchFieldException {
        ids.forEach(idEntity -> {
            clearHashMapsEntities();
            if (idEntity != null && idEntity > 0) {
                EntityManager entityManager = BeanUtil.getBean(EntityManager.class);
                DocumentAccountLine oldEntity = entityManager.find(DocumentAccountLine.class, idEntity);
                DocumentAccountLine entityToUpdate = new DocumentAccountLine();
                try {
                    BeanUtils.copyProperties(entityToUpdate, oldEntity);
                    entityManager.detach(entityToUpdate);
                    entityToUpdate.setClose(true);
                    entityToUpdate.setReconciliationDate(reconciliationDate);
                    mapAndSaveHistory(entityToUpdate, idEntity, oldEntity);
                    action = HistoricActionEnum.UPDATED.toString();
                } catch (IllegalAccessException | InvocationTargetException e) {
                    log.error(AN_EXCEPTION_IS_OCCURED, e);
                }
            }

        });
    }

    @Before("execution(* fr.sparkit.accounting.dao.DocumentAccountLineDao.setDocumentAccountLineIsNotClose(..)) && args(ids,..)")
    public void handleBeforeFreeReconciliation(List<Long> ids) throws NoSuchFieldException {
        ids.forEach(idEntity -> {
            clearHashMapsEntities();
            if (idEntity != null && idEntity > 0) {
                EntityManager entityManager = BeanUtil.getBean(EntityManager.class);
                DocumentAccountLine oldEntity = entityManager.find(DocumentAccountLine.class, idEntity);
                DocumentAccountLine entityToUpdate = new DocumentAccountLine();
                try {
                    BeanUtils.copyProperties(entityToUpdate, oldEntity);
                    entityManager.detach(entityToUpdate);
                    entityToUpdate.setClose(false);
                    entityToUpdate.setReconciliationDate(null);
                    mapAndSaveHistory(entityToUpdate, idEntity, oldEntity);
                    action = HistoricActionEnum.UPDATED.toString();
                } catch (IllegalAccessException | InvocationTargetException e) {
                    log.error(AN_EXCEPTION_IS_OCCURED, e);
                }
            }
        });
    }

}
