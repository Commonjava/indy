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
package org.commonjava.indy.depgraph.model;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import java.util.Map;

public class UrlMapResult
{

    private Map<ProjectVersionRef, UrlMapProject> projects;

    public UrlMapResult(){}

    public UrlMapResult( Map<ProjectVersionRef, UrlMapProject> projects )
    {
        this.projects = projects;
    }

    public Map<ProjectVersionRef, UrlMapProject> getProjects()
    {
        return projects;
    }

    public void setProjects( Map<ProjectVersionRef, UrlMapProject> projects )
    {
        this.projects = projects;
    }

}
