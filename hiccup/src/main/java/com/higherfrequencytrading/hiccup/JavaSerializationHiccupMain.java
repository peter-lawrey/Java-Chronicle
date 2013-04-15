package com.higherfrequencytrading.hiccup;

import java.io.*;
import java.math.BigDecimal;
import java.util.concurrent.*;

/**
 * @author peter.lawrey
 */
public class JavaSerializationHiccupMain {
    static final int RUN_TIME = Integer.getInteger("runtime", 30);
    static final int RATE = Integer.getInteger("rate", 10);

    public static void main(String... args) throws IOException, ClassNotFoundException, InterruptedException, ExecutionException {
        int nThreads = 4;
        ExecutorService service = Executors.newFixedThreadPool(nThreads);

        for (int t = 0; t < 3; t++) {
            @SuppressWarnings("unchecked")
            Future<Histogram>[] futures = new Future[nThreads];

            for (int i = 0; i < nThreads; i++)
                futures[i] = service.submit(new Callable<Histogram>() {
                    @Override
                    public Histogram call() throws Exception {
                        Histogram h = new Histogram(1000, 10000, 7);
                        long start = System.nanoTime(), last = start, count = -20000 / RATE;
                        do {

                            int iters = (int) Math.ceil(RATE / 1000.0);
                            for (int i = 0; i < iters; i++) {
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                ObjectOutputStream oos = new ObjectOutputStream(baos);
//                                oos.writeObject(last);
                                oos.writeObject(BigDecimal.valueOf(last));
//                                oos.writeObject(Calendar.getInstance());
//                                oos.writeObject(new Throwable());
                                oos.close();

                                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
                                for (int j = 0; j < 1; j++)
                                    ois.readObject();

                                long now = System.nanoTime();
                                h.sample(now - last);
                                last = now;
                            }
                            if (++count == 0)
                                h.clear();
                            if (count >= 0)
                                Thread.sleep((long) Math.ceil(1000.0 / RATE));
                            last = System.nanoTime();
                        } while (last - start < RUN_TIME * 1e9);
                        return h;
                    }
                });
            Histogram[] histograms = new Histogram[nThreads];
            for (int i = 0; i < nThreads; i++)
                histograms[i] = futures[i].get();
            Histogram h = Histogram.add(histograms);

            StringBuilder heading = new StringBuilder("       ");
            StringBuilder values = new StringBuilder("serial");
            for (double perc : new double[]{50, 90, 99, 99.9, 99.99, 100}) {
                if (perc < 100)
                    heading.append("\t").append(perc).append("%");
                else
                    heading.append("\tworst");

                long value = h.percentile(perc);
                values.append("\t").append(String.format("%,7d", value / 1000));
            }
            values.append(" micro-seconds");
            System.out.println(heading);
            System.out.println(values);

        }
        service.shutdown();
    }
}
