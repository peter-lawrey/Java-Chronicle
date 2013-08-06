This models a more complex example.

There is;
 - one Source thread writing to a chronicle which replicated by InProcessSource/Sink.
 - one Engine with two threads
   - one central broker thread which serializes all inputs.
   - one processing engine thread which does the heavy lifting with the data.
 - one Sink process which receives this data.

The purpose of this demo is to pass a moderately complex event through the system and time at each interval across two/three boxes.

