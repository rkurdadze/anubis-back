package ge.comcom.anubis.mapper.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
public class JsonMapperUtils {

    private final ObjectMapper mapper = new ObjectMapper();

    @JsonMappingQualifier
    @Named("jsonToString")
    public String jsonToString(JsonNode node) {
        if (node == null || node.isNull()) return null;
        try {
            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            return node.toString();
        }
    }

    @JsonMappingQualifier
    @Named("stringToJson")
    public JsonNode stringToJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            // 1) Первичная попытка
            JsonNode n = mapper.readTree(json);

            // 2) Если это текстовый узел с вложенным JSON -> разэкранируем и парсим ещё раз
            if (n.isTextual()) {
                // Преобразуем строковый литерал в «сырую» строку JSON
                String unescaped = mapper.readValue(json, String.class);
                n = mapper.readTree(unescaped);
            }
            return n;
        } catch (Exception e) {
            // Последняя попытка: вдруг пришла сырая строка без кавычек/экрана
            try {
                String unescaped = mapper.readValue("\"" + json.replace("\\", "\\\\").replace("\"", "\\\"") + "\"", String.class);
                return mapper.readTree(unescaped);
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
