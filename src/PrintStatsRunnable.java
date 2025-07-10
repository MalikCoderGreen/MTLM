import java.util.Map;
import java.util.logging.Logger;

public class PrintStatsThread extends Thread {
    private final Map<String, Map<String, Integer>> logMap;
    private static final Logger logger = Logger.getLogger(PrintStatsThread.class.getName());
    public PrintStatsThread(Map<String, Map<String, Integer>> logMap) {
        this.logMap = logMap;

    }
    @Override
    public void run() {
        for (String logFile: logMap.keySet()) {
            logger.info("Stats for " + logFile);
            logMap.get(logFile).forEach(
                    (K, V) -> {
                        logger.info("Severity: " + K + " Count: " + V);
                    }
            );

        }

    }
}
