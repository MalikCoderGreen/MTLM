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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class Main {

    private static final Logger logger = Logger.getLogger(PrintStatsRunnable.class.getName());
    public static void main(String[] args) throws IOException, InterruptedException {
        int BOUND = 10;
        int NUM_CONSUMERS = 3;
        String POISON_PILL = "STOP";

        Map<String, Map<String, Integer>> logMap = new ConcurrentHashMap<>();
        ReentrantLock alertLock = new ReentrantLock();
        Set<String> inProgressFiles = new HashSet<>();
        ExecutorService executor = Executors.newFixedThreadPool(NUM_CONSUMERS);

        // week 2: implement watch service
        WatchService watchService = FileSystems.getDefault().newWatchService();
        Path directory = Paths.get(".");
        WatchKey watchKey = directory.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY);

        // week 5: consumer queue and flush to output.log.
        ScheduledExecutorService consumerExecutor = Executors.newScheduledThreadPool(1);
        ConcurrentLinkedQueue<String> logSummaryQueue = new ConcurrentLinkedQueue<>();

        // Decouple server log file processing using a BlockingQueue
        MyBlockingQueue<String> fileBlockingQueue = new MyBlockingQueue<>(BOUND);

        Runnable blockingQueueConsumerRunnable = () -> {
            while (true) {
                // Consume from the queue
                String file = fileBlockingQueue.dequeue();
                if (file.equals("STOP")) {
                    break;
                }
                inProgressFiles.remove(file);
                logger.info("File " + file + " has been processed");
            }
        };

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            executor.shutdown();
            consumerExecutor.shutdown();

            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }

                if (!consumerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    consumerExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                System.err.println("Executor termination interrupted");
                executor.shutdownNow();
                consumerExecutor.shutdownNow();
            }

            // Flush contents of Queue to output.log
            if (!logSummaryQueue.isEmpty()) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter("output.log", true))) { // append mode
                    String logSummary;
                    while ((logSummary = logSummaryQueue.poll()) != null) {
                        writer.write(logSummary);
                        writer.newLine();
                    }
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // Close WatchService
            try {
                watchService.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }));

        long lastEventTime = System.currentTimeMillis();
        try {
            while (true) {
                //logger.info("Beginning of While loop");
                long currentTime = System.currentTimeMillis();

                // Too much time has passed since the last file event; break out of loop
                if (lastEventTime > 0 && currentTime - lastEventTime > 10000) {
                    break;
                }

                watchKey = watchService.poll(5, TimeUnit.SECONDS);
                if (watchKey != null) {
                    //logger.info("HEREE WITH lastEventTime = " + lastEventTime + " and currentTime = " + currentTime);
                    boolean hasRelevant = false;

                    List<WatchEvent<?>> events = watchKey.pollEvents();
                    for (WatchEvent<?> event : events) {
                        String fileName = event.context().toString();
                        if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE || event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                            // Swap files end up being created causing duplicate key counts.
                            // Using a set because multiple threads keep counting the same file.
                            if (fileName.startsWith("server") && !fileName.endsWith("~") && inProgressFiles.add(fileName)) {
                                hasRelevant = true;
                                if (!logMap.containsKey(fileName)) {
                                    logMap.putIfAbsent(fileName, new ConcurrentHashMap<>(Map.of(
                                            "ERROR", 0, "INFO", 0, "WARN", 0
                                    )));
                                }

                                fileBlockingQueue.enqueue(fileName);
                            }
                        }
                    }

                    if (hasRelevant) {
                        lastEventTime = currentTime;
                    }

                    watchKey.reset();
                    logger.info("Exiting watchKey if statement");
                }
            }

        } finally {
            for (int i = 0; i < NUM_CONSUMERS; i++) {
                executor.submit(blockingQueueConsumerRunnable);
            }
            for (int i = 0; i < NUM_CONSUMERS; i++) {
                fileBlockingQueue.enqueue("STOP");
            }

            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                consumerExecutor.shutdownNow();
            }
        }
    }
}