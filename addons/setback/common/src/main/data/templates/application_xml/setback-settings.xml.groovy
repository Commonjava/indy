/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
<?xml version="1.0" encoding="UTF-8">
<settings>
<% def overridesCentral = false %>
<!-- 
This settings.xml for ${key} incorporates the following stores from AProx:<% allStores.each { %>
  * ${it.key}<% } %>
 --><%
allStores.each {
  if ( it.name == 'central' ){
    overridesCentral = true
  }
}
%>
  <localRepository>${System.getProperty( "user.home" )}/.m2/repository-${key.type.singularEndpointName()}-${key.name}</localRepository>
<% if (!overridesCentral){ %>  <mirrors>
    <mirror>
      <mirrorOf>central</mirrorOf>
      <id>disabled-central</id>
      <url>http://not.used:99999/</url>
    </mirror>
  </mirrors><% } 
%>  <profiles>
    <profile>
      <id>aprox-repos</id>
      <repositories><% remotes.each{ %>
        <repository>
          <id>${it.name}</id>
          <url>${it.url}</url>
          <name>${it.description}</name>
          <releases>
            <enabled>true</enabled>
          </releases>
          <!-- TODO: Not sure how to detect this... -->
          <snapshots>
            <enabled>true</enabled>
          </snapshots>
        </repository><% } %>
      </repositories>
      <pluginRepositories><% remotes.each{ %>
        <pluginRepository>
          <id>${it.name}-plugins</id>
          <url>${it.url}</url>
          <name>${it.description} - PluginRepository</name>
          <releases>
            <enabled>true</enabled>
          </releases>
          <!-- TODO: Not sure how to detect this... -->
          <snapshots>
            <enabled>true</enabled>
          </snapshots>
        </pluginRepository><% } %>
      </pluginRepositories>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>aprox-repos</activeProfile>
  </activeProfiles>
</settings>
