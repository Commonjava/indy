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
package org.commonjava.indy.implrepo.inject;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.indy.implrepo.data.ImpliedRepoMetadataManager;
import org.commonjava.indy.model.core.io.IndyObjectMapper;

@ApplicationScoped
public class ImpliedRepoProvider
{
    private ImpliedRepoMetadataManager metadataManager;

    @Inject
    private IndyObjectMapper mapper;

    @PostConstruct
    public void setup()
    {
        metadataManager = new ImpliedRepoMetadataManager( mapper );
    }

    @Produces
    @Default
    public ImpliedRepoMetadataManager getImpliedRepoMetadataManager()
    {
        return metadataManager;
    }
}
