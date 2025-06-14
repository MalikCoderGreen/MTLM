import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LogReaderRunnable implements Runnable {
    Map<String, Integer> sharedMap;
    private String logPath;
    public LogReaderRunnable(Map<String, Integer> sharedMap, String logPath) {
        this.sharedMap = sharedMap;
        this.logPath = logPath;
    }

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        try {
            System.out.printf("%s is handling this task for file: %s\n", threadName, this.logPath);
            BufferedReader reader = new BufferedReader(new FileReader(this.logPath));
            String line = reader.readLine();

            while (line != null) {
                //System.out.println(line);
                for (String level: sharedMap.keySet()) {
                    if (line.contains(level)) {
                        sharedMap.merge(level, 1, Integer::sum);
                        //sharedMap.put(level, sharedMap.get(level) + 1);
                    }
                }
                line = reader.readLine();
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        sharedMap.forEach((K, V) ->
                System.out.printf("%s => Severity: %s, Count: %d\n", threadName, K, V)
        );
    }
}
