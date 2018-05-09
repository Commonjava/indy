/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.relate.ftest;

import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by ruhan on 2/17/17.
 *
 * <b>GIVEN:</b>
 * <ul>
 *     <li>{@link HostedRepository} A</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Path P points to a POM file and the POM is uploaded to {@link HostedRepository} A</li>
 *     <li>Path R points to the Rel file of the target POM</li>
 *     <li>Path R is requested from {@link HostedRepository} A</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>{@link HostedRepository} A returns notNull (exists) for Path R</li>
 * </ul>
 */
public class PomUploadListenerTest
        extends AbstractRelateFunctionalTest
{
    private static final String path = "org/foo/bar/1/bar-1.pom";

    private static final String pathRel = path + ".rel";

    private static final String content =
                    "<project><modelVersion>4.0.0</modelVersion><groupId>org.foo</groupId><artifactId>bar</artifactId><version>1</version></project>";

    @Test
    public void run() throws Exception
    {
        final String repo1 = "repo1";

        HostedRepository hosted1 = new HostedRepository( repo1 );
        client.stores().create( hosted1, "adding hosted", HostedRepository.class );

        StoreKey key = new StoreKey( hosted, repo1 );
        InputStream stream = new ByteArrayInputStream( content.getBytes() );
        client.content().store( key, path, stream );

        waitForEventPropagation();

        boolean exists = client.content().exists( hosted, repo1, pathRel, true );
        assertThat( exists, equalTo( true ) );
    }
}
