import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Callable;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.concurrent.ConcurrentHashMap;

public class LogReaderCallable implements Callable {
    private final String logPath;
    private BufferedReader reader;

    LogReaderCallable(String logPath) {
        this.logPath = logPath;
    }

    @Override
    public Map<String, Integer> call() throws Exception {
        //File logFile = new File(String.valueOf(this.logPath));
        Map<String, Integer> rtnMap = new ConcurrentHashMap<>();
        rtnMap.put("ERROR", 0);
        rtnMap.put("INFO", 0);
        rtnMap.put("WARN", 0);
        String threadName = Thread.currentThread().getName();
        try {
            System.out.printf("%s is handling this task for file: %s\n", threadName, this.logPath);
            reader = new BufferedReader(new FileReader(this.logPath));
            String line = reader.readLine();

            while (line != null) {
                //System.out.println(line);
                for (String level: rtnMap.keySet()) {
                    if (line.contains(level)) {
                        rtnMap.put(level, rtnMap.get(level) + 1);
                    }
                }
                line = reader.readLine();
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rtnMap;
    }
}
