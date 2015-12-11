/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import angular from 'angular'

angular
.module('ignite-console.configuration.sidebar', [

])
.provider('igniteConfigurationSidebar', function() {
    var items = [
        { text: 'Clusters', sref: 'base.configuration.clusters' },
        { text: 'Caches', sref: 'base.configuration.caches' },
        { text: 'Metadata', sref: 'base.configuration.metadata' },
        { text: 'IGFS', sref: 'base.configuration.igfs' }
    ];

    this.push = function(data) {
        items.push(data);
    };

    this.$get = [function() {
        var r = angular.copy(items);

        r.push({ text: 'Summary', sref: 'base.configuration.summary' });

        return r;
    }]
})
.directive('igniteConfigurationSidebar', ['igniteConfigurationSidebar', function(igniteConfigurationSidebar) {
    function controller() {
        var ctrl = this;

        ctrl.items = igniteConfigurationSidebar;
    }

    return {
        restrict: 'A',
        controller: controller,
        controllerAs: 'sidebar'
    }
}]);
