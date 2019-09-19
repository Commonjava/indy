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
package org.commonjava.indy.ftest.core.content;

import org.commonjava.indy.ftest.core.category.EventDependent;
import org.commonjava.indy.ftest.core.category.TimingDependent;
import org.commonjava.indy.model.core.RemoteRepository;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ContentTimeoutWorkingTest
        extends AbstractContentTimeoutWorkingTest
{
    @Test
    @Category( TimingDependent.class )
    public void timeoutArtifact()
            throws Exception
    {
        fileCheckingAfterTimeout();
    }

    @Override
    protected RemoteRepository createRemoteRepository( String repoId )
    {
        final RemoteRepository repository = new RemoteRepository( repoId, server.formatUrl( repoId ) );
        repository.setCacheTimeoutSeconds( TIMEOUT_SECONDS );
        return repository;
    }
}
