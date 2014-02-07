<html>
  <body>
    <h2>Directory listing for ${new java.io.File(path).parent}</h2>
    <ul>
      <li><a href="${parentUrl}">..</a></li>
      <% items.each { 
           if ( it.getPath().endsWith("/") ) { %>
      <li><a href="${storeUrl}/${it.path}index.html">${new java.io.File(it.getPath()).name}/</a></li>
      <%   } else { %>
      <li><a href="${storeUrl}/${it.path}">${new java.io.File(it.getPath()).name}</a></li>
      <%   }
         } %>
    </ul>
  </body>
</html>