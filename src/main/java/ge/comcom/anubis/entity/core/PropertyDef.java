package ge.comcom.anubis.entity.core;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "property_def")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PropertyDef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "property_def_id")
    private Long id;

    @Column(nullable = false)
    private String name; // e.g. "Status", "Expiration Date", "Department"

    @Column(name = "data_type", nullable = false)
    private String dataType; // e.g. "Text", "Number", "Date", "Boolean", "Lookup"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ref_object_type_id")
    private ObjectType refObjectType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "value_list_id")
    private ValueList valueList;

    @Column(name = "allow_multiple")
    private Boolean allowMultiple = false;

    private String description;
}
