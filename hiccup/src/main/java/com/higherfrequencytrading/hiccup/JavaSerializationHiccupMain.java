package com.higherfrequencytrading.hiccup;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.*;

/**
 * @author peter.lawrey
 *         Binary 4x
 *         <p/>
 *         50.0%	90.0%	99.0%	99.9%	worst
 *         nodelay	    140	    150	    260	    670	 36,000 micro-seconds
 *         1 ms	    150	    220	    300	    690	 17,000 micro-seconds
 *         3 ms	    200	    240	    320	    750	  6,400 micro-seconds
 *         10 ms	    210	    240	    320	    790	 16,000 micro-seconds
 *         <p/>
 *         XML 1x
 *         <p/>
 *         50.0%	90.0%	99.0%	99.9%	worst
 *         nodelay	  1,400	  1,600	  2,100	  2,500	 17,000 micro-seconds
 *         1 ms	  1,400	  1,600	  2,100	  2,600	  5,300 micro-seconds
 *         3 ms	  1,500	  1,700	  2,300	  2,700	  4,100 micro-seconds
 *         10 ms	  1,800	  2,000	  2,700	  3,300	 17,000 micro-seconds
 *         <p/>
 *         XML 4x
 *         <p/>
 *         50.0%	90.0%	99.0%	99.9%	worst
 *         nodelay	  2,400	  2,800	  3,500	  5,900	 10,000 micro-seconds
 *         1 ms	  2,400	  2,800	  3,300	  4,100	  8,000 micro-seconds
 *         3 ms	  1,600	  2,500	  3,200	  4,600	 14,000 micro-seconds
 *         10 ms	  1,500	  1,800	  2,600	  3,900	 26,000 micro-seconds
 */
public class JavaSerializationHiccupMain {
    static final int RUNS = Integer.getInteger("run", 10000);

    public static void main(String... args) throws IOException, ClassNotFoundException, InterruptedException, ExecutionException {
        int nThreads = 1;
        ExecutorService service = Executors.newFixedThreadPool(nThreads);
        runTestWithDelay(nThreads, service, "warmup", -1);

        for (int t = 0; t < 3; t++) {
            runTestWithDelay(nThreads, service, "nodelay", -1);
            runTestWithDelay(nThreads, service, "1 ms", 1);
            runTestWithDelay(nThreads, service, "3 ms", 3);
            runTestWithDelay(nThreads, service, "10 ms", 10);

        }
        service.shutdown();
    }

    private static void runTestWithDelay(int nThreads, ExecutorService service, String desc, final long delay) throws InterruptedException, ExecutionException {
        @SuppressWarnings("unchecked")
        Future<Histogram>[] futures = new Future[nThreads];

        for (int i = 0; i < nThreads; i++)
            futures[i] = service.submit(new Callable<Histogram>() {
                @Override
                public Histogram call() throws Exception {
                    return getHistogram();
                }

                private Histogram getHistogram() throws IOException, ClassNotFoundException, InterruptedException {
                    Histogram h = new Histogram(100, 1000, 7);
                    long start = 0, last = start;

                    for (int i = -1000; i < RUNS; i++) {
                        if (i == 0) {
                            h.clear();
                            start = System.nanoTime();
                        }

/*
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
//                                oos.writeObject(last);
                        oos.writeObject(BigDecimal.valueOf(last));
                        oos.writeObject(Calendar.getInstance());
//                                oos.writeObject(new Throwable());
                        oos.close();

                        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
                        for (int j = 0; j < 2; j++)
                            ois.readObject();
*/

                        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                        XMLEncoder encoder = new XMLEncoder(baos2);
//                                encoder.writeObject(BigDecimal.valueOf(last)); // not supported !!
                        encoder.writeObject(Calendar.getInstance());
                        encoder.close();

                        XMLDecoder decoder = new XMLDecoder(new ByteArrayInputStream(baos2.toByteArray()));
                        for (int j = 0; j < 1; j++)
                            decoder.readObject();

                        long now = System.nanoTime();
                        h.sample(now - last);

                        if (i >= 0)
                            if (delay > 0)
                                Thread.sleep(delay);
                        last = System.nanoTime();
                    }
                    return h;
                }
            });
        Histogram[] histograms = new Histogram[nThreads];
        for (int i = 0; i < nThreads; i++)
            histograms[i] = futures[i].get();

        Histogram h = Histogram.add(histograms);

        StringBuilder heading = new StringBuilder("       ");
        StringBuilder values = new StringBuilder(desc);
        for (double perc : new double[]{50, 90, 99, 99.9, 100}) {
            if (perc < 100)
                heading.append("\t").append(perc).append("%");
            else
                heading.append("\tworst");

            long value = h.percentile(perc);
            values.append("\t").append(String.format("%,7d", value / 1000));
        }

        values.append(" micro-seconds");
        if (delay < 0)
            System.out.println(heading);
        System.out.println(values);
    }
}
