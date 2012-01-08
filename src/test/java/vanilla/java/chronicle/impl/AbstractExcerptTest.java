package vanilla.java.chronicle.impl;

import org.junit.Test;
import vanilla.java.chronicle.Chronicle;
import vanilla.java.chronicle.Excerpt;

import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.*;

/**
 * @author peter.lawrey
 */
public class AbstractExcerptTest {
    @Test
    public void mockTest() {
        Excerpt e = createMock(Excerpt.class);
        expect(e.chronicle()).andReturn(createMock(Chronicle.class));
        expect(e.readByte()).andReturn((byte) -128);
        replay(e);
        AbstractExcerptImpl aei = new AbstractExcerptImpl(e);
        assertEquals(128, aei.readUnsignedByte());
        verify(e);
    }
}
