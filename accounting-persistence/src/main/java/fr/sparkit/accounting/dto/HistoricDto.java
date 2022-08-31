package fr.sparkit.accounting.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import fr.sparkit.accounting.auditing.HistoricActionEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HistoricDto implements Serializable {
    private static final long serialVersionUID = -467635119312915289L;
    private Long id;
    private HistoricActionEnum action;
    protected String createdBy;
    protected LocalDateTime createdDate;
    protected String lastModifiedBy;
    protected LocalDateTime lastModifiedDate;
    private String entity;
    private Long entityId;
    private String entityField;
    private String fieldOldValue;
    private String fieldNewValue;
    private String entityName;
    private List<DocumentAccountLineDto> documentAccountLineAffected = new ArrayList<>();
    private String entityIds;
    private List<HistoricDto> historicList = new ArrayList<>();
    private String idsList;

    public HistoricDto(String createdBy, LocalDateTime createdDate, String lastModifiedBy,
            LocalDateTime lastModifiedDate, Long id, HistoricActionEnum action, String entity, Long entityId,
            String entityField, String fieldOldValue, String fieldNewValue) {
        super();
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.lastModifiedBy = lastModifiedBy;
        this.lastModifiedDate = lastModifiedDate;
        this.id = id;
        this.action = action;
        this.entity = entity;
        this.entityId = entityId;
        this.entityField = entityField;
        this.fieldOldValue = fieldOldValue;
        this.fieldNewValue = fieldNewValue;
    }

    public HistoricDto(LocalDateTime createdDate, String fieldNewValue, HistoricActionEnum action, String entity,
            String createdBy, String entityIds) {
        super();
        this.createdDate = createdDate;
        this.createdBy = createdBy;
        this.action = action;
        this.entityIds = entityIds;
        this.entity = entity;
        this.fieldNewValue = fieldNewValue;
    }

    public HistoricDto(LocalDateTime createdDate, HistoricActionEnum action, String entity, String createdBy,
            String entityIds, String idsList) {
        super();
        this.createdDate = createdDate;
        this.createdBy = createdBy;
        this.action = action;
        this.entityIds = entityIds;
        this.entity = entity;
        this.idsList = idsList;
    }

}
