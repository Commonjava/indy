/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.indy.revisions.conf;

import java.io.File;
import java.io.InputStream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.indy.conf.AbstractIndyConfigInfo;
import org.commonjava.indy.conf.AbstractIndyFeatureConfig;
import org.commonjava.indy.conf.IndyConfigClassInfo;
import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.indy.subsys.git.ConflictStrategy;
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
        extends AbstractIndyConfigInfo
    {
        public ConfigInfo()
        {
            super( RevisionsConfig.class );
        }

        @Override
        public String getDefaultConfigFileName()
        {
            return new File( IndyConfigInfo.CONF_INCLUDES_DIR, "revisions.conf" ).getPath();
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
        extends AbstractIndyFeatureConfig<RevisionsConfig, RevisionsConfig>
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
        public IndyConfigClassInfo getInfo()
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
