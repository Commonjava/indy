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
package org.commonjava.indy.revisions.conf;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.indy.subsys.git.ConflictStrategy;
import org.commonjava.propulsor.config.annotation.ConfigName;
import org.commonjava.propulsor.config.annotation.SectionName;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.InputStream;

@SectionName( "revisions" )
@ApplicationScoped
public class RevisionsConfig
    implements IndyConfigInfo
{

    private boolean enabled = false;

    private boolean pushEnabled = false;

    private String dataUpstreamUrl;

    private String conflictAction;

    private String branchName;

    private String userEmail;

    public boolean isEnabled()
    {
        return enabled;
    }

    @ConfigName( "enabled" )
    public void setEnabled( boolean enabled )
    {
        this.enabled = enabled;
    }

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
