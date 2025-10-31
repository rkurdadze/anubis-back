package ge.comcom.anubis.entity.core;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "object_type")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ObjectType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "object_type_id")
    private Long id;

    @Column(nullable = false, unique = true)
    @Comment("Human readable name of the object type")
    private String name; // e.g. "Document", "Customer", "Project"

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vault_id", nullable = false,
            foreignKey = @ForeignKey(name = "object_type_vault_id_fkey"))
    @Comment("Vault that owns all objects of this type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VaultEntity vault;
}
