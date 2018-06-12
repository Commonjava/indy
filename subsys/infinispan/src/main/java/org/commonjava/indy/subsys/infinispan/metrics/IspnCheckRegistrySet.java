package org.commonjava.indy.subsys.infinispan.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.codahale.metrics.MetricRegistry.name;

public class IspnCheckRegistrySet
                implements MetricSet
{
    public static final String INDY_METRIC_ISPN = "indy.ispn";

    private static final String SIZE = "size";

    private static final String CURRENT_NUMBER_OF_ENTRIES = "CurrentNumberOfEntries";

    private static final String CURRENT_NUMBER_OF_ENTRIES_IN_MEMORY = "CurrentNumberOfEntriesInMemory";

    private static final String TOTAL_NUMBER_OF_ENTRIES = "TotalNumberOfEntries";

    private static final String HITS = "Hits";

    private static final String MISSES = "Misses";

    private static final String RETRIEVALS = "Retrievals";

    private static final String EVICTIONS = "Evictions";

    private EmbeddedCacheManager cacheManager;

    private List<String> ispnGauges;

    public IspnCheckRegistrySet( EmbeddedCacheManager cacheManager, List<String> ispnGauges )
    {
        this.cacheManager = cacheManager;
        this.ispnGauges = ispnGauges;
    }

    @Override
    public Map<String, Metric> getMetrics()
    {
        final Map<String, Metric> gauges = new HashMap<String, Metric>();
        Set<String> names = cacheManager.getCacheNames();
        names.forEach( n ->
           {
               Cache<Object, Object> cache = cacheManager.getCache( n );
               AdvancedCache<Object, Object> advancedCache = cache.getAdvancedCache();

               gauges.put( name( cache.getName(), SIZE ), (Gauge) () -> advancedCache.size() ); // default

               if ( ispnGauges == null || ispnGauges.contains( CURRENT_NUMBER_OF_ENTRIES ) )
               {
                   gauges.put( name( cache.getName(), CURRENT_NUMBER_OF_ENTRIES ),
                               (Gauge) () -> advancedCache.getStats().getCurrentNumberOfEntries() );
               }
               if ( ispnGauges == null || ispnGauges.contains( TOTAL_NUMBER_OF_ENTRIES ) )
               {
                   gauges.put( name( cache.getName(), TOTAL_NUMBER_OF_ENTRIES ),
                               (Gauge) () -> advancedCache.getStats().getTotalNumberOfEntries() );
               }
               if ( ispnGauges == null || ispnGauges.contains( HITS ) )
               {
                   gauges.put( name( cache.getName(), HITS ),
                               (Gauge) () -> advancedCache.getStats().getHits() );
               }
               if ( ispnGauges == null || ispnGauges.contains( MISSES ) )
               {
                   gauges.put( name( cache.getName(), MISSES ),
                               (Gauge) () -> advancedCache.getStats().getMisses() );
               }
               if ( ispnGauges == null || ispnGauges.contains( RETRIEVALS ) )
               {
                   gauges.put( name( cache.getName(), RETRIEVALS ),
                               (Gauge) () -> advancedCache.getStats().getRetrievals() );
               }
               if ( ispnGauges == null || ispnGauges.contains( EVICTIONS ) )
               {
                   gauges.put( name( cache.getName(), EVICTIONS ),
                               (Gauge) () -> advancedCache.getStats().getEvictions() );
               }
           } );
        return gauges;
    }
}
