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
package org.commonjava.indy.metrics.zabbix.sender;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by xiabai on 4/7/17.
 * Copy from
 */
public class ZabbixSender
{
    private static final Pattern PATTERN = Pattern.compile( "[^0-9\\.]+");
    private static final Logger logger = LoggerFactory.getLogger( ZabbixSender.class );
    String host;
    int port;
    int connectTimeout = 3 * 1000;
    int socketTimeout = 3 * 1000;

    public ZabbixSender(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public ZabbixSender(String host, int port, int connectTimeout, int socketTimeout) {
        this(host, port);
        this.connectTimeout = connectTimeout;
        this.socketTimeout = socketTimeout;
    }

    public SenderResult send(DataObject dataObject) throws IOException
    {
        return send(dataObject, System.currentTimeMillis() / 1000);
    }

    /**
     *
     * @param dataObject
     * @param clock
     *            TimeUnit is SECONDS.
     * @return
     * @throws IOException
     */
    public SenderResult send(DataObject dataObject, long clock) throws IOException {
        return send( Collections.singletonList( dataObject), clock);
    }

    public SenderResult send(List<DataObject> dataObjectList) throws IOException {
        return send(dataObjectList, System.currentTimeMillis() / 1000);
    }

    /**
     *
     * @param dataObjectList
     * @param clock
     *            TimeUnit is SECONDS.
     * @return
     * @throws IOException
     */
    public SenderResult send(List<DataObject> dataObjectList, long clock) throws IOException {
        //TODO Need refacing the method using XNIO
        SenderResult senderResult = new SenderResult();

        Socket socket = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            socket = new Socket();

            socket.setSoTimeout(socketTimeout);
            socket.connect( new InetSocketAddress( host, port), connectTimeout);

            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            SenderRequest senderRequest = new SenderRequest();
            senderRequest.setData(dataObjectList);
            senderRequest.setClock(clock);

            outputStream.write(senderRequest.toBytes());

            outputStream.flush();

            // normal responseData.length < 100
            byte[] responseData = new byte[512];

            int readCount = 0;

            while (true) {
                int read = inputStream.read(responseData, readCount, responseData.length - readCount);
                if (read <= 0) {
                    break;
                }
                readCount += read;
            }

            if (readCount < 13) {
                // seems zabbix server return "[]"?
                senderResult.setbReturnEmptyArray(true);
            }

            // header('ZBXD\1') + len + 0
            // 5 + 4 + 4
            String jsonString = new String( responseData, 13, readCount - 13, Charset.forName( "UTF-8"));
            logger.info( "jsonString : " +jsonString );
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json =  mapper.readTree( jsonString );

//            JSONObject json = JSON.parseObject(jsonString);
            String info = json.get("info").asText();
            // example info: processed: 1; failed: 0; total: 1; seconds spent:
            // 0.000053
            // after split: [, 1, 0, 1, 0.000053]
            String[] split = PATTERN.split(info);
            senderResult.setProcessed(Integer.parseInt(split[1]));
            senderResult.setFailed(Integer.parseInt(split[2]));
            senderResult.setTotal(Integer.parseInt(split[3]));
            senderResult.setSpentSeconds(Float.parseFloat(split[4]));

        } finally {
            if (socket != null) {
                socket.close();
            }
            if (inputStream != null) {
                    inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        }

        return senderResult;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

}
