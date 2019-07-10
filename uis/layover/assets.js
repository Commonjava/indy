const CSS = [
  'node_modules/html5-boilerplate/dist/css/normalize.css',
  'node_modules/html5-boilerplate/dist/css/main.css',
  'node_modules/ng-dialog/css/ngDialog.css',
  'node_modules/ng-dialog/css/ngDialog-theme-default.css',
  'node_modules/bootstrap/dist/css/bootstrap.min.css',
  'node_modules/bootstrap/dist/css/bootstrap-theme.min.css',  
  {"from":"app/css/", "to":"css/"}
];
const JS = [
  'node_modules/angular/angular.min.js',
  'node_modules/angular-route/angular-route.min.js',
  'node_modules/html5-boilerplate/dist/js/vendor/modernizr-2.8.3.min.js',
  'node_modules/angular-resource/angular-resource.min.js',
  'node_modules/ng-dialog/js/ngDialog.min.js',
  'node_modules/jquery/dist/jquery.min.js',
  'node_modules/bootstrap/dist/js/bootstrap.min.js',  
  {"from":"app/js/", "to":"js/"}
];
const IMG=[  
  {"from":"app/img/", "to":"img/"}
];
const SWAGGER=[
  'node_modules/swagger-ui/dist/'
];
const OTHER=[
  {"from":"app/index.html", "to":"index.html"},
  {"from":"app/rest-api.html", "to":"rest-api.html"},
  {"from":"app/partials/", "to":"partials/"},
  {"from":"app/keycloak.json", "to":"keycloak.json"}
]

module.exports = [...JS, ...CSS, ...IMG, ...SWAGGER, ...OTHER];