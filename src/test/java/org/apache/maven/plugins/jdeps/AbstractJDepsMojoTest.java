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
package org.apache.maven.plugins.jdeps;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.cli.Commandline;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractJDepsMojoTest {

    private static class TestJDepsMojo extends AbstractJDepsMojo {
        TestJDepsMojo(ToolchainManager toolchainManager) {
            super(toolchainManager);
        }

        @Override
        protected String getClassesDirectory() {
            return "/path/to/classes";
        }

        @Override
        protected Collection<Path> getClassPath() {
            return new HashSet<>();
        }
    }

    @Test
    void testJDKInternalsOptionIsAdded() throws Exception {
        TestJDepsMojo mojo = new TestJDepsMojo(null);

        // Set jdkinternals to true
        Field jdkInternalsField = AbstractJDepsMojo.class.getDeclaredField("jdkinternals");
        jdkInternalsField.setAccessible(true);
        jdkInternalsField.setBoolean(mojo, true);

        Commandline cmd = new Commandline();
        Set<Path> dependenciesToAnalyze = new HashSet<>();
        dependenciesToAnalyze.add(Paths.get("/path/to/classes"));

        mojo.addJDepsOptions(cmd, dependenciesToAnalyze);

        String cmdLine = cmd.toString();
        assertTrue(cmdLine.contains("-jdkinternals"), "Command line should contain -jdkinternals flag");
    }

    @Test
    void testJDKInternalsOptionNotAddedWhenFalse() throws Exception {
        TestJDepsMojo mojo = new TestJDepsMojo(null);

        // Set jdkinternals to false (default)
        Field jdkInternalsField = AbstractJDepsMojo.class.getDeclaredField("jdkinternals");
        jdkInternalsField.setAccessible(true);
        jdkInternalsField.setBoolean(mojo, false);

        Commandline cmd = new Commandline();
        Set<Path> dependenciesToAnalyze = new HashSet<>();
        dependenciesToAnalyze.add(Paths.get("/path/to/classes"));

        mojo.addJDepsOptions(cmd, dependenciesToAnalyze);

        String cmdLine = cmd.toString();
        assertFalse(
                cmdLine.contains("-jdkinternals"), "Command line should not contain -jdkinternals flag when disabled");
    }
}
