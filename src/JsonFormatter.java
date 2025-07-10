import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class JsonFormatter {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String prettyPrint(Object data) throws Exception {
        try {
            ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
            return writer.writeValueAsString(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}