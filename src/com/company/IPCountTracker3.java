package com.company;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * This implementation combines the good parts of IPCountTracker and IPCountTracker2:
 *        A Queue for fast response times
 *        Using both a TreeSet (for sorted top100 data) and a HashMap (for data storage)
 * It also adds a mutex on top100IPs so that it doesn't return undefined values during continuous asynchronous updates.
 */
public class IPCountTracker3 {
    private ReentrantLock mutex = new ReentrantLock();

    private ConcurrentHashMap<String, Integer> IPCounts;

    private TreeSet<IPCountTuple> top100IPs;

    @SomeQueue(queueName = "IPQueue")
    private ConcurrentLinkedQueue<String> IPQueue;

    public IPCountTracker3() {
        IPCounts = new ConcurrentHashMap<>();
        top100IPs = new TreeSet<>(new Comparator<IPCountTuple>() {
            @Override
            public int compare(IPCountTuple s1, IPCountTuple s2) {
                return s1.compareTo(s2);
            }
        });
        IPQueue = new ConcurrentLinkedQueue<>();
    }

    /**
     *
     * This method simply publishes to a queue, so the calling server does not have to  wait for the
     * mutex to be unblocked.
     * @param ipAddress The IP Address of the end-user's request
     */
    public void requestHandled(String ipAddress) {
        IPQueue.add(ipAddress);
    }

    /**
     * This obviously needs more code to actually work... there are ways to implement in-memory queues with listeners
     * in Java, but I'm just pseudocoding this for time.
     *
     * put() and get() in a HashMap should usually be O(1), O(log(n)) at worst, depending on how many buckets are in the HashMap.
     * https://docs.oracle.com/javase/8/docs/api/java/util/ConcurrentHashMap.html
     *
     * Note that this method should be happening asynchronously, outside of requestHandled(),
     * so the calling server will not be blocked on this.
     */
    @SomeQueueConsumer(queueName = "IPQueue")
    public void queueConsumer(String ipAddress) {
        try {
            Integer currentCount = IPCounts.get(ipAddress);
            Integer newCount;
            if (currentCount == null) {
                newCount = 1;
            } else {
                newCount = currentCount + 1;
            }

            IPCounts.put(ipAddress, newCount);

            evaluateTop100(ipAddress, newCount);
        } catch (Exception e) {
            LOGGER.error("Data lost! IP: " + ipAddress);
        }

    }

    /**
     * This method maintains a sorted list of the top 100 IP address using a TreeSet, which sorts
     * in ascending order.
     *
     * The idea is this list of 100 will always be available (via mutex), allowing us to avoid sorting a list of 2 million
     * IPs.
     *
     * Insertions/deletions are guaranteed to be O(log(n)).
     *
     * https://docs.oracle.com/javase/8/docs/api/java/util/TreeSet.html
     *
     * @param ipAddress
     * @param newCount
     */
    private void evaluateTop100(String ipAddress, Integer newCount) {
        try {
            mutex.lock();
            if (top100IPs.isEmpty()) {
                top100IPs.add(new IPCountTuple(ipAddress, newCount));
                return; //yes, the below 'finally' block still runs.
            }
            int minCountToQualifyForTop100 = top100IPs.first().getCount() + 1;
            if (top100IPs.size() < 100) {
                top100IPs.remove(new IPCountTuple(ipAddress, newCount-1));
                top100IPs.add(new IPCountTuple(ipAddress, newCount));
            } else if (newCount >= minCountToQualifyForTop100) {
                top100IPs.remove(new IPCountTuple(ipAddress, newCount-1));
                top100IPs.add(new IPCountTuple(ipAddress, newCount));
            }
        } finally {
            mutex.unlock();
        }
    }

    private void clear() {
        IPCounts.clear();
        try {
            mutex.lock();
            top100IPs.clear();
        } finally {
            mutex.unlock();
        }
        IPQueue.clear();
    }

    /**
     * This method is eventually-consistent (similar to DynamoDB DB reads in AWS) due to the asynchronous nature of the queue processing.
     *
     * I saw no requirement that top100() must be strongly consistent with the data that comes through requestHandled.
     *   Speed was the main requirement. So this will wait for the lock on top100, then return whatever the current top 100 are.
     *
     * TODO: However, it's possible this will take a long time to acquire the lock, since there are lots of queue
     *    messages. This could be solved by implementing a 'high-priority' lock, where the top100() method
     *    moves itself to the front of the lock-acquiring queue.
     */
    public List<String> top100() {

        // Why we need this mutex: According to the docs for descendingSet():
        //  "If the map is modified while an iteration over the set is in progress (except through the iterator's
        //  own remove operation), the results of the iteration are undefined".
        // We need a mutex to block so we don't get undefined iteration.
        try {
            mutex.lock();
            return top100IPs.descendingSet().stream().map(IPCountTuple::getIP).collect(Collectors.toList());
        } finally {
            mutex.unlock();
        }

    }
}
