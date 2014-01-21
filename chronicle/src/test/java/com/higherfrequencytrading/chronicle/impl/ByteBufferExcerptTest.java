/*
 * Copyright 2013 Peter Lawrey
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

package com.higherfrequencytrading.chronicle.impl;

/**
 * @author peter.lawrey
 */
public class ByteBufferExcerptTest {
/*    @Test
    public void mockTest() {
        DirectChronicle dc = createMock(DirectChronicle.class);
        expect(dc.getIndexData(1)).andReturn(8L);
        expect(dc.getIndexData(0)).andReturn(0L);
        MappedMemory mbb = createMock(MappedMemory.class);
        expect(mbb.getLong(0)).andReturn(128L);
        expect(mbb.getLong(0)).andReturn(128L);
        expect(mbb.get(0)).andReturn((byte) -128);
        expect(dc.acquireDataBuffer(0)).andReturn(mbb);
        expect(dc.positionInBuffer(0)).andReturn(0);
        expect(dc.positionInBuffer(7)).andReturn(0);
        expect(dc.multiThreaded()).andReturn(true);
        replay(dc);
        replay(mbb);
        ByteBufferExcerpt aei = new ByteBufferExcerpt(dc);
        aei.index(0);
        assertEquals(128, aei.readUnsignedByte());
        aei.finish();
        verify(dc);
    }*/

/*    @Test
    @Ignore
    // todo fix
    public void testAppendDouble() {
        ByteBuffer bb = ByteBuffer.allocate(8 * 1024);

        DirectChronicle dc = createMock(DirectChronicle.class);
        expect(dc.getIndexData(1)).andReturn(8L);
        expect(dc.getIndexData(0)).andReturn(0L);
        MappedByteBuffer mbb = createMock(MappedByteBuffer.class);
        expect(dc.acquireDataBuffer(0)).andReturn(mbb);
        expect(dc.positionInBuffer(7)).andReturn(0);
        expect(dc.positionInBuffer(0)).andReturn(0);
        expect(mbb.getLong(0)).andReturn(128L);
        replay(dc);
        replay(mbb);
        ByteBufferExcerpt aei = new ByteBufferExcerpt(dc);
        aei.index(0);
        double d = 0.001;
        for (int i = 0; i < 200; i++)
            aei.append(d *= 1.1).append('\n');
        String text = new String(bb.array(), 0, aei.position());
        double d2 = 0.001;
        for (String value : text.split("\n")) {
            d2 *= 1.1;
            assertEquals(d2, Double.parseDouble(value));
        }
    }*/

/*    @Test
    @Ignore
    // todo fix
    public void testAppendDoublePrecision() {
        ByteBuffer bb = ByteBuffer.allocate(8 * 1024);

        DirectChronicle dc = createMock(DirectChronicle.class);
        expect(dc.getIndexData(1)).andReturn(1L);
        expect(dc.getIndexData(0)).andReturn(0L);
        MappedByteBuffer mbb = createMock(MappedByteBuffer.class);
        expect(mbb).andDelegateTo(bb);
        expect(dc.acquireDataBuffer(0)).andReturn(mbb);
        expect(dc.positionInBuffer(0)).andReturn(0);
        expect(dc.positionInBuffer(0)).andReturn(0);
        replay(dc);
        replay(mbb);
        ByteBufferExcerpt aei = new ByteBufferExcerpt(dc);
        aei.index(0);
        double d = 0.001;
        for (int i = 0; i < 200; i++)
            aei.append(d *= 1.1, 6).append('\n');
        String text = new String(bb.array(), 0, aei.position());
        double d2 = 0.001;
        for (String value : text.split("\n")) {
            d2 *= 1.1;
            assertEquals(d2, Double.parseDouble(value), 0.5e-6);
        }
    }*/
/*
    @Test
    @Ignore
    // todo fix
    public void testAppendTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

        ByteBuffer bb = ByteBuffer.allocate(8 * 1024);

        DirectChronicle dc = createMock(DirectChronicle.class);
        expect(dc.getIndexData(1)).andReturn(1L);
        expect(dc.getIndexData(0)).andReturn(0L);
        MappedByteBuffer mbb = createMock(MappedByteBuffer.class);
        expect(mbb).andDelegateTo(bb);
        expect(dc.acquireDataBuffer(0)).andReturn(mbb);
        expect(dc.positionInBuffer(0)).andReturn(0);
        expect(dc.positionInBuffer(0)).andReturn(0);
        replay(dc);
        replay(mbb);
        ByteBufferExcerpt aei = new ByteBufferExcerpt(dc);
        aei.index(0);
        long time = 1000;
        for (int i = 0; i < 1000; i++) {
            time *= 1.05;
            if (time >= 86400000)
                break;
            aei.appendTime(time).append('\n');
        }
        String text = new String(bb.array(), 0, aei.position());
        long time2 = 1000, count = 0;
        for (String value : text.split("\n")) {
            time2 *= 1.05;
            String expected = sdf.format(new Date(time2));
            assertEquals("i= " + count++, expected, value);
        }
    }*/
}
