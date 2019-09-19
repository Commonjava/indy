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
package org.commonjava.indy.metrics.socket;

import org.commonjava.indy.metrics.zabbix.sender.SenderResult;

/**
 * Created by xiabai on 5/12/17.
 */
public class ZabbixResult
{
    private String info;

    private SenderResult result;

    public ZabbixResult( SenderResult senderResult )
    {
        this.result = senderResult;
    }

    public String getInfo()
    {
        return toString();
    }

    public void setInfo( String info )
    {
        this.info = info;
    }

    @Override
    public String toString()
    {
        return "," + result.getProcessed() + "," + result.getFailed() + "," + result.getTotal() + ","
                        + result.getSpentSeconds();
    }
}
