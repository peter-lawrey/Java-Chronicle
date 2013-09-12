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

// based from eishay/jvm-serializers
package data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ReprUtil {
    @NotNull
    public static String repr(@Nullable String s) {
        if (s == null) return "null";
        return '"' + s + '"';
    }

    @NotNull
    public static String repr(@NotNull Iterable<String> it) {
        StringBuilder buf = new StringBuilder();
        buf.append('[');
        String sep = "";
        for (String s : it) {
            buf.append(sep);
            sep = ", ";
            buf.append(repr(s));
        }
        buf.append(']');
        return buf.toString();
    }
}
