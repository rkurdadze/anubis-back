package ge.comcom.anubis.enums;

public enum StorageKindEnum {
    DB,   // Stored directly in the database (BYTEA)
    FS,   // Stored on filesystem path
    S3    // Stored in S3-compatible storage
}
