/*
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict';

/* Filters */

var filterModule = angular.module('indy.filters', []);

filterModule
  .filter('checkmark', function() {
    return function(input) {
      return input ? '\u2713' : '\u2718';
    };
  });

filterModule
  .filter('duration', ['StoreUtilSvc', function(StoreUtilSvc) {
    return function(secs, useDefault) {
      return StoreUtilSvc.secondsToDuration( secs, useDefault );
    };
  }]);

filterModule
  .filter('dateFormat', ['StoreUtilSvc', function(StoreUtilSvc) {
    return function(secs) {
      return StoreUtilSvc.timestampToDateFormat( secs );
    };
  }]);

filterModule
  .filter('timestampToDuration', ['StoreUtilSvc', function(StoreUtilSvc) {
    return function(secs) {
      if ( secs == undefined ){
        return 'never';
      }

      if ( secs < 1 ){
        return 'never';
      }
       var nextDate =  new Date(secs);
       var toDay = new Date();
       var total = nextDate.getTime() - toDay.getTime();
       return StoreUtilSvc.secondsToDuration( total / 1000);
    };
  }]);
