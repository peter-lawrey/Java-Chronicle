package com.higherfrequencytrading.chronicle;

/**
 * @author plawrey
 */
public interface ByteStringParser {
    public void parseUTF(Appendable builder, StopCharTester tester);

    public String parseUTF(StopCharTester tester);

    public <E> E parseEnum(Class<E> eClass, StopCharTester tester);

    public long parseLong();

    public double parseDouble();
}
