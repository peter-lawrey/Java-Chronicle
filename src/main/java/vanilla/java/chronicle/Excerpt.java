package vanilla.java.chronicle;

import java.io.DataInput;
import java.io.DataOutput;

/**
 * An extracted record within a Chronicle.  This record refers to one entry.
 *
 * @author peter.lawrey
 */
public interface Excerpt extends RandomDataInput, RandomDataOutput {
    /**
     * @return the chronicle this is an excerpt for.
     */
    Chronicle chronicle();
    /**
     * Attempt to set the index to this number.  The method is re-tryable as another thread or process could be writing to this Chronicle.
     *
     * @param index within the Chronicle
     * @return true if the index could be set to a valid entry.
     * @throws IndexOutOfBoundsException If index < 0
     */
    boolean index(long index) throws IndexOutOfBoundsException;

    /**
     * @return the index of a valid entry or -1 if the index has never been set.
     */
    long index();

    /**
     * Set the position within this except.
     * @param position to move to.
     * @return this
     */
    Excerpt position(int position);

    /**
     * @return the position within this excerpt
     */
    int position();
    
    /**
     * Change the type of the excerpt
     *
     * @param type of except to change this excerpt to.
     */
    void type(short type);

    /**
     * @return the type of excerpt.
     */
    short type();

    /**
     * @return the capacity of the excerpt.
     */
    int capacity();

    /**
     * Start a new excerpt in the Chronicle.
     *
     * @param type     of excerpt
     * @param capacity minimum capacity to allow for.
     */
    void startExcerpt(short type, int capacity);

    /**
     * Finish a record.  The record is not available until this is called.
     * 
     * When the method is called the first time, the excerpt is shrink wrapped to the size actually used. i.e. where the position is.
     */
    void finish();
}
