var repos_loaded = false;

$(function(){
    $("#main-menu ul").append( '<li><a href="#" id="load-repo-panel">Repositories (Proxies)</a></li>' );
    
    $("#load-repo-panel").click( function(){
        if ( !repos_loaded ){
            $('#tabs').tabs( 'add', 'panel-repos.html', 'Repositories (Proxies)' );
            repos_loaded = true;
        }
    });
    
});