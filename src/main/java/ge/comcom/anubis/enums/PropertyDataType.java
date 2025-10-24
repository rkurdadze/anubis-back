package ge.comcom.anubis.enums;

/**
 * Defines the allowed property data types.
 * Similar to M-Files property definition types.
 */
public enum PropertyDataType {
    TEXT,          // plain text
    INTEGER,       // numeric integer
    FLOAT,         // decimal number
    BOOLEAN,       // true / false
    DATE,          // ISO date
    VALUELIST,     // lookup to a ValueList
    MULTI_VALUELIST // multi-select lookup
}
