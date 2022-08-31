package fr.sparkit.accounting.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@ToString
@NoArgsConstructor
@Table(name = "T_HISTORIC")
public class Historic extends Auditable {
    private static final long serialVersionUID = -8767494425680321255L;

    @Id
    @GeneratedValue
    @Column(name = "HIS_ID")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "HIS_ACTION")
    private String action;

    @Column(name = "HIS_ENTITY")
    private String entity;

    @Column(name = "HIS_ENTITY_ID")
    private Long entityId;

    @Column(name = "HIS_ENTITY_FIELD")
    private String entityField;

    @Column(name = "HIS_ENTITY_FIELD_OLD_VALUE")
    private String fieldOldValue;

    @Column(name = "HIS_ENTITY_FIELD_NEW_VALUE")
    private String fieldNewValue;

    @Column(name = "HIS_IS_DELETED", columnDefinition = "bit default 0")
    private boolean isDeleted;

    @Column(name = "HIS_DELETED_TOKEN")
    private UUID deletedToken;

    public Historic(String action, String entity, Long entityId, String entityField, String fieldOldValue,
            String fieldNewValue, LocalDateTime createDate, String currentUser) {
        this.action = action;
        this.entity = entity;
        this.entityId = entityId;
        this.entityField = entityField;
        this.fieldOldValue = fieldOldValue;
        this.fieldNewValue = fieldNewValue;
        super.setCreatedDate(createDate);
        super.setCreatedBy(currentUser);
    }
}
