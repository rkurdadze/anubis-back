package ge.comcom.anubis.entity.core;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "value_list_item")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ValueListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "value_list_id", nullable = false)
    private ValueList valueList;

    @Column(name = "value_text", nullable = false)
    private String valueText; // e.g. "Draft", "Approved", "Archived"

    @Column(name = "sort_order")
    private Integer sortOrder;
}
