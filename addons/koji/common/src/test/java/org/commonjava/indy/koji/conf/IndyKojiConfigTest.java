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
package org.commonjava.indy.koji.conf;

import org.commonjava.indy.conf.AbstractIndyMapConfig;
import org.commonjava.indy.test.utils.WeldJUnit4Runner;
import org.jboss.weld.Weld;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import static org.junit.Assert.assertTrue;

/**
 * Created by jdcasey on 6/7/16.
 */
@RunWith( WeldJUnit4Runner.class )
public class IndyKojiConfigTest
{

    @Inject
    private Instance<AbstractIndyMapConfig> instances;

    @Test
    public void mapConfigsContainKojiConfig()
    {
        boolean found = false;
        for ( AbstractIndyMapConfig config : instances )
        {
            if ( config instanceof IndyKojiConfig )
            {
                found = true;
                break;
            }
        }

        assertTrue( "Cannot find IndyKojiConfig in Instance<AbstractIndyMapConfig>!", found );
    }
}
