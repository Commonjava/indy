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
