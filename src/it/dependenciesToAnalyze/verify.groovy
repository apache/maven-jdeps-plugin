
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
 
def buildLog = new File( basedir, 'build.log' )

def found = false;

assert buildLog.readLines().each { String line ->

  if ( line.startsWith( '[DEBUG] Executing: ' ) )
  {
    assert line.count( "classes" ) == 1 : "invalid classes count: " + line.count( "classes" )

    // Match any plexus-utils-<version>.jar rather than pinning an exact version,
    // so future Dependabot bumps of this dependency don't need a matching edit here.
    def plexusUtilsJarCount = ( line =~ /plexus-utils-\d+(\.\d+)*\.jar/ ).count
    assert plexusUtilsJarCount == 1 : "invalid plexus-utils jar count: " + plexusUtilsJarCount

    found = true;
  }
}

assert found == true : "Executing line not found"
