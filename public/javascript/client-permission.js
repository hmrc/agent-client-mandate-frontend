function showHideClientPermissionFunc() {

  var selectedDiv = $('#hidden-identifiers');
  var submitButton = $('#continue');
  var viewClientsButton = $('#view-all-clients-div');
  var permissionFalse = $("#client-permission-false-hidden");
  var permissionTrue = $("#client-permission-true-hidden");

  submitButton.show();
  viewClientsButton.hide();
  permissionFalse.hide();
  permissionTrue.hide();

  $('input[type=radio][name=hasPermission]').change(function(){
    if(this.value == 'true') {
      submitButton.show();
      viewClientsButton.hide();
      permissionFalse.hide();
      permissionTrue.show();
    } else {
      submitButton.hide();
      viewClientsButton.show();
      permissionFalse.show();
      permissionTrue.hide();
    }
  });

}

$(document).ready(function() {
  showHideClientPermissionFunc();
});
