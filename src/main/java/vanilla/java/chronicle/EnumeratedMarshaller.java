package vanilla.java.chronicle;

/**
 * @author plawrey
 */
public interface EnumeratedMarshaller<E> {
    public Class<E> classMarshaled();

    public void write(Excerpt excerpt, E e);

    public E read(Excerpt excerpt);

    public E parse(Excerpt excerpt, StopCharTester tester);
}
