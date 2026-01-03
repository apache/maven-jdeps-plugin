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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * Consumes the output of the jdeps tool
 *
 * @author Robert Scholte
 *
 */
public class JDepsConsumer extends CommandLineUtils.StringStreamConsumer implements StreamConsumer {

    /**
     * JDK8 Windows: JDK internal API (rt.jar)
     * JDK8 Linux:   JDK internal API (JDK removed internal API)
     * JDK9:         JDK internal API (java.base)
     */
    private static final Pattern JDKINTERNALAPI =
            Pattern.compile(".+->\\s([a-z\\.]+)\\s+(JDK (?:removed )?internal API.*)");

    /**
     * <dl>
     *  <dt>key</dt><dd>The offending package</dd>
     *  <dt>value</dt><dd>Offending details</dd>
     * </dl>
     */
    private Map<String, String> offendingPackages = new HashMap<>();

    private static final Pattern PROFILE = Pattern.compile("\\s+->\\s([a-z\\.]+)\\s+(\\S+)");

    /**
     * <dl>
     *  <dt>key</dt><dd>The package</dd>
     *  <dt>value</dt><dd>The profile</dd>
     * </dl>
     */
    private Map<String, String> profiles = new HashMap<>();

    public void consumeLine(String line) {
        super.consumeLine(line);
        Matcher matcher;

        matcher = JDKINTERNALAPI.matcher(line);
        if (matcher.matches()) {
            offendingPackages.put(matcher.group(1), matcher.group(2));
            return;
        }

        matcher = PROFILE.matcher(line);
        if (matcher.matches()) {
            profiles.put(matcher.group(1), matcher.group(2));
        }
    }

    public Map<String, String> getOffendingPackages() {
        return offendingPackages;
    }

    public Map<String, String> getProfiles() {
        return profiles;
    }
}
