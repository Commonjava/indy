package org.commonjava.indy.koji.conf;

import com.redhat.red.build.koji.config.KojiConfig;
import org.commonjava.rwx.binding.anno.Contains;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.commonjava.util.jhttpc.model.SiteConfigBuilder;
import org.commonjava.util.jhttpc.model.SiteTrustType;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;

import static org.commonjava.util.jhttpc.model.SiteConfig.DEFAULT_MAX_CONNECTIONS;
import static org.commonjava.util.jhttpc.model.SiteConfig.DEFAULT_PROXY_PORT;
import static org.commonjava.util.jhttpc.model.SiteConfig.DEFAULT_REQUEST_TIMEOUT_SECONDS;

/**
 * Created by jdcasey on 5/20/16.
 */
@SectionName( "koji" )
@ApplicationScoped
public class IndyKojiConfig
        implements KojiConfig
{
    private static final String KOJI_SITE_ID = "koji";

    private String url;

    private String clientPemPath;

    private String serverPemPath;

    private String keyPassword;

    private Integer maxConnections;

    private String proxyHost;

    private Integer proxyPort;

    private String proxyUser;

    private Integer requestTimeoutSeconds;

    private String siteTrustType;

    private String proxyPassword;

    @Override
    public SiteConfig getKojiSiteConfig()
            throws IOException
    {
        return new SiteConfigBuilder().withId( getKojiSiteId() )
                                      .withKeyCertPem( getClientPemPath() )
                                      .withServerCertPem( getServerPemPath() )
                                      .withUri( getKojiURL() )
                                      .withMaxConnections( getMaxConnections() )
                                      .withProxyHost( getProxyHost() )
                                      .withProxyPort( getProxyPort() )
                                      .withProxyUser( getProxyUser() )
                                      .withRequestTimeoutSeconds( getRequestTimeoutSeconds() )
                                      .withTrustType( SiteTrustType.getType( getSiteTrustType() ) )
                                      .build();
    }

    @Override
    public String getKojiURL()
    {
        return url;
    }

    @Override
    public String getKojiClientCertificatePassword()
    {
        return keyPassword;
    }

    @Override
    public String getKojiSiteId()
    {
        return KOJI_SITE_ID;
    }

    public Integer getMaxConnections()
    {
        return maxConnections == null ? DEFAULT_MAX_CONNECTIONS : maxConnections;
    }

    @ConfigName( "max.connections" )
    public void setMaxConnections( Integer maxConnections )
    {
        this.maxConnections = maxConnections;
    }

    public String getServerPemPath()
    {
        return serverPemPath;
    }

    @ConfigName( "server.pem" )
    public void setServerPemPath( String serverPemPath )
    {
        this.serverPemPath = serverPemPath;
    }

    public String getClientPemPath()
    {
        return clientPemPath;
    }

    @ConfigName( "client.pem" )
    public void setClientPemPath( String clientPemPath )
    {
        this.clientPemPath = clientPemPath;
    }

    public String getProxyHost()
    {
        return proxyHost;
    }

    @ConfigName( "proxy.host" )
    public void setProxyHost( String proxyHost )
    {
        this.proxyHost = proxyHost;
    }

    public Integer getProxyPort()
    {
        return proxyPort == null ? DEFAULT_PROXY_PORT : proxyPort;
    }

    @ConfigName( "proxy.port" )
    public void setProxyPort( Integer proxyPort )
    {
        this.proxyPort = proxyPort;
    }

    public String getProxyUser()
    {
        return proxyUser;
    }

    @ConfigName( "proxy.user" )
    public void setProxyUser( String proxyUser )
    {
        this.proxyUser = proxyUser;
    }

    public Integer getRequestTimeoutSeconds()
    {
        return requestTimeoutSeconds == null ? DEFAULT_REQUEST_TIMEOUT_SECONDS : requestTimeoutSeconds;
    }

    @ConfigName( "request.timeout.seconds" )
    public void setRequestTimeoutSeconds( Integer requestTimeoutSeconds )
    {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    public String getSiteTrustType()
    {
        return siteTrustType;
    }

    @ConfigName( "ssl.trust.type" )
    public void setSiteTrustType( String siteTrustType )
    {
        this.siteTrustType = siteTrustType;
    }

    public String getUrl()
    {
        return url;
    }

    @ConfigName( "url" )
    public void setUrl( String url )
    {
        this.url = url;
    }

    public String getKeyPassword()
    {
        return keyPassword;
    }

    @ConfigName( "client.pem.password" )
    public void setKeyPassword( String keyPassword )
    {
        this.keyPassword = keyPassword;
    }

    public String getProxyPassword()
    {
        return proxyPassword;
    }

    @ConfigName( "proxy.password" )
    public void setProxyPassword( String proxyPassword )
    {
        this.proxyPassword = proxyPassword;
    }
}
