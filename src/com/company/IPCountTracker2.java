package com.company;

import java.util.*;

public class IPCountTracker2 {
    private HashMap<String, Integer> IPCounts;

    private TreeMap<Integer, String> top100;

    public IPCountTracker2() {
        IPCounts = new HashMap<>();
        top100 = new TreeMap<>();
    }

    /**
     *  This method updates the count for the given ipAddress as well as updating the list of the top100.
     *
     * @param ipAddress The IP Address of the end-user's request
     */
    public void requestHandled(String ipAddress) {
        Integer currentCount = IPCounts.get(ipAddress);
        Integer newCount;
        if(currentCount == null) {
            newCount = 1;
        } else {
            newCount = currentCount + 1;
        }

        IPCounts.put(ipAddress, newCount);

        evaluateTop100(ipAddress, newCount);


    }

    /**
     * This method maintains a sorted list of the top 100 IP address using a TreeSet, which has a sorted keySet()
     * in ascending order. Insertions are guaranteed to be O(log(n)).
     *
     * https://docs.oracle.com/javase/8/docs/api/java/util/TreeMap.html
     *
     * If this isn't fast enough for requestHandled(), I could change this method to execute asynchronously and put some
     * sort of mutex on top100 to ensure it updates properly.
     * @param ipAddress
     * @param newCount
     */
    private void evaluateTop100(String ipAddress, Integer newCount) {
        if(top100.isEmpty()) {
            top100.put(newCount, ipAddress);
            return;
        }
        Integer minCountForTop100 = top100.firstKey();
        if(top100.size() < 100) {
            top100.put(newCount, ipAddress);
        } else if(newCount > minCountForTop100) {
           top100.remove(minCountForTop100);
           top100.put(newCount, ipAddress);
        }
    }

    /**
     * This method will just return the existing top100 valueSet (in reverse order). It's already sorted due to
     * the Red-Black tree in Java's TreeSet.
     *
     * In a real-world setting, we would need some sort of mutex on top100 because it will be continuously updating.
     * The docs for .values() (and many TreeSet methods) say that if the map is modified during iteration, the value is undefined.
     * So we may need to block updates to top100 while this method is executing.
     *
     *
     */
    public List<String> top100() {
        return Collections.reverse(top100.values());
    }
}
