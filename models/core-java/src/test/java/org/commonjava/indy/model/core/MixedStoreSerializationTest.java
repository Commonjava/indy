/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.model.core;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.commonjava.indy.model.core.StoreType.remote;
import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_MAVEN;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class MixedStoreSerializationTest
{
    @Test
    public void simpleRoundTrip()
            throws IOException, ClassNotFoundException
    {
        List<ArtifactStore> in = new ArrayList<>();
        in.add( new HostedRepository( PKG_TYPE_MAVEN, "test-hosted" ) );
        in.add( new RemoteRepository( PKG_TYPE_MAVEN, "test-remote", "http://nowhere.com" ) );
        in.add( new Group( PKG_TYPE_MAVEN, "test-group",
                                      new StoreKey( PKG_TYPE_MAVEN, hosted, "test-hosted" ),
                                      new StoreKey( PKG_TYPE_MAVEN, remote, "test-remote" ) ) );

        List<ArtifactStore> out = doRoundTrip( in );
        System.out.println( out );
    }

    private List<ArtifactStore> doRoundTrip( final List<ArtifactStore> in )
            throws IOException, ClassNotFoundException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream( baos );
        oos.writeObject( in );

        oos.flush();
        ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( baos.toByteArray() ) );

        return (List<ArtifactStore>) ois.readObject();
    }
}
