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
package org.commonjava.indy.koji.inject;

import com.redhat.red.build.koji.KojiClient;
import com.redhat.red.build.koji.config.KojiConfig;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.ShutdownAction;
import org.commonjava.indy.action.StartupAction;
import org.commonjava.indy.koji.conf.IndyKojiConfig;
import org.commonjava.rwx.binding.error.BindException;
import org.commonjava.util.jhttpc.auth.MemoryPasswordManager;
import org.commonjava.util.jhttpc.auth.PasswordManager;
import org.commonjava.util.jhttpc.auth.PasswordType;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.concurrent.ExecutorService;

/**
 * Initializes a Kojiji client ({@link KojiClient}) and provides it via CDI.
 *
 * Created by jdcasey on 5/20/16.
 */
@ApplicationScoped
public class KojijiProvider
        implements StartupAction, ShutdownAction
{
    @Inject
    private IndyKojiConfig config;

    private KojiClient kojiClient;

    private PasswordManager kojiPasswordManager;

    @Inject
    @ExecutorConfig( named = "koji-queries", threads = 4 )
    private ExecutorService kojiExecutor;

    @Produces
    public KojiClient getKojiClient()
    {
        return kojiClient;
    }

    @Override
    public void start()
            throws IndyLifecycleException
    {
        if ( !config.isEnabled() )
        {
            return;
        }

        KojiConfig kojiConfig = config.getKojiConfig();

        kojiPasswordManager = new MemoryPasswordManager();
        if ( config.getProxyPassword() != null )
        {
            kojiPasswordManager.bind( config.getProxyPassword(), kojiConfig.getKojiSiteId(), PasswordType.PROXY );
        }

        if ( config.getKeyPassword() != null )
        {
            kojiPasswordManager.bind( config.getKeyPassword(), kojiConfig.getKojiSiteId(), PasswordType.KEY );
        }

        try
        {
            kojiClient = new KojiClient( kojiConfig, kojiPasswordManager, kojiExecutor );
        }
        catch ( BindException e )
        {
            throw new IndyLifecycleException( "Failed to start koji client: %s", e, e.getMessage() );
        }
    }

    @Override
    public int getStartupPriority()
    {
        return 90;
    }

    @Override
    public String getId()
    {
        return "koji-client";
    }

    @Override
    public void stop()
            throws IndyLifecycleException
    {
        if ( kojiClient != null )
        {
            kojiClient.close();
        }
    }

    @Override
    public int getShutdownPriority()
    {
        return 1;
    }
}
