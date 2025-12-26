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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.jdeps.consumers.JDepsConsumer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.MatchPatterns;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * Abstract Mojo for JDeps
 *
 * @author Robert Scholte
 *
 */
public abstract class AbstractJDepsMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
    private File outputDirectory;

    /**
     * Indicates whether the build will continue even if there are jdeps warnings.
     */
    @Parameter(defaultValue = "true", property = "jdeps.failOnWarning")
    private boolean failOnWarning;

    /**
     * Specifies the version when processing multi-release JAR files version should be an integer >=9 or base.
     *
     * @since 3.1.1
     */
    @Parameter(property = "jdeps.multiRelease")
    private String multiRelease;

    /**
     * Whether only the sources need to be compatible or also every dependency on the classpath.
     *
     * @since 3.1.3
     */
    @Parameter(defaultValue = "true", property = "jdeps.includeClasspath")
    private boolean includeClasspath;

    /**
     * Additional dependencies which should be analyzed besides the classes.
     * Specify as {@code groupId:artifactId}, allowing ant-pattern.
     *
     * E.g.
     * <pre>
     *   &lt;dependenciesToAnalyzeIncludes&gt;
     *     &lt;include&gt;*:*&lt;/include&gt;
     *     &lt;include&gt;org.foo.*:*&lt;/include&gt;
     *     &lt;include&gt;com.foo.bar:*&lt;/include&gt;
     *     &lt;include&gt;dot.foo.bar:utilities&lt;/include&gt;
     *   &lt;/dependenciesToAnalyzeIncludes&gt;
     * </pre>
     */
    @Parameter
    private List<String> dependenciesToAnalyzeIncludes;

    /**
     * Subset of {@link AbstractJDepsMojo#dependenciesToAnalyzeIncludes} which should be not analyzed.
     * Specify as {@code groupId:artifactId}, allowing ant-pattern.
     *
     * E.g.
     * <pre>
     *   &lt;dependenciesToAnalyzeExcludes&gt;
     *     &lt;exclude&gt;org.foo.*:*&lt;/exclude&gt;
     *     &lt;exclude&gt;com.foo.bar:*&lt;/exclude&gt;
     *     &lt;exclude&gt;dot.foo.bar:utilities&lt;/exclude&gt;
     *   &lt;/dependenciesToAnalyzeExcludes&gt;
     * </pre>
     */
    @Parameter
    private List<String> dependenciesToAnalyzeExcludes;

    /**
     * Destination directory for DOT file output
     */
    @Parameter(property = "jdeps.dotOutput")
    private File dotOutput;

    /**
     * <dl>
     *   <dt>package</dt><dd>Print package-level dependencies excluding dependencies within the same archive<dd/>
     *   <dt>class</dt><dd>Print class-level dependencies excluding dependencies within the same archive<dd/>
     *   <dt>&lt;empty&gt;</dt><dd>Print all class level dependencies. Equivalent to -verbose:class -filter:none.<dd/>
     * </dl>
     */
    @Parameter(property = "jdeps.verbose")
    private String verbose;

    /**
     * Finds dependences matching the specified package name.
     *
     * @since 3.1.1.
     */
    @Parameter
    private List<String> packages;

    /**
     * Restrict analysis to classes matching pattern. This option filters the list of classes to be analyzed. It can be
     * used together with <code>-p</code> and <code>-e</code> which apply pattern to the dependences
     */
    @Parameter(property = "jdeps.include")
    private String include;

    /**
     * Restrict analysis to APIs i.e. dependences from the signature of public and protected members of public classes
     * including field type, method parameter types, returned type, checked exception types etc
     */
    @Parameter(defaultValue = "false", property = "jdeps.apionly")
    private boolean apiOnly;

    /**
     * Show profile or the file containing a package
     */
    @Parameter(defaultValue = "false", property = "jdeps.profile")
    private boolean profile;

    /**
     * Recursively traverse all dependencies. The {@code -R} option implies {@code -filter:none}.  If {@code -p},
     * {@code -e}, {@code -f} option is specified, only the matching dependences are analyzed.
     */
    @Parameter(defaultValue = "false", property = "jdeps.recursive")
    private boolean recursive;

    /**
     * Specifies the root module for analysis.
     *
     * @since JDK 1.9.0
     */
    @Parameter(property = "jdeps.module")
    private String module;

    /**
     * Show only internal API usage.
     *
     * @since 3.2.0
     */
    @Parameter(defaultValue = "false", property = "jdeps.jdkinternals")
    private boolean jdkinternals;

    private final ToolchainManager toolchainManager;

    protected AbstractJDepsMojo(ToolchainManager toolchainManager) {
        this.toolchainManager = toolchainManager;
    }

    protected MavenProject getProject() {
        return project;
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!new File(getClassesDirectory()).exists()) {
            getLog().debug("No classes to analyze");
            return;
        }

        String jExecutable;
        try {
            jExecutable = getJDepsExecutable();
        } catch (IOException e) {
            throw new MojoFailureException("Unable to find jdeps command: " + e.getMessage(), e);
        }

        //      Synopsis
        //      jdeps [options] classes ...
        Commandline cmd = new Commandline();
        cmd.setExecutable(jExecutable);

        Set<Path> dependenciesToAnalyze = null;
        try {
            dependenciesToAnalyze = getDependenciesToAnalyze(includeClasspath);
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        addJDepsOptions(cmd, dependenciesToAnalyze);
        addJDepsClasses(cmd, dependenciesToAnalyze);

        JDepsConsumer consumer = new JDepsConsumer();
        executeJDepsCommandLine(cmd, outputDirectory, consumer);

        // @ TODO if there will be more goals, this should be pushed down to AbstractJDKInternals
        if (!consumer.getOffendingPackages().isEmpty()) {
            final String ls = System.lineSeparator();

            StringBuilder msg = new StringBuilder();
            msg.append("Found offending packages:").append(ls);
            for (Map.Entry<String, String> offendingPackage :
                    consumer.getOffendingPackages().entrySet()) {
                msg.append(' ')
                        .append(offendingPackage.getKey())
                        .append(" -> ")
                        .append(offendingPackage.getValue())
                        .append(ls);
            }

            if (isFailOnWarning()) {
                throw new MojoExecutionException(msg.toString());
            }
        }
    }

    protected void addJDepsOptions(Commandline cmd, Set<Path> dependenciesToAnalyze) throws MojoFailureException {
        if (dotOutput != null) {
            cmd.createArg().setValue("-dotoutput");
            cmd.createArg().setFile(dotOutput);
        }

        if (verbose != null) {
            if ("class".equals(verbose)) {
                cmd.createArg().setValue("-verbose:class");
            } else if ("package".equals(verbose)) {
                cmd.createArg().setValue("-verbose:package");
            } else {
                cmd.createArg().setValue("-v");
            }
        }

        try {
            Collection<Path> cp = new ArrayList<>();

            for (Path path : getClassPath()) {
                if (!dependenciesToAnalyze.contains(path)) {
                    cp.add(path);
                }
            }

            if (!cp.isEmpty()) {
                cmd.createArg().setValue("-cp");

                cmd.createArg().setValue(StringUtils.join(cp.iterator(), File.pathSeparator));
            }

        } catch (DependencyResolutionRequiredException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }

        if (packages != null) {
            for (String pkgName : packages) {
                cmd.createArg().setValue("-p");
                cmd.createArg().setValue(pkgName);
            }
        }

        if (include != null) {
            cmd.createArg().setValue("-include");
            cmd.createArg().setValue(include);
        }

        if (profile) {
            cmd.createArg().setValue("-P");
        }

        if (module != null) {
            cmd.createArg().setValue("-m");
            cmd.createArg().setValue(module);
        }

        if (multiRelease != null) {
            cmd.createArg().setValue("--multi-release");
            cmd.createArg().setValue(multiRelease);
        }

        if (apiOnly) {
            cmd.createArg().setValue("-apionly");
        }

        if (recursive) {
            cmd.createArg().setValue("-R");
        }

        if (jdkinternals) {
            cmd.createArg().setValue("-jdkinternals");
        }
    }

    protected Set<Path> getDependenciesToAnalyze(boolean includeClasspath)
            throws DependencyResolutionRequiredException {
        Set<Path> jdepsClasses = new LinkedHashSet<>();

        jdepsClasses.add(Paths.get(getClassesDirectory()));

        if (includeClasspath) {
            jdepsClasses.addAll(getClassPath());
        }

        if (dependenciesToAnalyzeIncludes != null) {
            MatchPatterns includes = MatchPatterns.from(dependenciesToAnalyzeIncludes);

            MatchPatterns excludes;
            if (dependenciesToAnalyzeExcludes != null) {
                excludes = MatchPatterns.from(dependenciesToAnalyzeExcludes);
            } else {
                excludes = MatchPatterns.from(Collections.<String>emptyList());
            }

            for (Artifact artifact : project.getArtifacts()) {
                String versionlessKey = ArtifactUtils.versionlessKey(artifact);

                if (includes.matchesPatternStart(versionlessKey, true)
                        && !excludes.matchesPatternStart(versionlessKey, true)) {
                    jdepsClasses.add(artifact.getFile().toPath());
                }
            }
        }

        return jdepsClasses;
    }

    protected void addJDepsClasses(Commandline cmd, Set<Path> dependenciesToAnalyze) {
        // <classes> can be a pathname to a .class file, a directory, a JAR file, or a fully-qualified class name.
        for (Path dependencyToAnalyze : dependenciesToAnalyze) {
            cmd.createArg().setFile(dependencyToAnalyze.toFile());
        }
    }

    private String getJDepsExecutable() throws IOException {
        Toolchain tc = getToolchain();

        String jdepsExecutable = null;
        if (tc != null) {
            jdepsExecutable = tc.findTool("jdeps");
        }

        String jdepsCommand = "jdeps" + (SystemUtils.IS_OS_WINDOWS ? ".exe" : "");

        File jdepsExe;

        if (StringUtils.isNotEmpty(jdepsExecutable)) {
            jdepsExe = new File(jdepsExecutable);

            if (jdepsExe.isDirectory()) {
                jdepsExe = new File(jdepsExe, jdepsCommand);
            }

            if (SystemUtils.IS_OS_WINDOWS && jdepsExe.getName().indexOf('.') < 0) {
                jdepsExe = new File(jdepsExe.getPath() + ".exe");
            }

            if (!jdepsExe.isFile()) {
                throw new IOException("The jdeps executable '" + jdepsExe + "' doesn't exist or is not a file.");
            }
            return jdepsExe.getAbsolutePath();
        }

        jdepsExe = new File(SystemUtils.getJavaHome() + File.separator + ".." + File.separator + "sh", jdepsCommand);

        // ----------------------------------------------------------------------
        // Try to find jdepsExe from JAVA_HOME environment variable
        // ----------------------------------------------------------------------
        Properties env = CommandLineUtils.getSystemEnvVars();
        if (!jdepsExe.exists() || !jdepsExe.isFile()) {
            String javaHome = env.getProperty("JAVA_HOME");
            if (!StringUtils.isEmpty(javaHome)) {
                if ((!new File(javaHome).getCanonicalFile().exists())
                        || (new File(javaHome).getCanonicalFile().isFile())) {
                    throw new IOException("The environment variable JAVA_HOME=" + javaHome
                            + " doesn't exist or is not a valid directory.");
                }

                jdepsExe = new File(javaHome + File.separator + "bin", jdepsCommand);
            }
        }

        if (!jdepsExe.getCanonicalFile().exists()
                || !jdepsExe.getCanonicalFile().isFile()) {
            // ----------------------------------------------------------------------
            // Try to find jdepsExe from PATH environment variable
            // ----------------------------------------------------------------------
            String path = env.getProperty("PATH");
            if (path == null) {
                path = env.getProperty("Path");
            }
            if (path == null) {
                path = env.getProperty("path");
            }
            if (path != null) {
                String[] pathDirs = path.split(File.pathSeparator);
                for (String pathDir : pathDirs) {
                    if (StringUtils.isBlank(pathDir)) {
                        continue;
                    }
                    File pathJdepsExe = new File(pathDir, jdepsCommand);
                    File canonicalPathJdepsExe = pathJdepsExe.getCanonicalFile();
                    if (canonicalPathJdepsExe.exists()
                            && canonicalPathJdepsExe.isFile()
                            && canonicalPathJdepsExe.canExecute()) {
                        return canonicalPathJdepsExe.getAbsolutePath();
                    }
                }
            }

            throw new IOException(
                    "Unable to locate the jdeps executable. Verify that JAVA_HOME is set correctly or ensure that jdeps is available on the system PATH.");
        }

        if (!jdepsExe.canExecute()) {
            throw new IOException("The jdeps executable '" + jdepsExe + "' is not executable.");
        }
        return jdepsExe.getAbsolutePath();
    }

    private void executeJDepsCommandLine(
            Commandline cmd, File jOutputDirectory, CommandLineUtils.StringStreamConsumer consumer)
            throws MojoExecutionException {
        if (getLog().isDebugEnabled()) {
            // no quoted arguments
            getLog().debug("Executing: "
                    + CommandLineUtils.toString(cmd.getCommandline()).replaceAll("'", ""));
        }

        CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer() {
            @Override
            public void consumeLine(String line) {
                if (!line.startsWith("Picked up JAVA_TOOL_OPTIONS:")) {
                    super.consumeLine(line);
                }
            }
        };
        CommandLineUtils.StringStreamConsumer out;
        if (consumer != null) {
            out = consumer;
        } else {
            out = new CommandLineUtils.StringStreamConsumer();
        }

        try {
            int exitCode = CommandLineUtils.executeCommandLine(cmd, out, err);

            String output = (StringUtils.isEmpty(out.getOutput())
                    ? null
                    : '\n' + out.getOutput().trim());

            if (exitCode != 0) {
                if (StringUtils.isNotEmpty(output)) {
                    getLog().info(output);
                }

                StringBuilder msg = new StringBuilder("\nExit code: ");
                msg.append(exitCode);
                if (StringUtils.isNotEmpty(err.getOutput())) {
                    msg.append(" - ").append(err.getOutput());
                }
                msg.append('\n');
                msg.append("Command line was: ").append(cmd).append('\n').append('\n');

                throw new MojoExecutionException(msg.toString());
            }

            if (StringUtils.isNotEmpty(output)) {
                getLog().info(output);
            }
        } catch (CommandLineException e) {
            throw new MojoExecutionException("Unable to execute jdeps command: " + e.getMessage(), e);
        }

        // ----------------------------------------------------------------------
        // Handle JDeps warnings
        // ----------------------------------------------------------------------

        if (StringUtils.isNotEmpty(err.getOutput()) && getLog().isWarnEnabled()) {
            getLog().warn("JDeps Warnings");

            StringTokenizer token = new StringTokenizer(err.getOutput(), "\n");
            while (token.hasMoreTokens()) {
                String current = token.nextToken().trim();

                getLog().warn(current);
            }
        }
    }

    private Toolchain getToolchain() {
        Toolchain tc = null;
        if (toolchainManager != null) {
            tc = toolchainManager.getToolchainFromBuildContext("jdk", session);

            if (tc == null) {
                // Maven 3.2.6 has plugin execution scoped Toolchain Support
                try {
                    Method getToolchainsMethod = toolchainManager
                            .getClass()
                            .getMethod("getToolchains", MavenSession.class, String.class, Map.class);

                    @SuppressWarnings("unchecked")
                    List<Toolchain> tcs = (List<Toolchain>) getToolchainsMethod.invoke(
                            toolchainManager, session, "jdk", Collections.singletonMap("version", "[1.8,)"));

                    if (tcs != null && !tcs.isEmpty()) {
                        // pick up latest, jdeps of JDK9 has more options compared to JDK8
                        tc = tcs.get(tcs.size() - 1);
                    }
                } catch (NoSuchMethodException
                        | SecurityException
                        | IllegalAccessException
                        | IllegalArgumentException
                        | InvocationTargetException e) {
                    // ignore
                }
            }
        }

        return tc;
    }

    protected boolean isFailOnWarning() {
        return failOnWarning;
    }

    protected abstract String getClassesDirectory();

    protected abstract Collection<Path> getClassPath() throws DependencyResolutionRequiredException;
}
