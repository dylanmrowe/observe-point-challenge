* How does your code work?

IPCountTracker3 is my final solution. You can ignore IpCountTracker and IPCountTracker2 unless you want to see my thought process.

My code uses a queue (producer-consumer), to allow requestHandled() to return immediately. It simply publishes the 
IP to the queue.

I did this to avoid blocking the client while the code waits for a mutex to be 
available. I needed a mutex on my top100 data structure because we can't return 
results while the data structure is being updated. The data structure will be 
being constantly updated when there are >20 million IPs calling it. The mutex 
pauses updates so we can iterate over the data structure safely. We don't want the client 
to be blocked during those pauses. 
So the queue prevents the client from being blocked during that call.

* What is the runtime complexity of each function?

requestHandled - O(1)

evaluateTop100 - O(log(n)) from the Red-Black Tree inserts/deletions

queueConsumer - O(log(n)) from calling evaluateTop100()

clear - O(n)


* What other approaches did you decide not to pursue?

If I had specified that the server must call requestHandled() asynchronously, I wouldn't need the queue. This may have been simpler code-wise. But an in-memory queue gives the fastest function runtime.

I also considered blocking the client and doing all processing/sorting synchronously (I hate that word, by the way). But that obviously would lead to slower response times.

* How would you test this?

The best test would be an acceptance test, where I have my test code generate a list of 20 million IPs, spin up multiple threads that call requestHandled() with those IPs, and then call top100() in the middle of those 20 million IPs being processed. That would simulate a production environment and test for ConcurrentModificationExceptions.

One problem with that is, because top100() is eventually consistent, it's not going to return verifiable results (just verify we get results without exception). To test the eventual consistency, I could run the 20 million IPs, wait for the queue to empty, then verify the final results against expected IP counts.




* My entire thought process (somewhat scatter-brained, you don't need to read this):


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



The below idea proved to not work - Java TreeMaps must use the key for sorting, not the value

     In googling for a map that sorts based on value, I found I can implement a custom comparator for a TreeMap.
     This would give me the in-place sorting I was talking about earlier.

Since Option 1 is invalid (unless you use a 3rd party map with value-sorting), I ended up with some form of Option 2

     So the question now is which is faster -
        1. In-place sorting in a TreeMap as we insert, using a custom comparator, or
        2. Maintaining 2 separate Data Structures, one a HashMap for getting counts, and the other a TreeSet for maintaining a sorted list of IP addresses?

    I believe Option 1 would be faster, because automatic tree-balancing is done in both options, but Option 2 has
    the additional time of string manipulation (building the count-IP Key) and more reads/writes.

    Something still seems off. I'm questioning if sorting during the data collection is really faster than just sorting at the end...
    The internet seems to suggest that you should just sort at the end. Sort-while-reading gives you almost no advantage.

    I would have to implement both and run some execution-time experiments to be sure.

    https://stackoverflow.com/questions/15199319/is-it-faster-to-read-then-sort-or-to-sort-while-reading-an-array

After all these disjointed ideas, and attempts at implementing them, I refocused on the requirements of the problem and came up with the idea of a queue

    In the context of the problem, both the requestHandled() and the top100() methods need to run fast. Doing the sorting
    in the requestHandled() method (via TreeSet) would slow that method down.

    One possible approach is to publish a message to a queue. That way requestHandled() doesn't need to wait for the tree to sort.

    During implementation, I realized that reading anything from my data structures while the data structure was being 
    modified results in undefined behavior