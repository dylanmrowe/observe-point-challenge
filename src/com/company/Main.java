package com.company;

public class Main {



    public static void main(String[] args) {
	    IPCountTracker3 ipCountTracker = new IPCountTracker3();
    }


    /*

    Entire thought process:

    My first idea was to throw everything into a hashMap for O(1) insertion and retrieval. Use IP address as the Key, Count as the value.

    Then I realized you want the Top 100 IP addresses (sorting), so I thought of a TreeMap, which uses a Red-Black tree for sorting.
    But what I really need is keys sorted by their value, and I'm not sure the TreeMap keeps track of that by default. So I googled it.
    It looks like a TreeMap is primarily for maintaining sorted keys. So it wouldn't really give any huge advantage over a HashMap.

    https://docs.oracle.com/javase/8/docs/api/java/util/TreeMap.html

    I could just use a HashMap and throw everything into a SortedSet for the top100() method. But that probably wouldn't meet the <200ms time requirements.
    Some sort of concurrent sorting as we increment counts might be ideal.

    Perhaps, for in-place sorting using a TreeSet, if I combined the count with the IP address in a string, I could
    update the count inside the key, and delete the old one, thereby maintaining a sorted list of the top 100 IPs...
    Sorting-wise, that would only work if I standardized the count with leading zeroes, such as "000000000005-145.87.2.109" for 5 instances of the IP
    address "145.87.2.109".

    And then comes the question of how do I lookup an IP address in the TreeSet when I don't have the count...
    I could have an entirely separate map that has only IP address as the key, use that to get the count, and then
     query the TreeSet with the combined key using the count. This just seems a bit redundant though... too much memory. (20 million * 2)
     But time is the most important factor here. For this problem memory isn't limited. So perhaps this solution has merit.



    ----The below idea proved to not work - Java TreeMaps must use the key for sorting, not the value ----

     In googling for a map that automatically sorts based on value, I found I can implement a custom comparator for a TreeMap.
     This would give me the in-place sorting I was talking about earlier.

     So the question now is which is faster -
        1. In-place sorting in a TreeMap as we insert, using a custom comparator, or
        2. Maintaining 2 separate Data Structures, one a HashMap for getting counts, and the other a TreeSet for maintaining a sorted list of IP addresses?

    I believe Option 1 would be faster, because automatic tree-balancing is done in both options, but Option 2 has
    the additional time of string manipulation (building the count-IP Key) and more reads/writes.

    Something still seems off. I'm questioning if sorting during the data collection is really faster than just sorting at the end...
    The internet seems to suggest that you should just sort at the end. Sort-while-reading gives you almost no advantage.

    I would have to implement both and run some execution-time experiments to be sure.

    https://stackoverflow.com/questions/15199319/is-it-faster-to-read-then-sort-or-to-sort-while-reading-an-array

    ----END section----



    In the context of the problem, both the requestHandled() and the top100() methods need to run fast. Doing the sorting
    in the requestHandled() method (via TreeSet) would slow that method down.

    One possible approach is to publish a message to a queue. That way requestHandled() doesn't need to wait for the tree to sort.


     */
}
