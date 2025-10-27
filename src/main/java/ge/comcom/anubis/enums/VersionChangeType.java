package ge.comcom.anubis.enums;

/**
 * Defines all possible change types tracked in ObjectVersionAudit.
 */
public enum VersionChangeType {
    VERSION_CREATED,
    VERSION_SAVED,
    VERSION_DELETED,
    FILE_ADDED,
    FILE_REMOVED,
    FILE_UPDATED,
    FILE_UPLOAD_FAILED
}
