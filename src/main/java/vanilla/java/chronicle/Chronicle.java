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

package vanilla.java.chronicle;

import java.io.Closeable;
import java.nio.ByteOrder;

/**
 * Generic interface for all time-series, indexed data sets.
 *
 * @author peter.lawrey
 */
public interface Chronicle extends Closeable {
    /**
     * @return A name for logging purposes for this Chronicle.
     */
    String name();

    /**
     * @return a new Excerpt of this Chronicle
     */
    Excerpt createExcerpt();

    /**
     * @return The size of this Chronicle.
     */
    long size();

    /**
     * @return The byte order of the index and data in the chronicle.
     */
    ByteOrder byteOrder();

    /**
     * Close this resource.
     */
    void close();
}
