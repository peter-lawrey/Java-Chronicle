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

/**
 * @author peter.lawrey
 */
public class GenerateAppendMain {
    public static void main(String... ignored) {
        {
            System.out.println(
                    "    private int appendLong1(long num) {");
            int endIndex = AbstractExcerpt.MAX_NUMBER_LENGTH;
            for (int i = 0; i <= 18; i++) {
                System.out.println(
                        "        numberBuffer[" + --endIndex + "] = (byte) (num % 10L + '0');");
                if (i < 18)
                    System.out.println(
                            "        num /= 10;\n" +
                                    "        if (num <= 0) return " + endIndex + ";");
            }
            System.out.println(
                    "        return " + endIndex + ";\n" +
                            "    }");
        }

        /*
    private int appendDouble1(long num, int precision) {
        int endIndex  = MAX_NUMBER_LENGTH ;
        do {
            numberBuffer[--endIndex] = (byte) (num % 10L + '0');
            num /= 10L;
            if (precision-- == 0)
                numberBuffer[--endIndex] = (byte) '.';
        } while (num > 0L);
        return endIndex;
    }
         */
        {
            System.out.println(
                    "    private int appendDouble1(long num, final int precision) {");
            System.out.println(
                    "        int endIndex = AbstractExcerpt.MAX_NUMBER_LENGTH;");
            for (int i = 0; i <= 18; i++) {
                if (i > 0)
                    System.out.println(
                            "        if (precision == " + i + ") \n" +
                                    "            numberBuffer[--endIndex] = (byte) (num % 10L + '0');");
                System.out.println(
                        "        numberBuffer[--endIndex] = (byte) (num % 10L + '0');");

                if (i < 18)
                    System.out.println(
                            "        num /= 10;\n" +
                                    "        if (num <= 0) return endIndex;");
            }
            System.out.println(
                    "        return endIndex;\n" +
                            "    }");
        }
    }
}
