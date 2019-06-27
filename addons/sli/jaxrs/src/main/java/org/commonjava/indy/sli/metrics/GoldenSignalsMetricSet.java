package org.commonjava.indy.sli.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class GoldenSignalsMetricSet
        implements MetricSet
{
    public static final String FN_CONTENT = "content";

    public static final String FN_CONTENT_MAVEN = "content.maven";

    public static final String FN_CONTENT_NPM = "content.npm";

    public static final String FN_CONTENT_GENERIC = "content.generic";

    public static final String FN_METADATA = "metadata";

    public static final String FN_METADATA_MAVEN = "metadata.maven";

    public static final String FN_METADATA_NPM = "metadata.npm";

    public static final String FN_PROMOTION = "promotion";

    public static final String FN_TRACKING_RECORD = "tracking.record";

    public static final String FN_CONTENT_LISTING = "content.listing";

    public static final String FN_REPO_MGMT = "repo.mgmt";

    private static final String[] FUNCTIONS = {
            FN_CONTENT, FN_CONTENT_MAVEN, FN_CONTENT_NPM, FN_CONTENT_GENERIC,
            FN_METADATA, FN_METADATA_MAVEN, FN_METADATA_NPM,
            FN_PROMOTION, FN_TRACKING_RECORD, FN_CONTENT_LISTING, FN_REPO_MGMT
    };

    private Map<String, GoldenSignalsFunctionMetrics> functionMetrics = new HashMap<>();

    public GoldenSignalsMetricSet()
    {
        Stream.of( FUNCTIONS )
              .forEach( function -> {
                  System.out.println( "Wiring SLI metrics for: " + function );
                  functionMetrics.put( function, new GoldenSignalsFunctionMetrics( function ) );
              } );
    }

    @Override
    public Map<String, Metric> getMetrics()
    {
        Map<String, Metric> metrics = new HashMap<>();
        functionMetrics.values().forEach( ms -> metrics.putAll( ms.getMetrics() ) );

        return metrics;
    }

    public Optional<GoldenSignalsFunctionMetrics> function( String name )
    {
        return functionMetrics.containsKey( name ) ? Optional.of( functionMetrics.get( name ) ) : Optional.empty();
    }
}
