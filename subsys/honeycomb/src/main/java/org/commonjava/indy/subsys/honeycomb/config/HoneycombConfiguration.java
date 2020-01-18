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
import org.commonjava.propulsor.config.annotation.ConfigName;
import org.commonjava.propulsor.config.annotation.SectionName;

import javax.enterprise.context.ApplicationScoped;
import java.io.InputStream;

@SectionName( "honeycomb" )
@ApplicationScoped
public class HoneycombConfiguration
        implements IndyConfigInfo
{

    private final String WRITE_KEY = "write_key";

    private final String DATASET = "dataset";

    private final String DEFAULT_DATASET = "indy_dataset";

    private String writeKey;

    private String dataset;

    public HoneycombConfiguration()
    {
    }

    public String getWriteKey()
    {
        return writeKey;
    }

    @ConfigName( WRITE_KEY )
    public void setWriteKey( String writeKey )
    {
        this.writeKey = writeKey;
    }

    public String getDataset()
    {
        return dataset == null ? DEFAULT_DATASET : dataset;
    }

    @ConfigName( DATASET )
    public void setDataset( String dataset )
    {
        this.dataset = dataset;
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
}
