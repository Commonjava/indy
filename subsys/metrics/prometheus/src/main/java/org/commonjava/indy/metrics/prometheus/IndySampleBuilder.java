package org.commonjava.indy.metrics.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.dropwizard.samplebuilder.DefaultSampleBuilder;

import java.util.ArrayList;
import java.util.List;

public class IndySampleBuilder
        extends DefaultSampleBuilder
{
    private static final String NODE_NAME_LABEL = "node";

    private String nodeName;

    public IndySampleBuilder( String nodeName )
    {
        super();
        this.nodeName = nodeName;
    }

    @Override
    public Collector.MetricFamilySamples.Sample createSample( final String dropwizardName, final String nameSuffix,
                                                              final List<String> additionalLabelNames,
                                                              final List<String> additionalLabelValues,
                                                              final double value )
    {
        List<String> labelNames = new ArrayList( additionalLabelNames );
        labelNames.add( NODE_NAME_LABEL );

        List<String> labelValues = new ArrayList( additionalLabelValues );
        labelValues.add( nodeName );

        return super.createSample( dropwizardName, nameSuffix, labelNames, labelValues, value );
    }
}
