/*
 * Copyright 2011 Peter Lawrey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package vanilla.java.chronicle.impl;

import org.junit.Test;

import java.nio.ByteBuffer;

import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.*;

/**
 * @author peter.lawrey
 */
public class ByteBufferExcerptTest {
    @Test
    public void mockTest() {
        DirectChronicle dc = createMock(DirectChronicle.class);
        expect(dc.getIndexData(1)).andReturn(1L);
        expect(dc.getIndexData(0)).andReturn(0L);
        expect(dc.acquireDataBuffer(0)).andReturn(ByteBuffer.wrap(new byte[]{-128}));
        expect(dc.positionInBuffer(0)).andReturn(0);
        expect(dc.positionInBuffer(1)).andReturn(1);
        expect(dc.size()).andReturn(1L);
        replay(dc);
        ByteBufferExcerpt aei = new ByteBufferExcerpt(dc);
        aei.index(0);
        assertEquals(128, aei.readUnsignedByte());
        aei.finish();
        verify(dc);
    }
}
