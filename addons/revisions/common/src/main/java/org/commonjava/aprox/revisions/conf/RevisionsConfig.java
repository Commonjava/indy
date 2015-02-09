package org.commonjava.aprox.revisions.conf;

import java.io.File;
import java.io.InputStream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.aprox.conf.AbstractAproxConfigInfo;
import org.commonjava.aprox.conf.AbstractAproxFeatureConfig;
import org.commonjava.aprox.conf.AproxConfigClassInfo;
import org.commonjava.aprox.conf.AproxConfigInfo;
import org.commonjava.aprox.subsys.git.ConflictStrategy;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;

@SectionName( "revisions" )
@Alternative
@Named
public class RevisionsConfig
{

    @javax.enterprise.context.ApplicationScoped
    public static class ConfigInfo
        extends AbstractAproxConfigInfo
    {
        public ConfigInfo()
        {
            super( RevisionsConfig.class );
        }

        @Override
        public String getDefaultConfigFileName()
        {
            return new File( AproxConfigInfo.CONF_INCLUDES_DIR, "revisions.conf" ).getPath();
        }

        @Override
        public InputStream getDefaultConfig()
        {
            return Thread.currentThread()
                         .getContextClassLoader()
                         .getResourceAsStream( "default-revisions.conf" );
        }
    }

    @javax.enterprise.context.ApplicationScoped
    public static class FeatureConfig
        extends AbstractAproxFeatureConfig<RevisionsConfig, RevisionsConfig>
    {
        @Inject
        private ConfigInfo info;

        public FeatureConfig()
        {
            super( RevisionsConfig.class );
        }

        @Produces
        @Default
        @ApplicationScoped
        public RevisionsConfig getRevisionsConfig()
            throws ConfigurationException
        {
            final RevisionsConfig config = getConfig();
            return config == null ? new RevisionsConfig() : config;
        }

        @Override
        public AproxConfigClassInfo getInfo()
        {
            return info;
        }
    }

    private boolean pushEnabled = false;

    private String dataUpstreamUrl;

    private String conflictAction;

    private String branchName;

    private String userEmail;

    public boolean isPushEnabled()
    {
        return pushEnabled;
    }

    @ConfigName( "push.enabled" )
    public void setPushEnabled( final boolean pushEnabled )
    {
        this.pushEnabled = pushEnabled;
    }

    public String getDataUpstreamUrl()
    {
        return dataUpstreamUrl;
    }

    @ConfigName( "data.upstream.url" )
    public void setDataUpstreamUrl( final String dataUpstreamUrl )
    {
        this.dataUpstreamUrl = dataUpstreamUrl;
    }

    public String getConflictAction()
    {
        return conflictAction;
    }

    @ConfigName( "conflict.action" )
    public void setConflictAction( final String conflictStrategy )
    {
        this.conflictAction = conflictStrategy;
    }

    public ConflictStrategy getConflictStrategy()
    {
        String action = conflictAction;
        if ( action == null )
        {
            action = ConflictStrategy.overwrite.name();
        }
        else
        {
            action = action.toLowerCase();
        }

        ConflictStrategy result = ConflictStrategy.valueOf( action );
        if ( result == null )
        {
            result = ConflictStrategy.overwrite;
        }

        return result;
    }

    public String getBranchName()
    {
        return branchName;
    }

    @ConfigName( "branch.name" )
    public void setBranchName( final String branchName )
    {
        this.branchName = branchName;
    }

    public String getUserEmail()
    {
        return userEmail;
    }

    @ConfigName( "user.email" )
    public void setUserEmail( final String userEmail )
    {
        this.userEmail = userEmail;
    }

}
