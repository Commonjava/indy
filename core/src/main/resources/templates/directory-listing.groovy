<html>
  <body>
    <h2>Directory listing for ${new java.io.File(path).parent}</h2>
    <ul>
      <li><a href="${parentUrl}">..</a></li>
      <% items.each { 
           if ( it.getPath().endsWith("/") ) { %>
      <li><a title="${org.commonjava.maven.galley.util.PathUtils.normalize( it.location.uri, it.path )}" href='${org.commonjava.maven.galley.util.PathUtils.normalize( storeUrl, it.path, "index.html")}'>${new java.io.File(it.getPath()).name}/</a></li>
      <%   } else { %>
      <li><a title="${org.commonjava.maven.galley.util.PathUtils.normalize( it.location.uri, it.path )}" href="${org.commonjava.maven.galley.util.PathUtils.normalize( storeUrl, it.path)}">${new java.io.File(it.getPath()).name}</a></li>
      <%   }
         } %>
    </ul>
  </body>
</html>