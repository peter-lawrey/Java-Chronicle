#Chronicle
This library is an ultra low latency, high throughput, persisted, messaging and event driven in memory database.  The typical latency is as low as 80 nano-seconds and supports throughputs of 5-20 million messages/record updates per second.

This library also supports distributed, durable, observable collections (Map, List, Set)  The performance depends on the data structures used, but simple data structures can achieve throughputs of 5 million elements or key/value pairs in batches (eg addAll or putAll) and 500K elements or key/values per second when added/updated/removed individually.

It uses almost no heap, trivial GC impact, can be much larger than your physical memory size (only limited by the size of your disk) and can be shared *between processes* with better than 1/10th latency of using Sockets over loopback.
It can change the way you design your system because it allows you to have independent processes which can be running or not at the same time (as no messages are lost)  This is useful for restarting services and testing your services from canned data. e.g. like sub-microsecond durable messaging.
You can attach any number of readers, including tools to see the exact state of the data externally. e.g. I use; od -t cx1 {file}  to see the current state.

#Example
```java
public static void main(String[] args) throws Exception {
    final String basePath = "test";
    ChronicleTools.deleteOnExit(basePath);
    final Chronicle chronicle = new IntIndexedChronicle(basePath);
    final Excerpt excerpt = chronicle.createExcerpt();
    final int[] consolidates = new int[]{1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};
    int repeats = 10000;
    //Write
    for (int i = 0; i < repeats; i++) {
        excerpt.startExcerpt(8 + 4 + 4 * consolidates.length);
        excerpt.writeLong(System.nanoTime());
        excerpt.writeInt(consolidates.length);
        for (final int consolidate : consolidates) {
            excerpt.writeInt(consolidate);
        }
        excerpt.finish();
    }
	//Read
    long[] times = new long[repeats];
    int[] nbcs = new int[repeats];
    int count = 0;
    final Excerpt excerpt2 = chronicle.createExcerpt();
    while (excerpt2.nextIndex()) {
        final long timestamp = excerpt2.readLong();
        long time = System.nanoTime() - timestamp;
        times[count] = time;
        final int nbConsolidates = excerpt2.readInt();
        nbcs[count] = nbConsolidates;
        for (int i = 0; i < nbConsolidates; i++) {
            excerpt2.readInt();
        }
        excerpt2.finish();
        count++;
    }
    for (int i = 0; i < count; i++) {
        System.out.print("latency: " + times[i] / repeats / 1e3 + " us average, ");
        System.out.println("nbConsolidates: " + nbcs[i]);
    }
    chronicle.close();
}
```

#Support Group
https://groups.google.com/forum/?fromgroups#!forum/java-chronicle

#Software used to Develop this package
YourKit 11.x - http://www.yourkit.com -  If you don't profile the performance of your application, you are just guessing where the performance bottlenecks are.

IntelliJ CE - http://www.jetbrains.com - My favourite IDE.


#Version History

Version 1.8 - Add MutableDecimal and FIX support.

Version 1.7.1 - Bug fix and OGSi support.
           Sonar and IntelliJ code analysis - thank you, Mani.
           Add appendDate and appendDateTime
           Improved performance for appendLong and appendDouble (Thank you Andrew Bissell)

Version 1.7 - Add support to the DataModel for arbitrary events to be sent such as timestamps, heartbeats, changes in stages which can picked up by listeners.
           Add support for the DataModel for arbitrary annotations on the data so each map/collection can have additional configuration
           Add ConfigProperties which is scoped properties i.e. a single Properties file with a rule based properties.

Version 1.6 - Distributed, durable, observable collections, List, Set and Map.
           Efficient serialization of Java objects.  Java Serialization provided for compatibility but use of Externalizable preferred.
           Minimisation of virtual memory for 32-bit platforms.

Version 1.5 - Publishing Chronicle over TCP.
           Note: the package has changed to com.higherfrequencytrading to support publishing to maven central.

Version 1.4 - Reading/writing enumerated types, Enum and generic.
            Improve replication performance and memory usage (esp for large excerpts)
            Removed the requirement to provide a DataSizeBitsHint when it wasn't clear what this was for.
            Add ChronicleTest to support testing.
            Add support for parsing text to compliment existing appending of text.

Version 1.3 - Minor improvements.

Version 1.2 - Fixed a bug in the handling of writeBoolean.

Version 1.1 - Add support for OutputStream and InputStream required by ObjectOutputStream and ObjectInputStream.  Using Java Serialization is not suggested as its relatively slow, but sometimes its your only option. ;)

Version 1.0 - First formal release available in https://github.com/peter-lawrey/Java-Chronicle/tree/master/repository

Version 0.5.1 - Fix code to compile with Java 6 update 31. (Previously only Java 7 was used)

Version 0.5 - Add support for replication of a Chronicle over TCP to any number of listeners, either as a component or stand alone/independent application. Uses ChronicleSource and ChronicleSink.
            Add ChronicleReader to read records as they are added as text. (like less)

Version 0.4 - Add support for writing text to the log file without creating garbage via com.higherfrequencytrading.chronicle.ByteStringAppender interface. Useful for text logs.

Version 0.3.1 - Add support for 24-bit int and 48-bit long values.

Version 0.3 - Add support for unsigned byte, short and int. Add support for compacted short, unsigned short, int, unsigned int, long and double types. (Type will use half the size for small values otherwise 50% more)

Version 0.2 - Add support for a 32-bit unsigned index. IntIndexedChronicle. This is slightly slower on a 64-bit JVM, but more compact. Useful if you don't need more than 4 GB of data.

Version 0.1 - Can read/write all basic data types. 26 M/second (max) multi-threaded.

It uses memory mapped file to store "excerpts" of a "chronicle"  Initially it only supports an indexed array of data.

#Performance

###Throughput Test - FileLoggingMain
https://github.com/peter-lawrey/Java-Chronicle/blob/master/testing/src/main/java/com/higherfrequencytrading/chronicle/impl/FileLoggingMain.java

This test logs one million lines of text using Chronicle compared with Logger.

To log 1,000,000 messages took 0.234 seconds using Chronicle and 7.347 seconds using Logger

###Throughput Test - IndexedChronicleThroughputMain

Note: These timings include Serialization.  This is important because many performance tests don't include Serialization even though it can be many times slower than the data store they are testing.

https://github.com/peter-lawrey/Java-Chronicle/blob/master/testing/src/main/java/com/higherfrequencytrading/chronicle/impl/IndexedChronicleLatencyMain.java

On a 4.6 GHz, i7-2600, 16 GB of memory, Fast SSD drive. Centos 5.7.

The average RTT latency was 175 ns. The 50/99 / 99.9/99.99%tile latencies were 160/190 / 2,870/3,610 - ByteBuffer (tmpfs)
The average RTT latency was 172 ns. The 50/99 / 99.9/99.99%tile latencies were 160/190 / 2,780/3,520 - Using Unsafe (tmpfs)

The average RTT latency was 180 ns. The 50/99 / 99.9/99.99%tile latencies were 160/190 / 3,110/19,110 - ByteBuffer (ext4)
The average RTT latency was 178 ns. The 50/99 / 99.9/99.99%tile latencies were 160/190 / 3,100/19,090- Using Unsafe (ext4)

https://github.com/peter-lawrey/Java-Chronicle/blob/master/testing/src/main/java/com/higherfrequencytrading/chronicle/impl/IndexedChronicleThroughputMain.java

On a 4.6 GHz, i7-2600, 16 GB of memory, Fast SSD drive. Centos 5.7.

 Took 12.416 seconds to write/read 200,000,000 entries, rate was 16.1 M entries/sec - ByteBuffer (tmpfs)
 Took 9.185 seconds to write/read 200,000,000 entries, rate was 21.8 M entries/sec - Using Unsafe (tmpfs)

 Took 25.693 seconds to write/read 400,000,000 entries, rate was 15.6 M entries/sec - ByteBuffer (ext4)
 Took 19.522 seconds to write/read 400,000,000 entries, rate was 20.5 M entries/sec - Using Unsafe (ext4)

 Took 71.458 seconds to write/read 1,000,000,000 entries, rate was 14.0 M entries/sec - Using Unsafe (ext4)
 Took 141.424 seconds to write/read 2,000,000,000 entries, rate was 14.1 M entries/sec - Using Unsafe (ext4)

 Note: in the last test, it is using 112 GB! of dense virtual memory in Java without showing a dramatic slow down or performance hit.

 The 14.1 M entries/sec is close to the maximum write speed of the SSD as each entry is an average of 28 bytes (with the index) => ~ 400 MB/s

### More compact Index for less than 4 GB of data
https://github.com/peter-lawrey/Java-Chronicle/blob/master/testing/src/main/java/com/higherfrequencytrading/chronicle/impl/IntIndexedChronicleThroughputMain.java

on a 4.6 GHz, i7-2600
Took 6.325 seconds to write/read 100,000,000 entries, rate was 15.8 M entries/sec - ByteBuffer (tmpfs)
Took 4.590 seconds to write/read 100,000,000 entries, rate was 21.8 M entries/sec - Using Unsafe (tmpfs)

Took 7.352 seconds to write/read 100,000,000 entries, rate was 13.6 M entries/sec - ByteBuffer (ext4)
Took 5.283 seconds to write/read 100,000,000 entries, rate was 18.9 M entries/sec - Using Unsafe (ext4)
