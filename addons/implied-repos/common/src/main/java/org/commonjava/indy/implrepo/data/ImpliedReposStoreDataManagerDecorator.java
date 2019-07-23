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
package org.commonjava.indy.implrepo.data;

import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.data.ArtifactStoreQuery;
import org.commonjava.indy.implrepo.conf.ImpliedRepoConfig;
import org.commonjava.indy.model.core.ArtifactStore;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;

/**
 * Wrap methods that retrieve stores for a group, or groups containing a store. Check if the store(s) in question are
 * implied (created by implied-repos), and whether the group has implied repositories enabled. If the two don't match,
 * filter the results appropriately to keep implied repos out of groups that don't support them.
 *
 * Created by jdcasey on 8/17/16.
 */
@Decorator
public abstract class ImpliedReposStoreDataManagerDecorator
        implements StoreDataManager
{
    public static final String IMPLIED_REPO_ORIGIN = "implied-repos";

    @Delegate
    @Inject
    private StoreDataManager delegate;

    @Inject
    private ImpliedRepoConfig config;

    public ArtifactStoreQuery<ArtifactStore> query()
    {
        return new ImpliedReposQueryDelegate( delegate.query(), this, config );
    }

}
