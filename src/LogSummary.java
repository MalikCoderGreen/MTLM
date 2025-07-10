import java.time.Instant;
import java.util.Map;

public class LogSummary {
    private String fileName;
    private String timestamp;
    private Map<String, Integer> counts;

    public LogSummary(String fileName, Map<String, Integer> counts) {
        this.fileName = fileName;
        this.timestamp = Instant.now().toString();
        this.counts = counts;
    }

    public String getFileName() {
        return fileName;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Map<String, Integer> getCounts() {
        return counts;
    }
}
