import java.util.ArrayDeque;
import java.util.logging.Logger;

public class MyBlockingQueue<T> {
    private int MAX_CAPACITY;
    private ArrayDeque<T> internalDeque;
    private static final Logger logger = Logger.getLogger(MyBlockingQueue.class.getName());

    public MyBlockingQueue(int MAX_CAPACITY) {
        this.MAX_CAPACITY = MAX_CAPACITY;
        internalDeque  = new ArrayDeque<>(MAX_CAPACITY);
    }

    //  need to define custom put and take methods
    public void enqueue(T fileName) {
        synchronized(this) {
            while (internalDeque.size() == MAX_CAPACITY) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }


            internalDeque.add(fileName);
            this.notifyAll();
            logger.info("Added " + fileName + " to the queue");
        }
    }

    public T dequeue() {
        T fileName = null;
        synchronized(this) {
            while (internalDeque.isEmpty()) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            fileName = internalDeque.poll();
            this.notifyAll();
            logger.info("Processing log file " + fileName);
        }

        return fileName;
    }

    public int getMaxCapacity() {
        return this.MAX_CAPACITY;
    }
}