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
import org.commonjava.propulsor.config.ConfigurationException;
import org.commonjava.propulsor.config.annotation.SectionName;
import org.commonjava.propulsor.config.section.MapSectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.commonjava.indy.metrics.RequestContextHelper.CLIENT_ADDR;
import static org.commonjava.indy.metrics.RequestContextHelper.CONTENT_TRACKING_ID;
import static org.commonjava.indy.metrics.RequestContextHelper.HTTP_METHOD;
import static org.commonjava.indy.metrics.RequestContextHelper.HTTP_STATUS;
import static org.commonjava.indy.metrics.RequestContextHelper.PACKAGE_TYPE;
import static org.commonjava.indy.metrics.RequestContextHelper.PATH;
import static org.commonjava.indy.metrics.RequestContextHelper.TRACE_ID;
import static org.commonjava.indy.metrics.RequestContextHelper.REQUEST_LATENCY_MILLIS;
import static org.commonjava.indy.metrics.RequestContextHelper.REST_ENDPOINT_PATH;

@SectionName( "honeycomb" )
@ApplicationScoped
public class HoneycombConfiguration
        extends MapSectionListener
        implements IndyConfigInfo
{
    private static final Set<String> DEFAULT_FIELDS = Collections.unmodifiableSet( new HashSet<>(
            Arrays.asList( CONTENT_TRACKING_ID, HTTP_METHOD, HTTP_STATUS, TRACE_ID, CLIENT_ADDR, PATH, PACKAGE_TYPE,
                           REST_ENDPOINT_PATH, REQUEST_LATENCY_MILLIS ) ) );

    private static final String ENABLED = "enabled";

    private static final String WRITE_KEY = "write.key";

    private static final String DATASET = "dataset";

    private static final String FIELDS = "fields";

    private static final String BASE_SAMPLE_RATE = "base.sample.rate";

    private static final String SAMPLE_PREFIX = "sample.";

    private static final Integer DEFAULT_BASE_SAMPLE_RATE = 100;

    private boolean enabled;

    private String writeKey;

    private String dataset;

    private Integer baseSampleRate;

    private Map<String, Integer> spanRates = new HashMap<>();

    private Set<String> spansIncluded = Collections.emptySet();

    private Set<String> spansExcluded = Collections.emptySet();

    private Set<String> fields;

    public HoneycombConfiguration()
    {
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    @Override
    public void sectionStarted( final String name )
            throws ConfigurationException
    {
        // NOP; just block map init in the underlying implementation.
    }

    @Override
    public void parameter( final String name, final String value )
            throws ConfigurationException
    {
        switch(name)
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

    public String getWriteKey()
    {
        return writeKey;
    }

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

    public Integer getBaseSampleRate()
    {
        return baseSampleRate == null ? DEFAULT_BASE_SAMPLE_RATE : baseSampleRate;
    }

    public int getSampleRate( Method method )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        if ( !spanRates.isEmpty() )
        {
            String[] keys = {
                    method.getName(),
                    method.getDeclaringClass().getSimpleName() + "." + method.getName(),
                    method.getDeclaringClass().getName() + "." + method.getName(),
                    method.getDeclaringClass().getSimpleName(),
                    method.getDeclaringClass().getName()
            };

            for( String key: keys )
            {
                Integer rate = spanRates.get( key );
                if ( rate != null )
                {
                    logger.trace( "Found sampling rate for: {} = {}", key, rate );
                    return rate;
                }
            }
        }

        logger.trace( "Returning base sampling rate for: {} = {}", method, getBaseSampleRate() );
        return getBaseSampleRate();
    }

    public Set<String> getFieldSet()
    {
        return fields == null ? DEFAULT_FIELDS : fields;
    }

    public Integer getSampleRate( final String classifier )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        Integer rate = spanRates.get( classifier );
        if ( rate != null )
        {
            logger.trace( "Found sampling rate for: {} = {}", classifier, rate );
            return rate;
        }

        String[] parts = classifier.split( "\\." );
        StringBuilder sb = new StringBuilder();
        for ( int i = parts.length; i > 0; i-- )
        {
            sb.setLength( 0 );
            for ( int j = 0; j < i; j++ )
            {
                if ( sb.length() > 0 )
                {
                    sb.append( '.' );
                }
                sb.append( parts[j] );
            }

            rate = spanRates.get( sb.toString() );
            if ( rate != null )
            {
                logger.trace( "Found sampling rate for: {} = {}", sb, rate );
                return rate;
            }
        }

        logger.trace( "Returning base sampling rate for: {} = {}", classifier, getBaseSampleRate() );
        return getBaseSampleRate();
    }
}
