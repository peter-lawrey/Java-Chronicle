package vanilla.java.chronicle;

/**
 * Generic interface for all time-series, indexed data sets.
 *
 * @author peter.lawrey
 */
public interface Chronicle {
    /**
     * @return the format for fields.
     */
    FieldFormat fieldFormat();

    /**
     * @return a new Excerpt of this Chronicle
     */
    Excerpt createExcerpt();

    /**
     * @return The size of this Chronicle.
     */
    long size();
}
