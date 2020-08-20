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
package org.commonjava.indy.subsys.honeycomb.config;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.o11yphant.honeycomb.config.HoneycombConfiguration;
import org.commonjava.propulsor.config.ConfigurationException;
import org.commonjava.propulsor.config.annotation.SectionName;
import org.commonjava.propulsor.config.section.MapSectionListener;

import javax.enterprise.context.ApplicationScoped;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SectionName( "honeycomb" )
@ApplicationScoped
public class IndyHoneycombConfiguration
                extends MapSectionListener
                implements IndyConfigInfo, HoneycombConfiguration
{
    private static final String ENABLED = "enabled";

    private static final String WRITE_KEY = "write.key";

    private static final String DATASET = "dataset";

    private static final String FIELDS = "fields";

    private static final String BASE_SAMPLE_RATE = "base.sample.rate";

    private static final String SAMPLE_PREFIX = "sample.";

    private static final String ENVIRONMENT_MAPPINGS = "environment.mappings";

    private static final Integer DEFAULT_BASE_SAMPLE_RATE = 100;

    private boolean enabled;

    private String writeKey;

    private String dataset;

    private Integer baseSampleRate;

    private Map<String, Integer> spanRates = new HashMap<>();

    private Set<String> spansIncluded = Collections.emptySet();

    private Set<String> spansExcluded = Collections.emptySet();

    private Set<String> fields;

    private String environmentMappings;

    public IndyHoneycombConfiguration()
    {
    }

    @Override
    public Map<String, Integer> getSpanRates()
    {
        return spanRates;
    }

    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    @Override
    public String getServiceName()
    {
        return "indy";
    }

    @Override
    public void sectionStarted( final String name ) throws ConfigurationException
    {
        // NOP; just block map init in the underlying implementation.
    }

    @Override
    public void parameter( final String name, final String value ) throws ConfigurationException
    {
        switch ( name )
        {
            case ENABLED:
                this.enabled = Boolean.TRUE.equals( Boolean.parseBoolean( value.trim() ) );
                break;
            case WRITE_KEY:
                this.writeKey = value.trim();
                break;
            case DATASET:
                this.dataset = value.trim();
                break;
            case BASE_SAMPLE_RATE:
                this.baseSampleRate = Integer.parseInt( value.trim() );
                break;
            case ENVIRONMENT_MAPPINGS:
                this.environmentMappings = value.trim();
                break;
            case FIELDS:
                this.fields = Collections.unmodifiableSet(
                                new HashSet<>( Arrays.asList( value.trim().split( "\\s*,\\s*" ) ) ) );
                break;
            default:
                if ( name.startsWith( SAMPLE_PREFIX ) && name.length() > SAMPLE_PREFIX.length() )
                {
                    spanRates.put( name.substring( SAMPLE_PREFIX.length() ).trim(), Integer.parseInt( value ) );
                }
        }
    }

    @Override
    public String getWriteKey()
    {
        return writeKey;
    }

    @Override
    public String getDataset()
    {
        return dataset;
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return "honeycomb.conf";
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream( "default-honeycomb.conf" );
    }

    @Override
    public Integer getBaseSampleRate()
    {
        return baseSampleRate == null ? DEFAULT_BASE_SAMPLE_RATE : baseSampleRate;
    }

    @Override
    public Set<String> getFieldSet()
    {
        return fields == null ? DEFAULT_FIELDS : fields;
    }

    @Override
    public String getEnvironmentMappings()
    {
        return environmentMappings;
    }

}
