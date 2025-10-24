package ge.comcom.anubis.entity.core;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "property_value")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PropertyValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "property_value_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "object_version_id", nullable = false)
    private ObjectVersion objectVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_def_id", nullable = false)
    private PropertyDef propertyDef;

    @Column(name = "value_text")
    private String valueText;

    @Column(name = "value_number")
    private BigDecimal valueNumber;

    @Column(name = "value_date")
    private LocalDateTime valueDate;

    @Column(name = "value_boolean")
    private Boolean valueBoolean;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ref_object_id")
    private ObjectEntity refObject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "value_list_item_id")
    private ValueListItem valueListItem;
}
