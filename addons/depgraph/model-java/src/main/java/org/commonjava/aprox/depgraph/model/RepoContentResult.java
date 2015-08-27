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
package org.commonjava.aprox.depgraph.model;

import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class RepoContentResult
    implements Iterable<ProjectVersionRef>
{

    private Map<StoreKey, String> repoUrls;

    private Map<ProjectVersionRef, ProjectRepoContent> projects;

    public RepoContentResult(){}

    public Map<ProjectVersionRef, ProjectRepoContent> getProjects()
    {
        return projects;
    }

    public void setProjects( Map<ProjectVersionRef, ProjectRepoContent> projects )
    {
        this.projects = projects;
    }

    public synchronized void addProject( ProjectVersionRef ref, ProjectRepoContent content )
    {
        if ( projects == null )
        {
            projects = new HashMap<>();
        }

        projects.put( ref, content );
    }

    public Map<StoreKey, String> getRepoUrls()
    {
        return repoUrls;
    }

    public void setRepoUrls( Map<StoreKey, String> repoUrls )
    {
        this.repoUrls = repoUrls;
    }

    public synchronized void addRepoUrl( StoreKey key, String url )
    {
        if ( repoUrls == null )
        {
            repoUrls = new HashMap<>();
        }
        repoUrls.put( key, url );
    }

    @Override
    public Iterator<ProjectVersionRef> iterator()
    {
        return projects == null ? Collections.<ProjectVersionRef> emptySet().iterator() : projects.keySet().iterator();
    }

    public ProjectRepoContent getProject( ProjectVersionRef ref )
    {
        return projects == null ? null : projects.get( ref );
    }

    public String getRepoUrl( StoreKey key )
    {
        return repoUrls == null ? null : repoUrls.get( key );
    }
}
