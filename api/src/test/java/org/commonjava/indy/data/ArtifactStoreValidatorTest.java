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
package org.commonjava.indy.data;

import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.maven.galley.GalleyCore;
import org.commonjava.maven.galley.GalleyCoreBuilder;
import org.commonjava.maven.galley.auth.MemoryPasswordManager;
import org.commonjava.maven.galley.cache.FileCacheProvider;
import org.commonjava.maven.galley.cache.FileCacheProviderFactory;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.event.NoOpFileEventManager;
import org.commonjava.maven.galley.io.HashedLocationPathGenerator;
import org.commonjava.maven.galley.io.NoOpTransferDecorator;
import org.commonjava.maven.galley.transport.htcli.HttpClientTransport;
import org.commonjava.maven.galley.transport.htcli.HttpImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ArtifactStoreValidatorTest
{

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private FileCacheProvider cache;

    private GalleyCore galley;

    private File cacheDir;

    private ArtifactStoreValidator validator;

    @Before
    public void setup()
            throws Exception
    {
        cacheDir = temp.newFolder();

        galley = new GalleyCoreBuilder( new FileCacheProviderFactory( cacheDir ) ).withEnabledTransports(
                new HttpClientTransport( new HttpImpl( new MemoryPasswordManager() ) ) ).build();

        validator = new ArtifactStoreValidator( galley.getTransferManager() );
    }

    @Test
    public void testRepoValidation()
            throws Exception
    {
        RemoteRepository validRepo = new RemoteRepository( "test", "http://www.foo.com" );
        assertTrue( validator.isValid( validRepo ) );

        RemoteRepository inValidRepo = new RemoteRepository( "test", "this.is.not.valid.repo" );
        assertFalse( validator.isValid( inValidRepo ) );

        Group group = new Group( "group" );
        assertTrue( validator.isValid( group ) );

        HostedRepository hostedRepository = new HostedRepository( "hosted" );
        assertTrue( validator.isValid( hostedRepository ) );
    }
}