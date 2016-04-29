<% 
def dir=new java.io.File(path + '/')
%>
<html>
  <head>
    <title>Indy: Directory listing for ${dir} on ${storeKey.name}</title>
    <style media="screen" type="text/css">
      h2{
        color: #333;
      }
      .item-listing{
        list-style: none outside;
      }
      footer{
        border-top: 1px solid #777;
        font-size: small;
      }
    </style>
  </head>
  <body>
    <h2>Directory listing for ${dir} on ${storeKey.name}</h2>
    <ul class="item-listing">
    <% if( parentUrl ) { %><li><a href="${parentUrl}">..</a></li><% } %> 
    <% items.each { key, value -> 
         if ( key.endsWith("/") ) { %>
      <li><a class="item-link" title='sources:\n${value.join("\n")}' href='${key}'>${new java.io.File(key).name}/</a></li>
    <%   } else { %>
      <li><a class="item-link" title='sources:\n${value.join("\n")}' href="${key}">${new java.io.File(key).name}</a></li>
    <%   }
       } %>
    </ul>
    <footer>
      <p>Sources for this page:</p>
      <ul>
      <% sources.each { %>
        <li><a class="source-link" title="${it}" href="${it}">${it}</a></li>
      <% } %>
      </ul>
    </footer>
  </body>
</html>