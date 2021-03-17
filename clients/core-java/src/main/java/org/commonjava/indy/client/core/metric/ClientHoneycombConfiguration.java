/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.client.core.metric;

import org.commonjava.indy.client.core.inject.ClientMetricConfig;
import org.commonjava.o11yphant.honeycomb.config.HoneycombConfiguration;

@ClientMetricConfig
public class ClientHoneycombConfiguration
        implements HoneycombConfiguration {

    private static final Integer DEFAULT_BASE_SAMPLE_RATE = 100;

    private boolean enabled;

    private String writeKey;

    private String dataset;

    private Integer baseSampleRate;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled( boolean enabled ) {
        this.enabled = enabled;
    }

    @Override
    public String getServiceName() {
        return "indy-client";
    }

    @Override
    public String getWriteKey() {
        return writeKey;
    }

    public void setWriteKey( String writeKey ) {
        this.writeKey = writeKey;
    }

    @Override
    public String getDataset() {
        return dataset;
    }

    public void setDataset( String dataset ) {
        this.dataset = dataset;
    }

    @Override
    public String getNodeId() {
        return null;
    }

    @Override
    public Integer getBaseSampleRate()
    {
        return baseSampleRate == null ? DEFAULT_BASE_SAMPLE_RATE : baseSampleRate;
    }

    public void setBaseSampleRate( Integer baseSampleRate ) {
        this.baseSampleRate = baseSampleRate;
    }
}
