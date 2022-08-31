package fr.sparkit.accounting.entities;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "T_DOCUMENT_ACCOUNT_ATTACHEMENT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = { "isDeleted", "deletedToken" })
public class DocumentAccountAttachement implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "DAA_ID")
    private Long id;

    @Column(name = "DAA_FILE_NAME")
    private String fileName;

    @ManyToOne
    @JoinColumn(name = "DAA_DOCUMENT_ACCOUNT_ID")
    private DocumentAccount documentAccount;

    @Column(name = "DAA_IS_DELETED", columnDefinition = "bit default 0")
    private boolean isDeleted;

    @Column(name = "DAA_DELETED_TOKEN")
    private UUID deletedToken;
}
