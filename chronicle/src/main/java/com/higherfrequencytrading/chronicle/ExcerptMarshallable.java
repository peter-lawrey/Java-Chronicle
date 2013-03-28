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

package com.higherfrequencytrading.chronicle;

/**
 * @author peter.lawrey
 */
public interface ExcerptMarshallable {
    /**
     * read an object from an excerpt
     *
     * @param in to read from
     * @throws IllegalStateException if the object could not be read.
     */
    public void readMarshallable(Excerpt in) throws IllegalStateException;

    /**
     * write an object to an excerpt
     *
     * @param out to write to
     */
    public void writeMarshallable(Excerpt out);
}
