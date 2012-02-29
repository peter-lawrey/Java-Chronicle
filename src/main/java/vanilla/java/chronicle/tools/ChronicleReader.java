package vanilla.java.chronicle.tools;

import vanilla.java.chronicle.Excerpt;
import vanilla.java.chronicle.impl.IndexedChronicle;

import java.io.IOException;
import java.nio.ByteOrder;

/**
 * Display records in a Chronicle in a text form.
 *
 * @author peterlawrey
 */
public enum ChronicleReader {
    ;

    public static void main(String... args) throws IOException, InterruptedException {
        if (args.length < 1) {
            System.err.println("Usage: java " + ChronicleReader.class.getName() + " {chronicle-base-path} [from-index]");
            System.exit(-1);
        }
        int dataBitsHintSize = Integer.getInteger("dataBitsHintSize", 24);
        String def = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN ? "Big" : "Little";
        ByteOrder byteOrder = System.getProperty("byteOrder", def).equalsIgnoreCase("Big") ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
        String basePath = args[0];
        long index = args.length > 1 ? Long.parseLong(args[1]) : 0L;
        IndexedChronicle ic = new IndexedChronicle(basePath, dataBitsHintSize, byteOrder);
        Excerpt excerpt = ic.createExcerpt();
        while (true) {
            while (!excerpt.index(index))
                Thread.sleep(50);
            System.out.print(index + ": ");
            int nullCount = 0;
            while (excerpt.remaining() > 0) {
                char ch = (char) excerpt.readUnsignedByte();
                if (ch == 0) {
                    nullCount++;
                    continue;
                }
                if (nullCount > 0)
                    System.out.print(" " + nullCount + "*\\0");
                nullCount = 0;
                if (ch < ' ')
                    System.out.print("^" + (char) (ch + '@'));
                else if (ch > 126)
                    System.out.print("\\x" + Integer.toHexString(ch));
                else
                    System.out.print(ch);
            }
            if (nullCount > 0)
                System.out.print(" " + nullCount + "*\\0");
            System.out.println();
            index++;
        }
    }
}
