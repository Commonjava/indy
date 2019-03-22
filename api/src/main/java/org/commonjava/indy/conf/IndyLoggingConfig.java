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
package org.commonjava.indy.conf;

import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;
import org.commonjava.web.config.section.MapSectionListener;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by yma on 2019/3/19.
 */

@ApplicationScoped
@SectionName( IndyLoggingConfig.SECTION_NAME )
public class IndyLoggingConfig
        extends MapSectionListener
        implements IndyConfigInfo
{

    public static final String SECTION_NAME = "logging";

    public static final String HOST_NAME_KEY = "HOSTNAME";

    public static final String BUILD_COMMIT_KEY = "OPENSHIFT_BUILD_COMMIT";

    public static final String BUILD_NAME_KEY = "OPENSHIFT_BUILD_NAME";

    public static final String BUILD_NAMESPACE_KEY = "OPENSHIFT_BUILD_NAMESPACE";

    public static final String SYSTEM_HOST_NAME = System.getenv( HOST_NAME_KEY );

    public static final String SYSTEM_BUILD_COMMIT = System.getenv( BUILD_COMMIT_KEY );

    public static final String SYSTEM_BUILD_NAME = System.getenv( BUILD_NAME_KEY );

    public static final String SYSTEM_BUILD_NAMESPACE = System.getenv( BUILD_NAMESPACE_KEY );

    private String hostname;

    private String openshiftBuildCommit;

    private String openshiftBuildName;

    private String openshiftBuildNamespace;

    public IndyLoggingConfig()
    {
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return new File( IndyConfigInfo.CONF_INCLUDES_DIR, "logging.conf" ).getPath();
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream( "default-logging.conf" );
    }

    @Override
    public synchronized void parameter( final String name, final String value )
            throws ConfigurationException
    {
        switch ( name )
        {
            case HOST_NAME_KEY:
            {
                this.hostname = value;
                break;
            }
            case BUILD_COMMIT_KEY:
            {
                this.openshiftBuildCommit = value;
                break;
            }
            case BUILD_NAME_KEY:
            {
                this.openshiftBuildName = value;
                break;
            }
            case BUILD_NAMESPACE_KEY:
            {
                this.openshiftBuildNamespace = value;
                break;
            }
            default:
            {
                throw new ConfigurationException(
                        "Invalid value: '{}' for parameter: '{}', they are not accepted for section: '{}'.", value,
                        name, SECTION_NAME );
            }

        }
    }

    public String getHostname()
    {
        return hostname == null ? SYSTEM_HOST_NAME : hostname;
    }

    @ConfigName( HOST_NAME_KEY )
    public void setHostname( String host )
    {
        this.hostname = host;
    }

    public String getOpenshiftBuildCommit()
    {
        return openshiftBuildCommit == null ? SYSTEM_BUILD_COMMIT : openshiftBuildCommit;
    }

    @ConfigName( BUILD_COMMIT_KEY )
    public void setOpenshiftBuildCommit( String buildCommit )
    {
        this.openshiftBuildCommit = buildCommit;
    }

    public String getOpenshiftBuildName()
    {
        return openshiftBuildName == null ? SYSTEM_BUILD_NAME : openshiftBuildName;
    }

    @ConfigName( BUILD_NAME_KEY )
    public void setOpenshiftBuildName( String buildName )
    {
        this.openshiftBuildName = buildName;
    }

    public String getOpenshiftBuildNamespace()
    {
        return openshiftBuildNamespace == null ? SYSTEM_BUILD_NAMESPACE : openshiftBuildNamespace;
    }

    @ConfigName( BUILD_NAMESPACE_KEY )
    public void setOpenshiftBuildNamespace( String buildNamespace )
    {
        this.openshiftBuildNamespace = buildNamespace;
    }

    public Map<String, String> getEnvars()
    {
        Map<String, String> result = new HashMap<>();
        result.put( HOST_NAME_KEY, getHostname() );
        result.put( BUILD_COMMIT_KEY, getOpenshiftBuildCommit() );
        result.put( BUILD_NAME_KEY, getOpenshiftBuildName() );
        result.put( BUILD_NAMESPACE_KEY, getOpenshiftBuildNamespace() );
        return result;
    }

}
