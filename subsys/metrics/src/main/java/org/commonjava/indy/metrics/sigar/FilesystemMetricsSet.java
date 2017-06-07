
package org.commonjava.indy.metrics.sigar;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import java.util.HashMap;
import java.util.Map;

public class FilesystemMetricsSet implements MetricSet
{
    @Override
    public Map<String, Metric> getMetrics() {
        final Map<String, Metric> gauges = new HashMap<String, Metric>();
        Sigar sigar = SigarService.getSigar();
        try {
            FileSystem[] fileSystems = sigar.getFileSystemList();
            for (FileSystem fs : fileSystems) {
                if (fs.getType() != FileSystem.TYPE_LOCAL_DISK)
                    continue;
                createGaugesForFilesystem(gauges, fs);
            }
        } catch (SigarException e) {
            //
        }
        return gauges;
    }

    private void createGaugesForFilesystem( Map<String, Metric> gauges, FileSystem fs) throws SigarException {
        final String fsDirName = fs.getDirName();
        String fsDevName = fs.getDevName();
        String fsName = fsDevName.substring(fsDevName.lastIndexOf('/') + 1);

         gauges.put("fileSystem." + fsName + ".diskQueue", new Gauge<Double>() {
            FileSystemUsage sigarStat = null;
            @Override
            public Double getValue() {
                sigarStat = SigarService.getFileSystemUsage( fsDirName, sigarStat);
                return sigarStat.getDiskQueue();
            }
        });

        gauges.put("fileSystem." + fsName + ".diskReadBytes", new Gauge<Long>() {
            FileSystemUsage sigarStat = null;
            @Override
            public Long getValue() {
                sigarStat = SigarService.getFileSystemUsage( fsDirName, sigarStat);
                return sigarStat.getDiskReadBytes();
            }
        });

        gauges.put("fileSystem." + fsName + ".diskReads", new Gauge<Long>() {
            FileSystemUsage sigarStat = null;
            @Override
            public Long getValue() {
                sigarStat = SigarService.getFileSystemUsage( fsDirName, sigarStat);
                return sigarStat.getDiskReads();
            }
        });

        gauges.put("fileSystem." + fsName + ".diskServiceTime", new Gauge<Double>() {
            FileSystemUsage sigarStat = null;
            @Override
            public Double getValue() {
                sigarStat = SigarService.getFileSystemUsage( fsDirName, sigarStat);
                return sigarStat.getDiskServiceTime();
            }
        });

        gauges.put("fileSystem." + fsName + ".diskWriteBytes", new Gauge<Long>() {
            FileSystemUsage sigarStat = null;
            @Override
            public Long getValue() {
                sigarStat = SigarService.getFileSystemUsage( fsDirName, sigarStat);
                return sigarStat.getDiskWriteBytes();
            }
        });

        gauges.put("fileSystem." + fsName + ".diskWrites", new Gauge<Long>() {
            FileSystemUsage sigarStat = null;
            @Override
            public Long getValue() {
                sigarStat = SigarService.getFileSystemUsage( fsDirName, sigarStat);
                return sigarStat.getDiskWrites();
            }
        });

        gauges.put("fileSystem." + fsName + ".usedInodes", new Gauge<Long>() {
            FileSystemUsage sigarStat = null;
            @Override
            public Long getValue() {
                sigarStat = SigarService.getFileSystemUsage( fsDirName, sigarStat);
                return sigarStat.getFiles();
            }
        });

        gauges.put("fileSystem." + fsName + ".freeInodes", new Gauge<Long>() {
            FileSystemUsage sigarStat = null;
            @Override
            public Long getValue() {
                sigarStat = SigarService.getFileSystemUsage( fsDirName, sigarStat);
                return sigarStat.getFreeFiles();
            }
        });

        gauges.put("fileSystem." + fsName + ".free", new Gauge<Long>() {
            FileSystemUsage sigarStat = null;
            @Override
            public Long getValue() {
                sigarStat = SigarService.getFileSystemUsage( fsDirName, sigarStat);
                return sigarStat.getFree();
            }
        });

        gauges.put("fileSystem." + fsName + ".used", new Gauge<Long>() {
            FileSystemUsage sigarStat = null;
            @Override
            public Long getValue() {
                sigarStat = SigarService.getFileSystemUsage( fsDirName, sigarStat);
                return sigarStat.getUsed();
            }
        });

        gauges.put("fileSystem." + fsName + ".total", new Gauge<Long>() {
            FileSystemUsage sigarStat = null;
            @Override
            public Long getValue() {
                sigarStat = SigarService.getFileSystemUsage( fsDirName, sigarStat);
                return sigarStat.getTotal();
            }
        });
    }
}
