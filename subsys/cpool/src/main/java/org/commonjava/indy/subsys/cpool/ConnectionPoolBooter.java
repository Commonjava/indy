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
package org.commonjava.indy.subsys.cpool;

import org.commonjava.indy.action.BootupAction;
import org.commonjava.indy.action.IndyLifecycleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

@ApplicationScoped
public class ConnectionPoolBooter
        implements BootupAction
{
    @Inject
    private Instance<ConnectionPoolProvider> connectionPoolProvider;

    @Override
    public void init()
            throws IndyLifecycleException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.info( "\n\n\n\nStarting JNDI Connection Pools\n\n\n\n" );
        connectionPoolProvider.get().init();
        logger.info( "Connection pools started." );
    }

    @Override
    public int getBootPriority()
    {
        return 100;
    }

    @Override
    public String getId()
    {
        return "JNDI Connection Pools";
    }
}
