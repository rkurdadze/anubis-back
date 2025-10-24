package ge.comcom.anubis.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enumeration of property data types.
 * Matches PostgreSQL enum type 'data_type_enum'.
 */
@Schema(description = "Allowed property data types")
public enum DataType {
    TEXT,
    NUMBER,
    DATE,
    BOOLEAN,
    LOOKUP,
    VALUELIST
}
