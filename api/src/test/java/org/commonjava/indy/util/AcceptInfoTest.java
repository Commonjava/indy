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
package org.commonjava.indy.util;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class AcceptInfoTest
{

    @Test
    public void singleAcceptHeader()
        throws Exception
    {
        final List<AcceptInfo> accepts = AcceptInfo.parser()
                                                   .parse( "text/html" );
        assertThat( accepts.size(), equalTo( 1 ) );

        final AcceptInfo accept = accepts.get( 0 );
        assertThat( accept.getBaseAccept(), equalTo( "text/html" ) );
    }

    @Test
    public void multiAcceptHeader()
        throws Exception
    {
        final List<AcceptInfo> infos =
            AcceptInfo.parser()
                      .parse( "text/html", "application/xhtml+xml", "application/xml;q=0.9", "*/*;q=0.8" );

        assertThat( infos.size(), equalTo( 4 ) );
    }

    @Test
    public void multiAcceptHeader_SingleHeaderString()
        throws Exception
    {
        final List<AcceptInfo> infos =
            AcceptInfo.parser()
                      .parse( "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8" );

        assertThat( infos.size(), equalTo( 4 ) );
    }

}
