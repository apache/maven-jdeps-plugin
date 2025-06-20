/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.jdeps.consumers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JDepsConsumerTest {

    private JDepsConsumer consumer;

    @Test
    public void testJDKInterAPI() {

        consumer = new JDepsConsumer();
        consumer.consumeLine("test-classes -> java.base");
        consumer.consumeLine("   <unnamed> (test-classes)");
        consumer.consumeLine("      -> java.io                                            ");
        consumer.consumeLine("      -> java.lang                                          ");
        consumer.consumeLine(
                "      -> sun.misc                                           JDK internal API (java.base)");

        assertEquals(1, consumer.getOffendingPackages().size());
        assertEquals(
                "JDK internal API (java.base)", consumer.getOffendingPackages().get("sun.misc"));
        assertEquals(0, consumer.getProfiles().size());
    }

    @Test
    public void testJDKInternalAPILinuxJava8() {
        consumer = new JDepsConsumer();
        consumer.consumeLine("classes -> JDK removed internal API");
        consumer.consumeLine("classes -> java.base");
        consumer.consumeLine(
                "   <unnamed>                                          -> java.io                                            java.base");
        consumer.consumeLine(
                "   <unnamed>                                          -> java.lang                                          java.base");
        consumer.consumeLine(
                "   <unnamed>                                          -> sun.misc                                           JDK internal API (JDK removed internal API)");

        assertEquals(1, consumer.getOffendingPackages().size());
        assertEquals(
                "JDK internal API (JDK removed internal API)",
                consumer.getOffendingPackages().get("sun.misc"));
        assertEquals(0, consumer.getProfiles().size());
    }

    @Test
    public void testJDKInternalAPIJava8u291() {
        consumer = new JDepsConsumer();
        consumer.consumeLine("classes -> JDK removed internal API");
        consumer.consumeLine("classes -> java.base");
        consumer.consumeLine(
                "   <unnamed>                                          -> java.io                                            java.base");
        consumer.consumeLine(
                "   <unnamed>                                          -> java.lang                                          java.base");
        consumer.consumeLine(
                "   <unnamed>                                          -> sun.misc                                           JDK removed internal API");

        assertEquals(1, consumer.getOffendingPackages().size());
        assertEquals("JDK removed internal API", consumer.getOffendingPackages().get("sun.misc"));
        assertEquals(0, consumer.getProfiles().size());
    }

    @Test
    public void testProfile() {
        consumer = new JDepsConsumer();
        consumer.consumeLine("E:\\java-workspace\\apache-maven-plugins\\maven-jdeps-plugin\\target\\classes -> "
                + "C:\\Program Files\\Java\\jdk1.8.0\\jre\\lib\\rt.jar (compact1)");
        consumer.consumeLine("   <unnamed> (classes)");
        consumer.consumeLine("      -> java.io                                            compact1");
        consumer.consumeLine("      -> java.lang                                          compact1");
        consumer.consumeLine("      -> sun.misc                                           JDK internal API (rt.jar)");

        assertEquals(1, consumer.getOffendingPackages().size());
        assertEquals(
                "JDK internal API (rt.jar)", consumer.getOffendingPackages().get("sun.misc"));
        assertEquals(2, consumer.getProfiles().size());
        assertEquals("compact1", consumer.getProfiles().get("java.io"));
        assertEquals("compact1", consumer.getProfiles().get("java.lang"));
    }
}
