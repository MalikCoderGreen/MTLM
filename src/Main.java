import com.fasterxml.jackson.databind.util.JSONPObject;

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

    private static final Logger logger = Logger.getLogger(Main.class.getName());
    public static void main(String[] args) throws IOException, InterruptedException {
        int BOUND = 10;
        int NUM_CONSUMERS = 1;
        String POISON_PILL = "STOP";

        Map<String, Map<String, Integer>> logMap = new ConcurrentHashMap<>();
        ReentrantLock alertLock = new ReentrantLock();
        Map<String, Long> lastModifiedMap = new HashMap<>();
        ExecutorService logMapExecutor = Executors.newFixedThreadPool(NUM_CONSUMERS);
        ExecutorService consumerExecutor = Executors.newFixedThreadPool(NUM_CONSUMERS);

        // week 2: implement watch service
        WatchService watchService = FileSystems.getDefault().newWatchService();
        Path directory = Paths.get("./logs");
        WatchKey watchKey = directory.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY);

        // week 5: consumer queue and flush to output.log.
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
                logger.info("File " + file + " has been processed");
                try {
                    String summaryJSON = JsonFormatter.prettyPrint(logMap.get(file));
                    logSummaryQueue.offer(summaryJSON);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }
        };

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            consumerExecutor.shutdown();

            try {
                if (!consumerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    consumerExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                System.err.println("Executor termination interrupted");
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

        for (int i = 0; i < NUM_CONSUMERS; i++) {
            consumerExecutor.submit(blockingQueueConsumerRunnable);
        }
        try {
            watchDirectory(watchKey, watchService, logMap, lastModifiedMap, alertLock, fileBlockingQueue, logMapExecutor);
        } finally {
            if (!consumerExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                consumerExecutor.shutdownNow();
            }
        }

    }

    public static void watchDirectory(WatchKey watchKey, WatchService watchService, Map<String, Map<String, Integer>> logMap,
                               Map<String, Long> lastModifiedMap, ReentrantLock alertLock, MyBlockingQueue<String> fileBlockingQueue,
                               ExecutorService logMapExecutor) throws IOException, InterruptedException {
        // Main Program loop
        long lastEventTime = System.currentTimeMillis();
        while (true) {
            //logger.info("Beginning of While loop");
            long currentTime = System.currentTimeMillis();

            // Too much time has passed since the last file event; break out of loop
            if (lastEventTime > 0 && currentTime - lastEventTime > 10000) {
                break;
            }

            watchKey = watchService.poll(5, TimeUnit.SECONDS);
            if (watchKey != null) {
                boolean hasRelevant = false;

                List<WatchEvent<?>> events = watchKey.pollEvents();
                for (WatchEvent<?> event : events) {

                    String fileName = event.context().toString();
                    if (fileName.endsWith("~")) {
                        continue;
                    }
                    File file = new File("./logs", fileName);
                    long currentLastModified = file.lastModified();
                    Long lastSeenTimestamp = lastModifiedMap.get(fileName);
                    logger.info("file = " + fileName + " currentLastMod = " + currentLastModified + "; lastSeenTimestamp = " + lastSeenTimestamp);
                    if (lastSeenTimestamp == null || (currentLastModified - lastSeenTimestamp > 500)) {

                        lastModifiedMap.put(fileName, currentLastModified);
                        if (!fileName.endsWith("~")) {
                            logger.info("NEW " + event.kind().toString() + " event for file " + fileName);
                        }

                        // Swap files end up being created causing duplicate key counts.
                        if (fileName.startsWith("server") && !fileName.endsWith("~")) {
                            hasRelevant = true;
                            if (!logMap.containsKey(fileName)) {
                                logMap.putIfAbsent(fileName, new ConcurrentHashMap<>(Map.of(
                                        "ERROR", 0, "INFO", 0, "WARN", 0
                                )));
                            }
                            fileBlockingQueue.enqueue(fileName);
                            logMapExecutor.submit(new LogReaderRunnable(logMap, fileName, alertLock));
                            logger.info("Size of queue = " + fileBlockingQueue.size());
                        }
                    }
                }

                if (hasRelevant) {
                    lastEventTime = currentTime;
                }

                watchKey.reset();
            }
        }

    }
}