package ge.comcom.anubis.entity.core;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

/**
 * Entity для таблицы search_text_cache — хранит текст и индекс для полнотекстового поиска.
 */
@Entity
@Table(name = "search_text_cache")
@Getter
@Setter
@NoArgsConstructor
public class SearchTextCache {

    @Id
    @Column(name = "object_version_id")
    private Long objectVersionId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "object_version_id", referencedColumnName = "version_id", insertable = false, updatable = false)
    private ObjectVersion objectVersion;

    /**
     * Исходный текст, извлечённый из файла (Tika/OCR).
     */
    @Column(name = "extracted_text_raw", columnDefinition = "text")
    private String extractedTextRaw;

    /**
     * tsvector-представление (генерируется PostgreSQL автоматически).
     * Доступно только для чтения.
     */
    @Column(name = "extracted_text_vector", columnDefinition = "tsvector", insertable = false, updatable = false)
    private String extractedTextVector;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}