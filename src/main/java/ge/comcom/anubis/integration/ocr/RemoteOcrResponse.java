package ge.comcom.anubis.integration.ocr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RemoteOcrResponse(
        @JsonProperty("tika_text") String tikaText,
        @JsonProperty("ocr_text") String ocrText,
        @JsonProperty("combined_text") String combinedText,
        @JsonProperty("language_hint") String languageHint,
        List<OcrBlock> blocks
) {
    public List<OcrBlock> blocks() {
        return blocks == null ? Collections.emptyList() : Collections.unmodifiableList(blocks);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OcrBlock(
            String text,
            int left,
            int top,
            int width,
            int height,
            double confidence
    ) {
    }
}
