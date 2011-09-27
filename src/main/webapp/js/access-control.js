var userSectionId = "acl-users";
var roleSectionId = "acl-roles";
var permSectionId = "acl-perms";

var userListId = "#acl-user-list";
var roleListId = "#acl-role-list";
var permListId = "#acl-perm-list";

var aclUserListTable;
var aclRoleListTable;
var aclPermListTable;
  
$(function() {
  $(document).ready( loadUserList() );

  $("#tab-acl").accordion({
    header : "h3"
  });

  $("#tab-acl").bind('accordionchange', function(event, ui) {
    var id = $(ui.newHeader).parent().attr('id');

    alert(id);
    if (id === userSectionId) {
//      if ( aclUserListTable == null ){
        loadUserList();
//      }
    } else if (id === roleSectionId) {
//      if( aclRoleListTable == null ){
        loadRoleList();
//      }
    } else if (id === permSectionId) {
//      if( aclPermListTable == null ){
        loadPermissionList();
//      }
    } else {
    }

  });

});

function loadUserList(){
  if ( aclUserListTable ){
    aclUserListTable.dataTable({ "bRetrieve" : true });
  }
  else{
    var userListUrl = userBase + "list";
    
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

function loadRoleList(){
  if ( aclRoleListTable ){
    aclUserListTable.dataTable({ "bRetrieve" : true });
  }
  else{
    var roleListUrl = roleBase + "list";
    
    $(roleListId).dataTable({
      "bSort" : true,
      "bProcessing" : true,
      "sAjaxSource" : roleListUrl,
      "sAjaxDataProp" : "items",
      "aoColumns" : [
         { "mDataProp" : "name" },
       ],
    });
    
    aclUserListTable = $(userListId);
  }
}

function loadPermissionList(){
  if ( aclPermListTable ){
    aclPermListTable.dataTable({ "bRetrieve" : true });
  }
  else{
    var permListUrl = permBase + "list";
    
    $(permListId).dataTable({
      "bSort" : true,
      "bProcessing" : true,
      "sAjaxSource" : permListUrl,
      "sAjaxDataProp" : "items",
      "aoColumns" : [
         { "mDataProp" : "name" },
       ],
    });
    
    aclUserListTable = $(userListId);
  }
}
