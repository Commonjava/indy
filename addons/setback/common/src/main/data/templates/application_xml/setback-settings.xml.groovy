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
