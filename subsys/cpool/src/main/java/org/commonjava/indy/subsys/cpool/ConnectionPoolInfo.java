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
        return "ConnectionPoolInfo{" + "name='" + name + '\'' + ", url='" + url + '\'' + ", user='" + user + '\''
                + ", password='" + password + '\'' + ", dataSourceClassname='" + dataSourceClassname + '\''
                + ", driverClassname='" + driverClassname + '\'' + ", useMetrics=" + useMetrics + ", useHealthChecks="
                + useHealthChecks + ", properties=" + properties + '}';
    }
}
