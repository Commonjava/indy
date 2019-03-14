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
package org.commonjava.indy.subsys.cpool;

import java.util.Collections;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public class ConnectionPoolInfo
{
    private final String name;
    private final String url;

    private final String user;

    private final String password;

    private String dataSourceClassname;

    private String driverClassname;

    private boolean useMetrics;

    private boolean useHealthChecks;

    private final Map<String, String> properties;

    public ConnectionPoolInfo( final String name, final String url, final String user, final String password,
                               final String dataSourceClassname, final String driverClassname, final boolean useMetrics, final boolean useHealthChecks,
                               final Map<String, String> properties )
    {
        this.name = name;
        this.url = url;
        this.user = user;
        this.password = password;
        this.dataSourceClassname = dataSourceClassname;
        this.driverClassname = driverClassname;
        this.useMetrics = useMetrics;
        this.useHealthChecks = useHealthChecks;
        this.properties = properties;
    }

    public boolean isValid()
    {
        return isNotBlank( name ) && isNotBlank( url ) && isNotBlank( dataSourceClassname );
    }

    public boolean isUseMetrics()
    {
        return useMetrics;
    }

    public boolean isUseHealthChecks()
    {
        return useHealthChecks;
    }

    public String getName()
    {
        return name;
    }

    public String getDataSourceClassname()
    {
        return dataSourceClassname;
    }

    public String getDriverClassname()
    {
        return driverClassname;
    }

    public String getUrl()
    {
        return url;
    }

    public String getUser()
    {
        return user;
    }

    public String getPassword()
    {
        return password;
    }

    public Map<String, String> getProperties()
    {
        return Collections.unmodifiableMap( properties );
    }

    @Override
    public String toString()
    {
        return "ConnectionPoolInfo{" + "name='" + name + '\'' + ", url='" + url + '\'' + ", dataSourceClassname='"
                + dataSourceClassname + '\'' + ", driverClassname='" + driverClassname + '\'' + ", useMetrics="
                + useMetrics + ", useHealthChecks=" + useHealthChecks + ", properties=" + properties + '}';
    }
}
