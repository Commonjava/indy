/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.client.core.metric;

import org.apache.commons.lang3.StringUtils;
import org.commonjava.indy.client.core.inject.ClientMetricConfig;
import org.commonjava.o11yphant.honeycomb.HoneycombConfiguration;
import org.commonjava.o11yphant.otel.OtelConfiguration;
import org.commonjava.o11yphant.trace.TracerConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@SuppressWarnings( "unused" )
@ClientMetricConfig
public class ClientTracerConfiguration
        implements TracerConfiguration, OtelConfiguration, HoneycombConfiguration
{
    private static final Integer DEFAULT_BASE_SAMPLE_RATE = 100;

    private static final String DEFAULT_INDY_CLIENT_SERVICE_NAME = "indy-client";

    private boolean enabled;

    private String serviceName;

    private boolean consoleTransport;

    private String writeKey;

    private String dataset;

    private Integer baseSampleRate;

    private final Map<String, Integer> spanRates = new HashMap<>();

    private Set<String> fields;

    private String environmentMappings;

    private String cpNames;

    private String grpcUri;

    private Map<String, String> grpcHeaders = new HashMap<>();

    private Map<String, String> grpcResources = new HashMap<>();

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
    public boolean isConsoleTransport()
    {
        return consoleTransport;
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

    @Override
    public String getCPNames()
    {
        return cpNames;
    }

    @Override
    public Map<String, String> getGrpcHeaders()
    {
        return grpcHeaders;
    }

    @Override
    public Map<String, String> getResources()
    {
        return grpcResources;
    }

    @Override
    public String getServiceName()
    {
        return StringUtils.isBlank( serviceName ) ? DEFAULT_INDY_CLIENT_SERVICE_NAME : serviceName;
    }

    @Override
    public String getNodeId()
    {
        return null;
    }

    @Override
    public String getGrpcEndpointUri()
    {
        return grpcUri == null ? DEFAULT_GRPC_URI : grpcUri;
    }

    public void setDataset( String dataset )
    {
        this.dataset = dataset;
    }

    public void setWriteKey( String writeKey )
    {
        this.writeKey = writeKey;
    }

    public void setConsoleTransport( boolean consoleTransport )
    {
        this.consoleTransport = consoleTransport;
    }

    public void setEnabled( boolean enabled )
    {
        this.enabled = enabled;
    }

    public void setBaseSampleRate( Integer baseSampleRate )
    {
        this.baseSampleRate = baseSampleRate;
    }

    public void setFields( Set<String> fields )
    {
        this.fields = fields;
    }

    public void setEnvironmentMappings( String environmentMappings )
    {
        this.environmentMappings = environmentMappings;
    }

    public void setCpNames( String cpNames )
    {
        this.cpNames = cpNames;
    }

    public void setGrpcUri( String grpcUri )
    {
        this.grpcUri = grpcUri;
    }

    public void setServiceName( String serviceName )
    {
        this.serviceName = serviceName;
    }

    public void setGrpcHeaders( Map<String, String> grpcHeaders )
    {
        this.grpcHeaders = grpcHeaders;
    }

    public void setGrpcResources( Map<String, String> grpcResources )
    {
        this.grpcResources = grpcResources;
    }
}
