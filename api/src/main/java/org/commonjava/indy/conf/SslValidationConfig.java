/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.conf;


import org.commonjava.propulsor.config.annotation.ConfigName;
import org.commonjava.propulsor.config.annotation.SectionName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ApplicationScoped
@SectionName( SslValidationConfig.SECTION_NAME )
public class SslValidationConfig implements IndyConfigInfo{

    public final static String SECTION_NAME = "ssl";

    private Boolean sslRequired;

    private List<String> remoteNoSSLHosts;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public SslValidationConfig() {
    }

    @ConfigName(value = "remote.ssl.required")
    public void setSslRequired(Boolean sslEnabled)
    {
        this.sslRequired = sslEnabled;
    }

    public boolean isSSLRequired()
    {
        return this.sslRequired == null ? Boolean.FALSE : this.sslRequired;
    }

    @ConfigName(value = "remote.nossl.hosts")
    public void setRemoteNoSSLHosts(String hosts)
    {
        String[] arrayNSSLHosts = hosts.split(",");
        this.remoteNoSSLHosts = new ArrayList<>();
        this.remoteNoSSLHosts.addAll(Arrays.asList(arrayNSSLHosts));
    }

    public List<String> getRemoteNoSSLHosts()
    {
        return this.remoteNoSSLHosts == null ? new ArrayList<>() : this.remoteNoSSLHosts;
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return new File( IndyConfigInfo.CONF_INCLUDES_DIR, "ssl.conf" ).getPath();
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream( "default-main.conf" );
    }
}
