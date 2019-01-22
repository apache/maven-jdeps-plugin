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

def buildLog = new File(basedir,'build.log')

def depsLines = buildLog.readLines()
        .dropWhile{ it != 'com.google.common automatic' }  // start line, inclusive
        .takeWhile{ !it.startsWith('[INFO] ---') }                  // end line, inclusive
        .each{ it -> it.trim() }                                    // remove indentation
        .grep()  as Set                                             // remove empty lines

boolean containsOffendingLibrary = false
depsLines.each { it ->
    containsOffendingLibrary |= it.contains( "com/google/guava/guava/25.1-jre/guava-25.1-jre.jar" )
}
assert containsOffendingLibrary
assert depsLines.contains( "com.google.common -> jdk.unsupported" )
assert depsLines.contains( "sun.misc.Unsafe                          See http://openjdk.java.net/jeps/260" )