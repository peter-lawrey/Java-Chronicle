package com.higherfrequencytrading.chronicle.datamodel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author peter.lawrey
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface MasterContext {
    /**
     * @return the unique name of the master of this data structure.
     */
    String value();
}
