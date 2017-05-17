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
