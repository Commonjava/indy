/**
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
package org.commonjava.indy.metrics.zabbix.api;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public interface ZabbixApi
{

    void init();

    void destroy();

    String apiVersion() throws IOException;

    JsonNode call( Request request ) throws IOException;

    boolean login( String user, String password ) throws IOException;

    String hostCreate( String host, String groupId, String ip ) throws IOException;

    String hostgroupCreate( String name ) throws IOException;

    String createItem( String host, String item, String hostid, int valueType ) throws IOException;

    String getItem( String host, String item, String hostCache ) throws IOException;

    String getHost( String name ) throws IOException;

    String getHostgroup( String name ) throws IOException;
}
