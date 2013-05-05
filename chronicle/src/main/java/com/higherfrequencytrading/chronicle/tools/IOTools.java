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

import sun.reflect.Reflection;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

/**
 * @author peter.lawrey
 */
public enum IOTools {
    ;
    public static final Charset UTF_8 = Charset.forName("UTF-8");

    /**
     * Provide a normalised path name which can contain SimpleDateFormat syntax.
     * <p/>
     * e.g.  'directory/'yyyyMMdd would produce something like "directory/20130225"
     *
     * @param pathName to use. If it starts or ends with a single quote ' treat as a date format and use the current time
     * @return returns the normalise path.
     */
    public static String normalisePath(String pathName) {
        if (pathName.startsWith("'") || pathName.endsWith("'"))
            return new SimpleDateFormat(pathName).format(new Date());
        return pathName;
    }

    /**
     * Convert a path to a Stream. It looks first in local file system and then the class path.
     * This allows you to override local any file int he class path.
     * <p/>
     * If the name starts with an =, treat the string as the contents.  Useful for unit tests
     * <p/>
     * If the name ends with .gz, treat the stream as compressed.
     * <p/>
     * Formats the name with normalisePath(String).
     *
     * @param name of path
     * @return as an InputStream
     * @throws IOException If the file was not found, or the GZIP Stream was corrupt.
     */
    public static InputStream asStream(String name) throws IOException {
        ClassLoader classLoader = Reflection.getCallerClass(3).getClassLoader();
        return asStream(name, classLoader);
    }

    public static InputStream asStream(String name, ClassLoader classLoader) throws IOException {
        String name2 = normalisePath(name);
        if (name2.startsWith("="))
            return new ByteArrayInputStream(name2.getBytes(UTF_8));
        InputStream in;
        try {
            in = new FileInputStream(name2);
        } catch (FileNotFoundException e) {
            in = classLoader.getResourceAsStream(name2);
            if (in == null)
                throw e;
        }
        if (name2.endsWith(".gz") || name2.endsWith(".GZ"))
            in = new GZIPInputStream(in);
        in = new BufferedInputStream(in);
        return in;
    }

    /**
     * Reader wrapper for asStream above.
     *
     * @param name of the path
     * @return the BufferedReader
     * @throws IOException if the file was not found or the stream was corrupt.
     */
    public static BufferedReader asReader(String name) throws IOException {
        ClassLoader classLoader = Reflection.getCallerClass(3).getClassLoader();
        return new BufferedReader(new InputStreamReader(asStream(name, classLoader), UTF_8));
    }

    /**
     * Read a file as properties
     *
     * @param path to the file
     * @return Properties loaded
     * @throws IOException if the file was not found or the stream was corrupt.
     */
    public static Properties loadProperties(String path) throws IOException {
        BufferedReader br = IOTools.asReader(path);
        Properties prop = new Properties();
        prop.load(br);
        br.close();
        return prop;
    }

    public static void writeAllOrEOF(SocketChannel sc, ByteBuffer bb) throws IOException {
        writeAll(sc, bb);

        if (bb.remaining() > 0) throw new EOFException();
    }

    public static void writeAll(SocketChannel sc, ByteBuffer bb) throws IOException {
        while (bb.remaining() > 0)
            if (sc.write(bb) < 0)
                break;
    }

    public static void readFullyOrEOF(SocketChannel socket, ByteBuffer bb) throws IOException {
        readAvailable(socket, bb);
        if (bb.remaining() > 0) throw new EOFException();
    }

    public static void readAvailable(SocketChannel socket, ByteBuffer bb) throws IOException {
        while (bb.remaining() > 0)
            if (socket.read(bb) < 0)
                break;
    }
}
