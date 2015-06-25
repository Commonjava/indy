<?xml version="1.0" encoding="UTF-8"?>
<!--
  Copyright (c) 2014 Red Hat, Inc..
  All rights reserved. This program and the accompanying materials
  are made available under the terms of the GNU Public License v3.0
  which accompanies this distribution, and is available at
  http://www.gnu.org/licenses/gpl.html
  
  Contributors:
      Red Hat, Inc. - initial API and implementation
-->
<settings>
  <localRepository>\${user.home}/.m2/repo-${type}-${name}</localRepository>
  <mirrors>
    <mirror>
      <id>${name}</id>
      <mirrorOf>*</mirrorOf>
      <url>${url}</url>
    </mirror>
  </mirrors>
  <profiles>
    <profile>
      <id>resolve-settings</id>
      <repositories>
        <repository>
          <id>central</id>
          <url>${url}</url>
          <releases>
            <enabled>${releases}</enabled>
          </releases>
          <snapshots>
            <enabled>${snapshots}</enabled>
          </snapshots>
        </repository>
      </repositories>
      <pluginRepositories>
        <pluginRepository>
          <id>central</id>
          <url>${url}</url>
          <releases>
            <enabled>${releases}</enabled>
          </releases>
          <snapshots>
            <enabled>${snapshots}</enabled>
          </snapshots>
        </pluginRepository>
      </pluginRepositories>
    </profile>
    <% if(deployEnabled){ %>
    <profile>
      <id>deploy-settings</id>
      <properties>
        <altDeploymentRepository>${name}::default::${url}</altDeploymentRepository>
      </properties>
    </profile>
    <% } %>
  </profiles>
  <activeProfiles>
    <activeProfile>resolve-settings</activeProfile>
    <%if (deployEnabled){%>
    <activeProfile>deploy-settings</activeProfile>
    <% } %>
  </activeProfiles>
</settings>
