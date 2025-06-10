/*

🔄 Week-by-Week Breakdown
✅ Week 1: Build the Core + Concurrency Foundations
Goal: Parse multiple files concurrently.
Use ExecutorService with a fixed thread pool.

Each thread reads a .log file line by line.

Parse lines that match "ERROR", "WARN", etc.

Store results in a ConcurrentHashMap<String, Integer> → key is level, value is count.

Concepts Used: ExecutorService, Callable, Future, ConcurrentHashMap

 */

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class Main {
    private static Map<String, Integer> logMap = new ConcurrentHashMap<>();
    public static void main(String[] args) {
        String[] logFiles = {"server1.log", "server2.log", "server3.log"};
        List<Future<Map<String, Integer>>> futures = new ArrayList<>();
        var executor = Executors.newFixedThreadPool(3);
        for (var logFileName: logFiles) {
            futures.add(executor.submit(new LogReaderCallable(logFileName)));
        }

        for (Future<Map<String, Integer>> rtn: futures) {
            try {
                rtn.get().forEach((K, V) -> {
                    System.out.printf("Severity: %s, Count: %d\n", K, V);
                } );
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        executor.shutdown();
    }
}
