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

package vanilla.java.chronicle.impl;

import java.io.File;

/**
 * @author peterlawrey
 */
public enum GlobalSettings {
    ;

    public static final boolean USE_UNSAFE = Boolean.parseBoolean(System.getProperty("test.unsafe", "true"));
    public static final String BASE_DIR = System.getProperty("test.dir", System.getProperty("java.io.tmpdir", "/tmp")) + "/deleteme.iictm.";
    public static final int RUNS = Integer.getInteger("test.size", 30) * 1000 * 1000;
    public static final int WARMUP = Integer.getInteger("test.warmup", 12 * 1000);

    public static void deleteOnExit(String basePath) {
        new File(basePath + ".data").deleteOnExit();
        new File(basePath + ".index").deleteOnExit();
    }
}
