package ge.comcom.anubis.entity.core;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "property_value_multi",
        uniqueConstraints = @UniqueConstraint(columnNames = {"property_value_id", "value_list_item_id"}))
@Getter
@Setter
public class PropertyValueMulti {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_value_id", nullable = false)
    private PropertyValue propertyValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "value_list_item_id", nullable = false)
    private ValueListItem valueListItem;
}

