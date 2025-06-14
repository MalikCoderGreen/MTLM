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

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        Map<String, Integer> logMap = new ConcurrentHashMap<>();
        logMap.put("ERROR", 0);
        logMap.put("INFO", 0);
        logMap.put("WARN", 0);
        String[] logFiles = {"server1.log", "server2.log", "server3.log"};
        Set<String> inProgressFiles = new HashSet<>();
        var executor = Executors.newFixedThreadPool(3);

        // week 2: implement watch service
        WatchService watchService = FileSystems.getDefault().newWatchService();
        Path directory = Paths.get(".");
        WatchKey watchKey = directory.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);

        while ((watchKey = watchService.take()) != null) {
            List<WatchEvent<?>> events = watchKey.pollEvents();
            System.out.println(events.size());

            for (WatchEvent<?> e : events) {
                String fileName = e.context().toString();
                if (e.kind() == StandardWatchEventKinds.ENTRY_CREATE || e.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {

                // Swap files end up being created causing duplicate key counts.
                  if (!fileName.endsWith("~") && inProgressFiles.add(fileName)) {
                        executor.submit(() -> {
                            try {
                                new LogReaderRunnable(logMap, fileName).run();
                            } finally {
                                inProgressFiles.remove(fileName);
                        }
                  });
                  }
                }
            }

            watchKey.reset();
        }

        executor.shutdown();
    }
}