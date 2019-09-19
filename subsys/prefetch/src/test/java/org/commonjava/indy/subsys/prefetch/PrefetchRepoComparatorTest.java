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
package org.commonjava.indy.subsys.prefetch;

import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;
import org.commonjava.indy.subsys.prefetch.PrefetchRepoComparator;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Copyright (C) 2013 Red Hat, Inc.
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

public class PrefetchRepoComparatorTest
{
    private final PrefetchRepoComparator<RemoteRepository> repoComparator = new PrefetchRepoComparator<>();

    @Test
    public void simpleTest()
    {
        RemoteRepository repo1 =
                new RemoteRepository( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, "repo1", "http://localhost/test1" );
        RemoteRepository repo2 =
                new RemoteRepository( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, "repo2", "http://localhost/test2" );

        repo1.setPrefetchPriority( 2 );
        repo2.setPrefetchPriority( 3 );
        assertTrue( repoComparator.compare( repo1, repo2 ) > 0 );

        repo2.setPrefetchRescanTimestamp( "2018-06-08 11:09:42 UTC+000" );
        assertTrue( repoComparator.compare( repo1, repo2 ) < 0 );

        repo1.setPrefetchRescanTimestamp( "2018-06-08 10:09:42 UTC+0000" );
        repo2.setPrefetchRescanTimestamp( null );
        assertTrue( repoComparator.compare( repo1, repo2 ) > 0 );

        repo2.setPrefetchPriority( 1 );
        repo2.setPrefetchRescanTimestamp( "2018-06-08 11:09:42 UTC+0000" );
        assertTrue( repoComparator.compare( repo1, repo2 ) < 0 );

    }

    @Test
    public void collectionTest()
    {

        RemoteRepository repo1 =
                new RemoteRepository( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, "repo1", "http://localhost/test1" );
        RemoteRepository repo2 =
                new RemoteRepository( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, "repo2", "http://localhost/test2" );
        RemoteRepository repo3 =
                new RemoteRepository( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, "repo3", "http://localhost/test3" );

        repo1.setPrefetchPriority( 1 );
        repo2.setPrefetchPriority( 2 );
        repo3.setPrefetchPriority( 3 );

        List<RemoteRepository> list = new ArrayList<>( 3 );

        list.add( repo1 );
        list.add( repo2 );
        list.add( repo3 );
        list.sort( repoComparator );
        assertEquals( repo3, list.get( 0 ) );

        repo3.setPrefetchRescanTimestamp("2018-06-08 11:09:42 UTC+0000"  );
        list.sort( repoComparator );
        assertEquals( repo2, list.get( 0 ) );

        repo2.setPrefetchRescanTimestamp("2018-06-08 09:09:42 UTC+0000"  );
        list.sort( repoComparator );
        assertEquals( repo1, list.get( 0 ) );

        repo1.setPrefetchRescanTimestamp("2018-06-08 08:09:42 UTC+0000"  );
        list.sort( repoComparator );
        assertEquals( repo1, list.get( 0 ) );

        repo2.setPrefetchRescanTimestamp("2018-06-08 08:09:42 UTC+0000"  );
        list.sort( repoComparator );
        assertEquals( repo2, list.get( 0 ) );

        repo3.setPrefetchRescanTimestamp("2018-06-08 08:09:42 UTC+0000"  );
        list.sort( repoComparator );
        assertEquals( repo3, list.get( 0 ) );
    }
}
