package ge.comcom.anubis.entity.meta;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Композитный идентификатор для связки Class ↔ PropertyDef.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ClassPropertyId implements Serializable {

    @Column(name = "class_id")
    private Long classId;

    @Column(name = "property_def_id")
    private Long propertyDefId;
}
