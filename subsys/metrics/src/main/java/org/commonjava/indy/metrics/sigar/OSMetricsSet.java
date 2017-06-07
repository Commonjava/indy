
package org.commonjava.indy.metrics.sigar;


import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.Swap;

import java.util.HashMap;
import java.util.Map;

public class OSMetricsSet implements MetricSet
{
    @Override
    public Map<String, Metric> getMetrics() {
        final Map<String, Metric> gauges = new HashMap<String, Metric>();
        Sigar sigar = SigarService.getSigar();
        getMemMetrics(gauges);
        getSwapMetrics(gauges);
        getCpuTimeMetrics(gauges, sigar);
        return gauges;
    }

    private void getCpuTimeMetrics( Map<String, Metric> gauges, Sigar sigar) {
        try {
            Cpu[] cpus = sigar.getCpuList();
            for (int i = 0; i < cpus.length; i++) {
                final int idx = i;

                gauges.put("os.cpu." + String.valueOf(i) + ".total", new Gauge<Long>() {
                    Cpu sigarStat = null;
                    @Override
                    public Long getValue() {
                        sigarStat = SigarService.getCpuList(idx, sigarStat);
                        return sigarStat.getTotal();
                    }
                });

                gauges.put("os.cpu." + String.valueOf(i) + ".idle", new Gauge<Long>() {
                    Cpu sigarStat = null;
                    @Override
                    public Long getValue() {
                        sigarStat = SigarService.getCpuList(idx, sigarStat);
                        return sigarStat.getIdle();
                    }
                });

                gauges.put("os.cpu." + String.valueOf(i) + ".irq", new Gauge<Long>() {
                    Cpu sigarStat = null;
                    @Override
                    public Long getValue() {
                        sigarStat = SigarService.getCpuList(idx, sigarStat);
                        return sigarStat.getIrq();
                    }
                });

                gauges.put("os.cpu." + String.valueOf(i) + ".nice", new Gauge<Long>() {
                    Cpu sigarStat = null;
                    @Override
                    public Long getValue() {
                        sigarStat = SigarService.getCpuList(idx, sigarStat);
                        return sigarStat.getNice();
                    }
                });

                gauges.put("os.cpu." + String.valueOf(i) + ".softIrq", new Gauge<Long>() {
                    Cpu sigarStat = null;
                    @Override
                    public Long getValue() {
                        sigarStat = SigarService.getCpuList(idx, sigarStat);
                        return sigarStat.getSoftIrq();
                    }
                });

                gauges.put("os.cpu." + String.valueOf(i) + ".stolen", new Gauge<Long>() {
                    Cpu sigarStat = null;
                    @Override
                    public Long getValue() {
                        sigarStat = SigarService.getCpuList(idx, sigarStat);
                        return sigarStat.getStolen();
                    }
                });

                gauges.put("os.cpu." + String.valueOf(i) + ".sys", new Gauge<Long>() {
                    Cpu sigarStat = null;
                    @Override
                    public Long getValue() {
                        sigarStat = SigarService.getCpuList(idx, sigarStat);
                        return sigarStat.getSys();
                    }
                });

                gauges.put("os.cpu." + String.valueOf(i) + ".user", new Gauge<Long>() {
                    Cpu sigarStat = null;
                    @Override
                    public Long getValue() {
                        sigarStat = SigarService.getCpuList(idx, sigarStat);
                        return sigarStat.getUser();
                    }
                });

                gauges.put("os.cpu." + String.valueOf(i) + ".wait", new Gauge<Long>() {
                    Cpu sigarStat = null;
                    @Override
                    public Long getValue() {
                        sigarStat = SigarService.getCpuList(idx, sigarStat);
                        return sigarStat.getWait();
                    }
                });
            }
        } catch (SigarException e) {
            // nothing
        }
    }

    private void getSwapMetrics(Map<String, Metric> gauges) {
        gauges.put("os.swap.total", new Gauge<Long>() {
            Swap sigarStat = null;
            @Override
            public Long getValue() {
                sigarStat = SigarService.getSwap(sigarStat);
                return sigarStat.getTotal();
            }
        });

        gauges.put("os.swap.free", new Gauge<Long>() {
            Swap sigarStat = null;
            @Override
            public Long getValue() {
                sigarStat = SigarService.getSwap(sigarStat);
                return sigarStat.getFree();
            }
        });

        gauges.put("os.swap.used", new Gauge<Long>() {
            Swap sigarStat = null;
            @Override
            public Long getValue() {
                sigarStat = SigarService.getSwap(sigarStat);
                return sigarStat.getUsed();
            }
        });

        gauges.put("os.swap.pageIn", new Gauge<Long>() {
            Swap sigarStat = null;
            @Override
            public Long getValue() {
                sigarStat = SigarService.getSwap(sigarStat);
                return sigarStat.getPageIn();
            }
        });

        gauges.put("os.swap.pageOut", new Gauge<Long>() {
            Swap sigarStat = null;
            @Override
            public Long getValue() {
                sigarStat = SigarService.getSwap(sigarStat);
                return sigarStat.getPageOut();
            }
        });
    }

    private void getMemMetrics(Map<String, Metric> gauges) {
        gauges.put("os.mem.total", new Gauge<Long>() {
            Mem sigarStat = null;
            @Override
            public Long getValue() {
                sigarStat = SigarService.getMem(sigarStat);
                return sigarStat.getTotal();
            }
        });

        gauges.put("os.mem.free", new Gauge<Long>() {
            Mem sigarStat = null;
            @Override
            public Long getValue() {
                sigarStat = SigarService.getMem(sigarStat);
                return sigarStat.getFree();
            }
        });

        gauges.put("os.mem.used", new Gauge<Long>() {
            Mem sigarStat = null;
            @Override
            public Long getValue() {
                sigarStat = SigarService.getMem(sigarStat);
                return sigarStat.getUsed();
            }
        });

        gauges.put("os.mem.actualUsed", new Gauge<Long>() {
            Mem sigarStat = null;
            @Override
            public Long getValue() {
                sigarStat = SigarService.getMem(sigarStat);
                return sigarStat.getActualUsed();
            }
        });

        gauges.put("os.mem.actualFree", new Gauge<Long>() {
            Mem sigarStat = null;
            @Override
            public Long getValue() {
                sigarStat = SigarService.getMem(sigarStat);
                return sigarStat.getActualFree();
            }
        });
    }
}
