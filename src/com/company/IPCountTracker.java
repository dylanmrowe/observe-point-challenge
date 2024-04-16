package com.company;

import java.util.*;

/**
 * Alert - this implementation is non-functional due to me mistakenly thinking I could get a TreeMap to sort its entries
 * by value.
 *
 * This implementation sacrifices strong consistency for speed and eventual consistency.
 * The top100() method will not return the most recent results.
 */
public class IPCountTracker {
    private TreeMap<String, Integer> sortedIPCounts;
    /**
     * This queue could be an in-memory queue or an AWS queue.
     *   An AWS SQS FIFO queue (FIFO for queue de-duplication) would be safer, but slower to publish to.
     */
    @SomeQueue(queueName = "IPQueue")
    private Queue<String> IPQueue;

    public IPCountTracker() {
        sortedIPCounts = new TreeMap<>();
        IPQueue = new ArrayDeque<>();
    }

    /**
     *
     * This method simply publishes to a queue, so the calling server does not have to wait for the underlying Red-Black Tree to sort itself.
     * @param ipAddress The IP Address of the end-user's request
     */
    public void requestHandled(String ipAddress) {
        IPQueue.add(ipAddress);
    }

    /**
     * This obviously needs more code to actually work... there are ways to implement in-memory queues with listeners
     * in Java, but I'm just pseudocoding this for time.
     *
     * put() and get() in a TreeMap are guaranteed to be O(log(n)).
     * https://docs.oracle.com/javase/8/docs/api/java/util/TreeMap.html
     *
     * Note that this method is happening asynchronously, outside of requestHandled() and top100(),
     * so the calling server will not be blocked on this.
     */
    @SomeQueueConsumer(queueName = "IPQueue")
    public void queueConsumer() {
        try {
            String ipAddress = IPQueue.remove();
            sortedIPCounts.put(ipAddress, sortedIPCounts.get(ipAddress));
        } catch(Exception e) {
            LOGGER.error("Data lost! IP: " + ipAddress);
        }
    }

    /**
     * This method is eventually-consistent (similar to DynamoDB DB reads in AWS) due to the asynchronous nature of the queue processing.
     *
     * I saw no requirement that top100() must be strongly consistent with the data that comes through requestHandled.
     * Speed was the main requirement. So it will return whatever the current top 100 are (which have been pre-sorted outside of this method)
     *
     *
     */
    public top100() {

        // Note: According to the docs for descendingKeySet:
        //  "If the map is modified while an iteration over the set is in progress (except through the iterator's
        //  own remove operation), the results of the iteration are undefined"
        // We need a mutex to block
        NavigableSet<String> setIterator =  sortedIPCounts.descendingKeySet();
        int numIPs = 100;
        List<String> results = new ArrayList<>();
        for(int i=0; i<numIPs; i++) {
            results.add(setIterator.)
        }
    }

}
