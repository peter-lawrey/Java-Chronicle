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

package com.higherfrequencytrading.chronicle.tools;

import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author peter.lawrey
 */
public class ChronicleToolsTest {
    @Nullable
    Map map1 = null;
    @Nullable
    Map<String, Integer> map2 = null;
    @Nullable
    Map<String, List<Integer>> map3 = null;

    @Test
    public void testGetGenericTypes() throws NoSuchFieldException {
        assertEquals("[class java.lang.Object, class java.lang.Object]",
                Arrays.toString(ChronicleTools.getGenericTypes(ChronicleToolsTest.class.getDeclaredField("map1").getGenericType(), 2)));
        assertEquals("[class java.lang.String, class java.lang.Integer]",
                Arrays.toString(ChronicleTools.getGenericTypes(ChronicleToolsTest.class.getDeclaredField("map2").getGenericType(), 2)));
        assertEquals("[class java.lang.String, interface java.util.List]",
                Arrays.toString(ChronicleTools.getGenericTypes(ChronicleToolsTest.class.getDeclaredField("map3").getGenericType(), 2)));
    }
}
