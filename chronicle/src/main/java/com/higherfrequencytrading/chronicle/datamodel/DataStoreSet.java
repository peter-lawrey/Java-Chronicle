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

package com.higherfrequencytrading.chronicle.datamodel;

import com.higherfrequencytrading.chronicle.Chronicle;
import com.higherfrequencytrading.chronicle.impl.IndexedChronicle;
import com.higherfrequencytrading.chronicle.tcp.InProcessChronicleSink;
import com.higherfrequencytrading.chronicle.tcp.InProcessChronicleSource;
import com.higherfrequencytrading.chronicle.tools.ConfigProperties;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author peter.lawrey
 */
public class DataStoreSet {
    private final ConfigProperties configProperties;
    private final String nodeName;
    private final Map<String, DataStore> dataStoreMap = new LinkedHashMap<String, DataStore>();

    public DataStoreSet(String nodeName, ConfigProperties configProperties) {
        this.nodeName = nodeName;
        this.configProperties = configProperties.addToScope(nodeName);
    }

    public <Model> void inject(Model model) {
        try {
            for (Class<?> type = model.getClass(); type != null && type != Object.class && type != Enum.class; type = type.getSuperclass()) {
                MasterContext defaultContext = type.getAnnotation(MasterContext.class);
                for (Field field : type.getDeclaredFields()) {
                    MasterContext fieldContext = field.getAnnotation(MasterContext.class);
                    String contextName = fieldContext != null ? fieldContext.value() :
                            defaultContext != null ? defaultContext.value() : "local";
                    ConfigProperties configProperties2 = configProperties.addToScope(contextName);
                    String uri = configProperties2.get("uri");
                    DataStore dataStore = acquireDataStore(contextName, uri, contextName.equals(nodeName) ? ModelMode.MASTER : ModelMode.READ_ONLY);
                    dataStore.injectField(model, field);
                }
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private DataStore acquireDataStore(String contextName, String uriText, ModelMode mode) throws IOException {
        DataStore ds = dataStoreMap.get(contextName);
        if (ds == null) {
            URI uri = URI.create(uriText);
            String path = uri.getPath();
            if (path == null)
                throw new AssertionError("uri.path not set for " + contextName);
            if (path.startsWith("/."))
                path = path.substring(1);
            final Chronicle chronicle;
            switch (mode) {
                case MASTER:
                    chronicle = new InProcessChronicleSource(new IndexedChronicle(path), uri.getPort());
                    break;

                case READ_ONLY:
                    chronicle = new InProcessChronicleSink(new IndexedChronicle(path + "/" + nodeName), uri.getHost(), uri.getPort());
                    break;

                default:
                    throw new AssertionError("Unknown ModelMode " + mode);
            }
            dataStoreMap.put(contextName, ds = new DataStore(chronicle, mode));
        }
        return ds;
    }

    public void start() {
        for (DataStore dataStore : dataStoreMap.values()) {
            dataStore.start();
        }
    }

    public void startAtEnd() {
        for (DataStore dataStore : dataStoreMap.values()) {
            dataStore.startAtEnd();
        }
    }

    public void close() {
        for (DataStore dataStore : dataStoreMap.values()) {
            dataStore.close();
        }
    }
}
