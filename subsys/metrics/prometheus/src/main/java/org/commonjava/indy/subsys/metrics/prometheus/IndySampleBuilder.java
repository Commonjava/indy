/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.subsys.metrics.prometheus;

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
