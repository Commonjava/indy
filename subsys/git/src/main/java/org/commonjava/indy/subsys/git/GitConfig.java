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
package org.commonjava.indy.subsys.git;

import java.io.File;

public class GitConfig
{

    private static final long DEFAULT_LOCK_TIMEOUT_MILLIS = 2000;

    private final File dir;

    private final String url;

    private final boolean commitFileManifestsEnabled;

    private String remoteBranchName;

    private String userEmail;

    private Long lockTimeoutMillis;

    public GitConfig( final File dir, final String url, final boolean commitFileManifestsEnabled )
    {
        this.dir = dir;
        this.url = url;
        this.commitFileManifestsEnabled = commitFileManifestsEnabled;
    }

    public File getContentDir()
    {
        return dir;
    }

    public String getCloneFrom()
    {
        return url;
    }

    public boolean isCommitFileManifestsEnabled()
    {
        return commitFileManifestsEnabled;
    }

    public GitConfig setRemoteBranchName( final String remoteBranchName )
    {
        if ( remoteBranchName != null )
        {
            this.remoteBranchName = remoteBranchName;
        }

        return this;
    }

    public String getRemoteBranchName()
    {
        return remoteBranchName == null ? "master" : remoteBranchName;
    }

    public GitConfig setUserEmail( final String userEmail )
    {
        if ( userEmail != null )
        {
            this.userEmail = userEmail;
        }

        return this;
    }

    public String getUserEmail()
    {
        return userEmail;
    }

    public Long getLockTimeoutMillis()
    {
        return lockTimeoutMillis;
    }

    public void setLockTimeoutMillis( Long lockTimeoutMillis )
    {
        this.lockTimeoutMillis = lockTimeoutMillis;
    }

    public long getLockMillis()
    {
        return lockTimeoutMillis == null ? DEFAULT_LOCK_TIMEOUT_MILLIS : lockTimeoutMillis;
    }
}
