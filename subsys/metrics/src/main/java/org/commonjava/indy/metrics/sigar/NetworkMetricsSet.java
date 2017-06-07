
package org.commonjava.indy.metrics.sigar;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.NetStat;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.Tcp;

import java.util.HashMap;
import java.util.Map;

public class NetworkMetricsSet
                implements MetricSet
{
    public Map<String, Metric> getMetrics()
    {
        final Map<String, Metric> gauges = new HashMap<String, Metric>();
        Sigar sigar = SigarService.getSigar();

        try
        {
            String[] interfaces = sigar.getNetInterfaceList();
            for ( String iface : interfaces )
            {
                createGaugesForNetInterface( gauges, iface );
            }
        }
        catch ( SigarException e )
        {
        }
        try
        {
            createTcpStatGauges( gauges );
        }
        catch ( SigarException e )
        {
        }
        try
        {
            createNetStatGauges( gauges );
        }
        catch ( SigarException e )
        {
        }
        return gauges;
    }

    private void createNetStatGauges( Map<String, Metric> gauges ) throws SigarException
    {
        gauges.put( "net.netstat.allInboundTotal", new Gauge<Integer>()
        {
            NetStat sigarStat = null;

            @Override
            public Integer getValue()
            {
                sigarStat = SigarService.getNetStat( sigarStat );
                return sigarStat.getAllInboundTotal();
            }
        } );

        gauges.put( "net.netstat.allOutboundTotal", new Gauge<Integer>()
        {
            NetStat sigarStat = null;

            @Override
            public Integer getValue()
            {
                sigarStat = SigarService.getNetStat( sigarStat );
                return sigarStat.getAllOutboundTotal();
            }
        } );

        gauges.put( "net.netstat.tcpBound", new Gauge<Integer>()
        {
            NetStat sigarStat = null;

            @Override
            public Integer getValue()
            {
                sigarStat = SigarService.getNetStat( sigarStat );
                return sigarStat.getTcpBound();
            }
        } );

        gauges.put( "net.netstat.tcpClose", new Gauge<Integer>()
        {
            NetStat sigarStat = null;

            @Override
            public Integer getValue()
            {
                sigarStat = SigarService.getNetStat( sigarStat );
                return sigarStat.getTcpClose();
            }
        } );

        gauges.put( "net.netstat.tcpCloseWait", new Gauge<Integer>()
        {
            NetStat sigarStat = null;

            @Override
            public Integer getValue()
            {
                sigarStat = SigarService.getNetStat( sigarStat );
                return sigarStat.getTcpCloseWait();
            }
        } );

        gauges.put( "net.netstat.tcpClosing", new Gauge<Integer>()
        {
            NetStat sigarStat = null;

            @Override
            public Integer getValue()
            {
                sigarStat = SigarService.getNetStat( sigarStat );
                return sigarStat.getTcpClosing();
            }
        } );

        gauges.put( "net.netstat.tcpEstablished", new Gauge<Integer>()
        {
            NetStat sigarStat = null;

            @Override
            public Integer getValue()
            {
                sigarStat = SigarService.getNetStat( sigarStat );
                return sigarStat.getTcpEstablished();
            }
        } );

        gauges.put( "net.netstat.tcpFinWait1", new Gauge<Integer>()
        {
            NetStat sigarStat = null;

            @Override
            public Integer getValue()
            {
                sigarStat = SigarService.getNetStat( sigarStat );
                return sigarStat.getTcpFinWait1();
            }
        } );

        gauges.put( "net.netstat.tcpFinWait2", new Gauge<Integer>()
        {
            NetStat sigarStat = null;

            @Override
            public Integer getValue()
            {
                sigarStat = SigarService.getNetStat( sigarStat );
                return sigarStat.getTcpFinWait2();
            }
        } );

        gauges.put( "net.netstat.tcpIdle", new Gauge<Integer>()
        {
            NetStat sigarStat = null;

            @Override
            public Integer getValue()
            {
                sigarStat = SigarService.getNetStat( sigarStat );
                return sigarStat.getTcpIdle();
            }
        } );

        gauges.put( "net.netstat.tcpInboundTotal", new Gauge<Integer>()
        {
            NetStat sigarStat = null;

            @Override
            public Integer getValue()
            {
                sigarStat = SigarService.getNetStat( sigarStat );
                return sigarStat.getTcpInboundTotal();
            }
        } );

        gauges.put( "net.netstat.tcpLastAck", new Gauge<Integer>()
        {
            NetStat sigarStat = null;

            @Override
            public Integer getValue()
            {
                sigarStat = SigarService.getNetStat( sigarStat );
                return sigarStat.getTcpLastAck();
            }
        } );

        gauges.put( "net.netstat.tcpListen", new Gauge<Integer>()
        {
            NetStat sigarStat = null;

            @Override
            public Integer getValue()
            {
                sigarStat = SigarService.getNetStat( sigarStat );
                return sigarStat.getTcpListen();
            }
        } );

        gauges.put( "net.netstat.tcpOutboundTotal", new Gauge<Integer>()
        {
            NetStat sigarStat = null;

            @Override
            public Integer getValue()
            {
                sigarStat = SigarService.getNetStat( sigarStat );
                return sigarStat.getTcpOutboundTotal();
            }
        } );

//        gauges.put( "net.netstat.tcpStates", new Gauge<int[]>()
//        {
//            NetStat sigarStat = null;
//
//            @Override
//            public int[] getValue()
//            {
//                sigarStat = SigarService.getNetStat( sigarStat );
//                return sigarStat.getTcpStates();
//            }
//        } );

        gauges.put( "net.netstat.tcpSynRecv", new Gauge<Integer>()
        {
            NetStat sigarStat = null;

            @Override
            public Integer getValue()
            {
                sigarStat = SigarService.getNetStat( sigarStat );
                return sigarStat.getTcpSynRecv();
            }
        } );

        gauges.put( "net.netstat.tcpSynSent", new Gauge<Integer>()
        {
            NetStat sigarStat = null;

            @Override
            public Integer getValue()
            {
                sigarStat = SigarService.getNetStat( sigarStat );
                return sigarStat.getTcpSynSent();
            }
        } );

        gauges.put( "net.netstat.tcpTimeWait", new Gauge<Integer>()
        {
            NetStat sigarStat = null;

            @Override
            public Integer getValue()
            {
                sigarStat = SigarService.getNetStat( sigarStat );
                return sigarStat.getTcpTimeWait();
            }
        } );
    }

    private void createTcpStatGauges( Map<String, Metric> gauges ) throws SigarException
    {
        gauges.put( "net.tcp.activeOpens", new Gauge<Long>()
        {
            Tcp sigarStat = null;

            @Override
            public Long getValue()
            {
                sigarStat = SigarService.getTcp( sigarStat );
                return sigarStat.getActiveOpens();
            }
        } );

        gauges.put( "net.tcp.attemptFails", new Gauge<Long>()
        {
            Tcp sigarStat = null;

            @Override
            public Long getValue()
            {
                sigarStat = SigarService.getTcp( sigarStat );
                return sigarStat.getAttemptFails();
            }
        } );

        gauges.put( "net.tcp.currEstab", new Gauge<Long>()
        {
            Tcp sigarStat = null;

            @Override
            public Long getValue()
            {
                sigarStat = SigarService.getTcp( sigarStat );
                return sigarStat.getCurrEstab();
            }
        } );

        gauges.put( "net.tcp.estabResets", new Gauge<Long>()
        {
            Tcp sigarStat = null;

            @Override
            public Long getValue()
            {
                sigarStat = SigarService.getTcp( sigarStat );
                return sigarStat.getEstabResets();
            }
        } );

        gauges.put( "net.tcp.inErrs", new Gauge<Long>()
        {
            Tcp sigarStat = null;

            @Override
            public Long getValue()
            {
                sigarStat = SigarService.getTcp( sigarStat );
                return sigarStat.getInErrs();
            }
        } );

        gauges.put( "net.tcp.inSegs", new Gauge<Long>()
        {
            Tcp sigarStat = null;

            @Override
            public Long getValue()
            {
                sigarStat = SigarService.getTcp( sigarStat );
                return sigarStat.getInSegs();
            }
        } );

        gauges.put( "net.tcp.outRsts", new Gauge<Long>()
        {
            Tcp sigarStat = null;

            @Override
            public Long getValue()
            {
                sigarStat = SigarService.getTcp( sigarStat );
                return sigarStat.getOutRsts();
            }
        } );

        gauges.put( "net.tcp.outSegs", new Gauge<Long>()
        {
            Tcp sigarStat = null;

            @Override
            public Long getValue()
            {
                sigarStat = SigarService.getTcp( sigarStat );
                return sigarStat.getOutSegs();
            }
        } );

        gauges.put( "net.tcp.passiveOpens", new Gauge<Long>()
        {
            Tcp sigarStat = null;

            @Override
            public Long getValue()
            {
                sigarStat = SigarService.getTcp( sigarStat );
                return sigarStat.getPassiveOpens();
            }
        } );

        gauges.put( "net.tcp.retransSegs", new Gauge<Long>()
        {
            Tcp sigarStat = null;

            @Override
            public Long getValue()
            {
                sigarStat = SigarService.getTcp( sigarStat );
                return sigarStat.getRetransSegs();
            }
        } );
    }

    private void createGaugesForNetInterface( Map<String, Metric> gauges, final String iface ) throws SigarException
    {

        gauges.put( "net.ifaces." + iface + ".txCollisions", new Gauge<Long>()
        {
            NetInterfaceStat sigarStat = null;

            @Override
            public Long getValue()
            {
                sigarStat = SigarService.getNetInterfaceStat( iface, sigarStat );
                return sigarStat.getTxCollisions();
            }
        } );

        gauges.put( "net.ifaces." + iface + ".txCarrier", new Gauge<Long>()
        {
            NetInterfaceStat sigarStat = null;

            @Override
            public Long getValue()
            {
                sigarStat = SigarService.getNetInterfaceStat( iface, sigarStat );
                return sigarStat.getTxCarrier();
            }
        } );

        gauges.put( "net.ifaces." + iface + ".txBytes", new Gauge<Long>()
        {
            NetInterfaceStat sigarStat = null;

            @Override
            public Long getValue()
            {
                sigarStat = SigarService.getNetInterfaceStat( iface, sigarStat );
                return sigarStat.getTxBytes();
            }
        } );

        gauges.put( "net.ifaces." + iface + ".txDropped", new Gauge<Long>()
        {
            NetInterfaceStat sigarStat = null;

            @Override
            public Long getValue()
            {
                sigarStat = SigarService.getNetInterfaceStat( iface, sigarStat );
                return sigarStat.getTxDropped();
            }
        } );

        gauges.put( "net.ifaces." + iface + ".txErrors", new Gauge<Long>()
        {
            NetInterfaceStat sigarStat = null;

            @Override
            public Long getValue()
            {
                sigarStat = SigarService.getNetInterfaceStat( iface, sigarStat );
                return sigarStat.getTxErrors();
            }
        } );

        gauges.put( "net.ifaces." + iface + ".txOverruns", new Gauge<Long>()
        {
            NetInterfaceStat sigarStat = null;

            @Override
            public Long getValue()
            {
                sigarStat = SigarService.getNetInterfaceStat( iface, sigarStat );
                return sigarStat.getTxOverruns();
            }
        } );

        gauges.put( "net.ifaces." + iface + ".txPackets", new Gauge<Long>()
        {
            NetInterfaceStat sigarStat = null;

            @Override
            public Long getValue()
            {
                sigarStat = SigarService.getNetInterfaceStat( iface, sigarStat );
                return sigarStat.getTxPackets();
            }
        } );

        gauges.put( "net.ifaces." + iface + ".rxFrame", new Gauge<Long>()
        {
            NetInterfaceStat sigarStat = null;

            @Override
            public Long getValue()
            {
                sigarStat = SigarService.getNetInterfaceStat( iface, sigarStat );
                return sigarStat.getRxFrame();
            }
        } );

        gauges.put( "net.ifaces." + iface + ".rxBytes", new Gauge<Long>()
        {
            NetInterfaceStat sigarStat = null;

            @Override
            public Long getValue()
            {
                sigarStat = SigarService.getNetInterfaceStat( iface, sigarStat );
                return sigarStat.getRxBytes();
            }
        } );

        gauges.put( "net.ifaces." + iface + ".rxDropped", new Gauge<Long>()
        {
            NetInterfaceStat sigarStat = null;

            @Override
            public Long getValue()
            {
                sigarStat = SigarService.getNetInterfaceStat( iface, sigarStat );
                return sigarStat.getRxDropped();
            }
        } );

        gauges.put( "net.ifaces." + iface + ".rxErrors", new Gauge<Long>()
        {
            NetInterfaceStat sigarStat = null;

            @Override
            public Long getValue()
            {
                sigarStat = SigarService.getNetInterfaceStat( iface, sigarStat );
                return sigarStat.getRxErrors();
            }
        } );

        gauges.put( "net.ifaces." + iface + ".rxOverruns", new Gauge<Long>()
        {
            NetInterfaceStat sigarStat = null;

            @Override
            public Long getValue()
            {
                sigarStat = SigarService.getNetInterfaceStat( iface, sigarStat );
                return sigarStat.getRxOverruns();
            }
        } );

        gauges.put( "net.ifaces." + iface + ".rxPackets", new Gauge<Long>()
        {
            NetInterfaceStat sigarStat = null;

            @Override
            public Long getValue()
            {
                sigarStat = SigarService.getNetInterfaceStat( iface, sigarStat );
                return sigarStat.getRxPackets();
            }
        } );
    }
}
