/*
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
    return function(secs) {
      return StoreUtilSvc.secondsToDuration( secs );
    };
  }]);
