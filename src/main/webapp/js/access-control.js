var userSectionId = "acl-users";
var roleSectionId = "acl-roles";
var permSectionId = "acl-perms";

var userListId = "#acl-user-list";
var roleListId = "acl-role-list";
var permListId = "acl-perm-list";

var aclUserListTable;
  
$(function() {
  $(document).ready( loadUserList() );

  $("#tab-acl").accordion({
    header : "h3"
  });

  $("#tab-acl").bind('accordionchange', function(event, ui) {
    var id = $(ui.newHeader).parent().attr('id');

    if (id === userSectionId) {
      loadUserList();
    } else {
      $(userListId + "_wrapper").hide();
      
      alert(id);
    }

  });

});

function loadUserList(){
  if ( aclUserListTable ){
    aclUserListTable.dataTable({ "bRetrieve" : true });
  }
  else{
    var userListUrl = userBase + "list";
    
    $(userListId + "_wrapper").show();
    $(userListId).dataTable({
      "bSort" : true,
      "bProcessing" : true,
      "sAjaxSource" : userListUrl,
      "sAjaxDataProp" : "items",
      "aoColumns" : [
         { "mDataProp" : "username" },
         { "mDataProp" : "firstName" },
         { "mDataProp" : "lastName" },
         { "mDataProp" : "email" },
       ],
    });
    
    aclUserListTable = $(userListId);
  }
}
