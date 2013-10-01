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

import com.higherfrequencytrading.chronicle.EnumeratedMarshaller;
import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.StopCharTester;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author peter.lawrey
 */
public class StringMarshaller implements EnumeratedMarshaller<CharSequence> {
    private final StringBuilder reader = new StringBuilder();
    private final StringInterner interner;

    public StringMarshaller(StringInterner interner) {
        this.interner = interner;
    }

    @NotNull
    @Override
    public Class<CharSequence> classMarshaled() {
        return CharSequence.class;
    }

    @Override
    public void write(@NotNull Excerpt excerpt, CharSequence s) {
        excerpt.writeUTF(s);
    }

    @Nullable
    @Override
    public String read(@NotNull Excerpt excerpt) {
        if (excerpt.readUTF(reader))
            return builderToString();
        return null;
    }

    private String builderToString() {
        return interner.intern(reader);
    }


    @Override
    public String parse(@NotNull Excerpt excerpt, @NotNull StopCharTester tester) {
        reader.setLength(0);
        excerpt.parseUTF(reader, tester);
        return builderToString();
    }
}
