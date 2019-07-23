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
package org.commonjava.indy.promote.client;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.indy.client.core.Indy;
import org.junit.Test;

public class IndyPromoteClientModuleUrlsTest
{

    private static final String BASE = "http://localhost:8080/api";

    @Test
    public void promoteUrl()
        throws Exception
    {
        final String url = new Indy( BASE, new IndyPromoteClientModule() ).module( IndyPromoteClientModule.class )
                                                                            .promoteUrl();

        assertThat( url, equalTo( BASE + "/" + IndyPromoteClientModule.PATHS_PROMOTE_PATH ) );
    }

    @Test
    public void rollbackUrl()
        throws Exception
    {
        final String url = new Indy( BASE, new IndyPromoteClientModule() ).module( IndyPromoteClientModule.class )
                                                                            .rollbackUrl();

        assertThat( url, equalTo( BASE + "/" + IndyPromoteClientModule.PATHS_ROLLBACK_PATH ) );
    }

}
