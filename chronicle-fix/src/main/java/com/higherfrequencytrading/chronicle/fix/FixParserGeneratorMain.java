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
package com.higherfrequencytrading.chronicle.fix;

import com.higherfrequencytrading.chronicle.math.MutableDecimal;
import com.higherfrequencytrading.chronicle.tools.IOTools;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

/**
 * @author peter.lawrey
 *         <p/>
 *         TODO Under development !!!
 */
public class FixParserGeneratorMain {
    public static void main(String... args) throws IOException, ParserConfigurationException, SAXException {
        BufferedReader reader = IOTools.asReader(args[0]);

        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser saxParser = spf.newSAXParser();
        saxParser.parse(new InputSource(reader), new MyHandler());
    }

    static class MyHandler extends DefaultHandler {
        private final Map<String, FixFieldMeta> fieldMapByNumber = new LinkedHashMap<String, FixFieldMeta>();
        private final Map<String, FixFieldMeta> fieldMapByName = new LinkedHashMap<String, FixFieldMeta>();
        private MessageMeta messageMeta = null;
        private final Deque<FieldAddable> fieldAddables = new LinkedList<FieldAddable>();

        enum Mode {ROOT, HEADER, TRAILER, MESSAGES, MESSAGE, FIELDS}

        final List<FixField> headerFields = new ArrayList<FixField>();
        final List<FixField> trailerFields = new ArrayList<FixField>();
        final Map<String, MessageMeta> messageMap = new LinkedHashMap<String, MessageMeta>();
        Mode mode = Mode.ROOT;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName.equals("header")) {
                mode = Mode.HEADER;
                return;
            } else if (qName.equals("trailer")) {
                mode = Mode.TRAILER;
                return;
            } else if (qName.equals("messages")) {
                mode = Mode.MESSAGES;
                return;
            } else if (qName.equals("fields")) {
                mode = Mode.FIELDS;
                return;
            }
            switch (mode) {
                case ROOT:
                    break;

                case HEADER: {
                    String name = attributes.getValue("name");
                    String required = attributes.getValue("required");
                    FixField ff = new FixField(name, required);
                    headerFields.add(ff);
                    break;
                }

                case TRAILER: {
                    String name = attributes.getValue("name");
                    String required = attributes.getValue("required");
                    FixField ff = new FixField(name, required);
                    trailerFields.add(ff);
                    break;
                }

                case MESSAGES: {
                    if (qName.equals("message")) {
                        String name = attributes.getValue("name");
                        String msgtype = attributes.getValue("msgtype");
                        String msgcat = attributes.getValue("msgcat");

                        messageMeta = new MessageMeta(name, msgtype, msgcat);
                        messageMap.put(msgtype, messageMeta);
                        mode = Mode.MESSAGE;
                        fieldAddables.clear();
                        fieldAddables.addFirst(messageMeta);
                    }
                    break;
                }
                case MESSAGE: {
                    String name = attributes.getValue("name");
                    String required = attributes.getValue("required");
                    FixField ff = new FixField(name, required);
                    fieldAddables.getFirst().add(ff);
                    if (qName.equals("group"))
                        fieldAddables.addFirst(ff);
                    break;
                }

                case FIELDS: {
                    if (qName.equals("field")) {
                        String number = attributes.getValue("number");
                        String name = attributes.getValue("name");
                        String type = attributes.getValue("type");
                        FixFieldMeta ff = new FixFieldMeta(number, name, type);
                        fieldMapByNumber.put(number, ff);
                        fieldMapByName.put(name, ff);
                    } else {

                    }
                    break;
                }
            }
            System.out.print("[" + qName);
            for (int i = 0, len = attributes.getLength(); i < len; i++)
                System.out.print(" " + attributes.getQName(i) + "=" + attributes.getValue(i));
            System.out.println();
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals("message")) {
                mode = Mode.MESSAGES;
            } else if (qName.equals("group")) {
                if (!fieldAddables.isEmpty())
                    fieldAddables.removeFirst();
            }
        }

        @Override
        public void endDocument() throws SAXException {
            System.out.println("headerFields = " + headerFields);
            System.out.println("trailerFields = " + trailerFields);
            for (Map.Entry<String, MessageMeta> entry : messageMap.entrySet()) {
                System.out.println(entry.getValue());
            }
            System.out.println("fields = " + fieldMapByNumber.values());

            for (FixFieldMeta fixFieldMeta : fieldMapByName.values()) {
                System.out.print("\t" + fixFieldMeta.type.clazz.getSimpleName() + " " + toCamelCase(fixFieldMeta.name) + " = ");
                if (fixFieldMeta.type.clazz.isPrimitive())
                    System.out.println("0;");
                else
                    System.out.println("new " + fixFieldMeta.type.clazz.getSimpleName() + "();");
            }
        }

        private String toCamelCase(String name) {
            return Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
    }

    enum FieldType {
        AMT(MutableDecimal.class), BOOLEAN(boolean.class), CHAR(char.class),
        COUNTRY(String.class), CURRENCY(String.class), DATA(StringBuilder.class),
        EXCHANGE(StringBuilder.class), FLOAT(double.class), INT(long.class),
        LENGTH(int.class), LOCALMKTDATE(Date.class), MONTHYEAR(Date.class),
        MULTIPLEVALUESTRING(EnumSet.class), NUMINGROUP(int.class), PERCENTAGE(double.class),
        PRICE(MutableDecimal.class), PRICEOFFSET(MutableDecimal.class), QTY(MutableDecimal.class),
        SEQNUM(long.class), STRING(StringBuilder.class),
        UTCDATEONLY(Date.class), UTCTIMEONLY(Date.class), UTCTIMESTAMP(Date.class);

        private final Class clazz;

        FieldType(Class clazz) {

            this.clazz = clazz;
        }
    }

    static class FixFieldMeta {
        private final String number;
        private final String name;
        private final FieldType type;

        public FixFieldMeta(String number, String name, String type) {
            assert number != null;
            assert name != null;
            assert type != null;
            this.number = number;
            this.name = name;
            this.type = FieldType.valueOf(type);
        }

        @Override
        public String toString() {
            return "FixFieldMeta{" +
                    "number='" + number + '\'' +
                    ", name='" + name + '\'' +
                    ", type='" + type + '\'' +
                    '}';
        }
    }

    interface FieldAddable {
        public void add(FixField fixField);
    }

    static class FixField implements FieldAddable {
        private final String name;
        private final String required;
        private final List<FixField> fields = new ArrayList<FixField>();

        public FixField(String name, String required) {
            this.name = name;
            this.required = required;
        }

        public void add(FixField ff) {
            fields.add(ff);
        }

        @Override
        public String toString() {
            return "FF{" +
                    "name='" + name + '\'' +
                    ", required='" + required + '\'' +
                    (fields.isEmpty() ? "" : ", fields=" + fields) +
                    '}';
        }
    }

    static class MessageMeta implements FieldAddable {
        private final String name;
        private final String msgtype;
        private final List<FixField> fields = new ArrayList<FixField>();

        public MessageMeta(String name, String msgtype, String msgcat) {
            this.name = name;
            this.msgtype = msgtype;
        }

        public void add(FixField ff) {
            fields.add(ff);
        }

        @Override
        public String toString() {
            return "MessageMeta{" +
                    "name='" + name + '\'' +
                    ", msgtype='" + msgtype + '\'' +
                    ", fields=" + fields +
                    '}';
        }
    }
}
