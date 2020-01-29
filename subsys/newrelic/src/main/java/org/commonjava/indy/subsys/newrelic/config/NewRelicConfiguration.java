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
package org.commonjava.indy.subsys.newrelic.config;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.propulsor.config.annotation.ConfigName;
import org.commonjava.propulsor.config.annotation.SectionName;

import javax.enterprise.context.ApplicationScoped;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.commonjava.indy.metrics.RequestContextHelper.CLIENT_ADDR;
import static org.commonjava.indy.metrics.RequestContextHelper.CONTENT_TRACKING_ID;
import static org.commonjava.indy.metrics.RequestContextHelper.HTTP_METHOD;
import static org.commonjava.indy.metrics.RequestContextHelper.HTTP_STATUS;
import static org.commonjava.indy.metrics.RequestContextHelper.PACKAGE_TYPE;
import static org.commonjava.indy.metrics.RequestContextHelper.PATH;
import static org.commonjava.indy.metrics.RequestContextHelper.PREFERRED_ID;
import static org.commonjava.indy.metrics.RequestContextHelper.REQUEST_LATENCY_MILLIS;
import static org.commonjava.indy.metrics.RequestContextHelper.REST_ENDPOINT_PATH;

@SectionName( "newrelic" )
@ApplicationScoped
public class NewRelicConfiguration
                implements IndyConfigInfo
{
    private static final String[] FIELDS =
            { CONTENT_TRACKING_ID, HTTP_METHOD, HTTP_STATUS, PREFERRED_ID, CLIENT_ADDR, PATH, PACKAGE_TYPE, REST_ENDPOINT_PATH, REQUEST_LATENCY_MILLIS };

    private boolean enabled;

    private String insertKey;

    private Set<String> spansIncluded = Collections.emptySet();

    private Set<String> spansExcluded = Collections.emptySet();

    public boolean isEnabled()
    {
        return enabled;
    }

    @ConfigName( "enabled" )
    public void setEnabled( boolean enabled )
    {
        this.enabled = enabled;
    }

    public String getInsertKey()
    {
        return insertKey;
    }

    @ConfigName( "insights.insert.key" )
    public void setInsertKey( String insertKey )
    {
        this.insertKey = insertKey;
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return "newrelic.conf";
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream( "default-newrelic.conf" );
    }

    @ConfigName( "spans.include" )
    public void setSpansIncluded( final String spans )
    {
        this.spansIncluded = new HashSet<>( Arrays.asList( spans.split( "\\s*,\\s*" ) ) );
    }

    public Set<String> getSpansIncluded()
    {
        return spansIncluded;
    }

    @ConfigName( "spans.exclude" )
    public void setSpansExcluded( final String spans )
    {
        this.spansExcluded = new HashSet<>( Arrays.asList( spans.split( "\\s*,\\s*" ) ) );
    }

    public Set<String> getSpansExcluded()
    {
        return spansExcluded;
    }

    public boolean isSpanIncluded( Method method )
    {
        /* @formatter:off */
        if ( !spansIncluded.isEmpty() )
        {
            boolean included = spansIncluded.contains( method.getName() ) ||
                                spansIncluded.contains( method.getDeclaringClass().getSimpleName() + "." + method.getName() ) ||
                                spansIncluded.contains( method.getDeclaringClass().getName() + "." + method.getName() ) ||
                                spansIncluded.contains( method.getDeclaringClass().getSimpleName() ) ||
                                spansIncluded.contains( method.getDeclaringClass().getName() );
            return included;
        }

        if ( !spansExcluded.isEmpty() )
        {
            boolean excluded = spansExcluded.contains( method.getName() ) ||
                                spansExcluded.contains( method.getDeclaringClass().getSimpleName() + "." + method.getName() ) ||
                                spansExcluded.contains( method.getDeclaringClass().getName() + "." + method.getName() ) ||
                                spansExcluded.contains( method.getDeclaringClass().getSimpleName() ) ||
                                spansExcluded.contains( method.getDeclaringClass().getName() );
            return !excluded;
        }
        /* @formatter:on */

        return true;
    }

    public String[] getFields()
    {
        return FIELDS;
    }
}
