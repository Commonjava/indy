$(function() {
  var userSectionId = "acl-users";
  var roleSectionId = "acl-roles";
  var permSectionId = "acl-perms";
  
  var userListId = "acl-user-list";
  var roleListId = "acl-role-list";
  var permListId = "acl-perm-list";
  
  $(document).ready( loadUserList() );

  $("#tab-acl").accordion({
    header : "h3"
  });

  $("#tab-acl").bind('accordionchange', function(event, ui) {
    var id = $(ui.newHeader).parent().attr('id');

    if (id === userSectionId) {
      loadUserList();
    } else {
      alert(id);
    }

  });

});

function loadUserList(){
  var userListUrl = userBase + "list";
  
  $.getJSON({
    url: userListUrl,
    success: function( data, textStatus ){
      var users = data.items;
      alert( "Got " + users.length + " users." );
      
      listing = "Users:\n----------";
      $.each( users, function( user ){
        listing = listing + "\n" + user.username;
      });
      
      alert( listing );
      
      $(userListId).CreateTable({ Body: users });
    }
  });
}