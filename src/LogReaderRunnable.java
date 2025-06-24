import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class LogReaderRunnable implements Runnable {
    private final Map<String, Map<String, Integer>> sharedMap;
    private final ReentrantLock alertLock;
    private final String logPath;

    public LogReaderRunnable(Map<String, Map<String, Integer>> sharedMap, String logPath, ReentrantLock alertLock) {
        this.sharedMap = sharedMap;
        this.logPath = logPath;
        this.alertLock = alertLock;
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
                for (String level : sharedMap.get(this.logPath).keySet()) {
                    if (line.contains(level)) {
                        sharedMap.get(this.logPath).merge(level, 1, Integer::sum);
                        //sharedMap.put(level, sharedMap.get(level) + 1);
                    }
                }
                line = reader.readLine();
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        sharedMap.get(this.logPath).forEach((K, V) ->
//                System.out.printf("%s => Severity: %s, Count: %d\n", threadName, K, V)
//        );

        checkErrorCount();
    }

    public void checkErrorCount() {
        try {
            System.out.println("checkErrorCount() Thread: " + Thread.currentThread().getName() + " is trying to acquire the lock ");
            this.alertLock.lock();
            if (this.sharedMap.get(this.logPath).containsKey("ERROR") && this.sharedMap.get(this.logPath).get("ERROR") >= 10) {
                System.out.println("Thread: " +  Thread.currentThread().getName() + "TOO MANY ERRORS!!!");
            }
        } finally {
            this.alertLock.unlock();
        }

        System.out.println("Thread " + Thread.currentThread().getName() + " released the lock; exiting checkErrorCount()");
    }
}