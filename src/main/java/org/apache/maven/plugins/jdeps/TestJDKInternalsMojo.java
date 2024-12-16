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

import javax.inject.Inject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.toolchain.ToolchainManager;

/**
 * Check if test classes depend on internal JDK classes
 *
 * @author Robert Scholte
 *
 */
@Mojo(
        name = "test-jdkinternals",
        requiresDependencyResolution = ResolutionScope.TEST,
        defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES,
        threadSafe = true)
public class TestJDKInternalsMojo extends AbstractJDepsMojo {
    /**
     * Indicates whether the build will continue even if there are jdeps warnings.
     */
    @Parameter(defaultValue = "true", property = "jdeps.test.failOnWarning")
    private boolean failOnWarning;

    @Inject
    public TestJDKInternalsMojo(ToolchainManager toolchainManager) {
        super(toolchainManager);
    }

    @Override
    protected boolean isFailOnWarning() {
        return failOnWarning;
    }

    @Override
    protected String getClassesDirectory() {
        return getProject().getBuild().getTestOutputDirectory();
    }

    @Override
    protected Collection<Path> getClassPath() throws DependencyResolutionRequiredException {
        Set<Path> classPath =
                new LinkedHashSet<>(getProject().getTestClasspathElements().size());

        for (String elm : getProject().getTestClasspathElements()) {
            classPath.add(Paths.get(elm));
        }

        return classPath;
    }
}
