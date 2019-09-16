/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.sli.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.health.HealthCheck;
import org.commonjava.indy.metrics.healthcheck.IndyCompoundHealthCheck;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class GoldenSignalsMetricSet
        implements MetricSet, IndyCompoundHealthCheck
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

    public static final String FN_MAVEN_UPLOAD = "maven.upload";

    public static final String FN_MAVEN_DOWNLOAD = "maven.download";

    public static final String FN_NPM_UPLOAD = "npm.upload";

    public static final String FN_NPM_DOWNLOAD = "npm.download";

    private static final String[] FUNCTIONS = {
            FN_CONTENT, FN_CONTENT_MAVEN, FN_CONTENT_NPM, FN_CONTENT_GENERIC,
            FN_METADATA, FN_METADATA_MAVEN, FN_METADATA_NPM,
            FN_PROMOTION, FN_TRACKING_RECORD, FN_CONTENT_LISTING, FN_REPO_MGMT,
            FN_MAVEN_UPLOAD, FN_MAVEN_DOWNLOAD, FN_NPM_UPLOAD, FN_NPM_DOWNLOAD
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

    @Override
    public Map<String, HealthCheck> getHealthChecks()
    {
        Map<String, HealthCheck> checks = new HashMap<>();
        functionMetrics.forEach( (key,value)-> checks.put( "sli.golden." + key, value.getHealthCheck() ) );
        return checks;
    }
}
