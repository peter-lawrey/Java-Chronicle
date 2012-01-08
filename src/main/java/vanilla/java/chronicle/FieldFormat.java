package vanilla.java.chronicle;

/**
 * The format for raw fields in an excerpt
 * 
 * @author peter.lawrey
 */
public interface FieldFormat {
    /**
     * Estimate how much space to allow given the capacity for fixed width record.
     * @param fixedCapacity capacity for fixed width binary fields
     * @return the estimate capacity.
     */
    int estimateCapacity(int fixedCapacity);

    /**
     * @return whether the fields are text formatted
     */
    boolean isText();
}
