<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Apache Maven JDeps Plugin
The JDeps Plugin uses the jdeps tool to analyze classes for internal API calls. For more information about the standard jdeps tool, please refer to [Java+Dependency+Analysis+Tool](https://wiki.openjdk.java.net/display/JDK8/Java+Dependency+Analysis+Tool).

**NOTE:** The jdeps tool is available since JDK8.

**NOTE:** The plugin has [toolchains support](http://maven.apache.org/guides/mini/guide-using-toolchains.html). When using Apache Maven 3.2.6, the build doesn't need to use toolchains itself, ie. use the `maven-toolchain-plugin`; the `maven-jdeps-plugin` can pick up a `jdk` toolchain defined in `toolchain.xml` independently from build configuration: it chooses a `jdk` toolchain from available configurations that have a version `1.8` or above.

## Goals Overview

The JDeps Plugin has 2 goals:

- [jdeps:jdkinternals](./jdkinternals-mojo.html) checks if main classes depend on internal JDK classes.
- [jdeps:test-jdkinternals](./test-jdkinternals-mojo.html) checks if test classes depend on internal JDK classes.
## Usage

General instructions on how to use the JDeps Plugin can be found on the [usage page](./usage.html). Some more specific use cases are described in the examples given below. Last but not least, users occasionally contribute additional examples, tips or errata to the [plugin's wiki page](http://docs.codehaus.org/display/MAVENUSER/JDeps+Plugin).

In case you still have questions regarding the plugin's usage, please have a look at the [FAQ](./faq.html) and feel free to contact the [user mailing list](./mailing-lists.html). The posts to the mailing list are archived and could already contain the answer to your question as part of an older thread. Hence, it is also worth browsing/searching the [mail archive](./mailing-lists.html).

If you feel like the plugin is missing a feature or has a defect, you can fill a feature request or bug report in our [issue tracker](./issue-management.html). When creating a new issue, please provide a comprehensive description of your concern. Especially for fixing bugs it is crucial that the developers can reproduce your problem. For this reason, entire debug logs, POMs or most preferably little demo projects attached to the issue are very much appreciated. Of course, patches are welcome, too. Contributors can check out the project from our [source repository](./scm.html) and will find supplementary information in the [guide to helping with Maven](http://maven.apache.org/guides/development/guide-helping.html).
