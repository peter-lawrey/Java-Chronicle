package vanilla.java.chronicle.tools;

import org.junit.Test;
import vanilla.java.chronicle.Excerpt;
import vanilla.java.chronicle.impl.IndexedChronicle;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static vanilla.java.chronicle.impl.GlobalSettings.*;

/**
 * @author peterlawrey
 */
public class ObjectStreamTest {
    @Test
    public void testSerailization() throws IOException, ClassNotFoundException {
        final String basePath = BASE_DIR + "objects";
        deleteOnExit(basePath);

        IndexedChronicle tsc = new IndexedChronicle(basePath, DATA_BIT_SIZE_HINT);
        tsc.useUnsafe(false /*USE_UNSAFE*/);

        Excerpt<IndexedChronicle> excerpt = tsc.createExcerpt();

        List objects = Arrays.asList(1, 1L, "one", new Date(1));

        excerpt.startExcerpt(293);
        // a new ObjectOutputStream is required for each record as they are not reusable :(
        ObjectOutputStream coos = new ObjectOutputStream(excerpt.outputStream());
        coos.writeObject(objects);
        coos.close();

        excerpt.index(0);
        assertEquals(293, excerpt.remaining());

        // a new ObjectInputStream is required for each record as they are not reusable :(
        ObjectInputStream cois = new ObjectInputStream(excerpt.inputStream());
        List objects2 = (List) cois.readObject();
        assertEquals(objects, objects2);
    }
}
