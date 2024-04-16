package com.company;

public class IPCountTuple implements Comparable{
    private String IP;
    private Integer count;

    public IPCountTuple(String IP, Integer count) {
        this.IP = IP;
        this.count = count;
    }

    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    /**
     * Compares by count first, and secondly by IP if counts are the same. This will make the containing TreeSet
     * order the tuples by count.
     * @param other
     * @return
     */
    @Override
    public int compareTo(Object other) {
        int countComparison = this.count.compareTo(((IPCountTuple)other).getCount());
        return countComparison != 0 ? countComparison : this.IP.compareTo(((IPCountTuple) other).IP);
    }


}
