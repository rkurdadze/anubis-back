package ge.comcom.anubis.enums;

/**
 * Defines the allowed property data types.
 * Similar to M-Files property definition types.
 */
public enum PropertyDataType {
    TEXT,       // plain text
    NUMBER,     // numeric value (integer or decimal)
    DATE,       // ISO date/time
    BOOLEAN,    // true / false
    LOOKUP,     // reference to another object
    VALUELIST   // lookup to a ValueList
}
