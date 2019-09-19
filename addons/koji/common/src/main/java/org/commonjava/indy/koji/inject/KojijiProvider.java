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
package org.commonjava.indy.koji.inject;

import com.codahale.metrics.MetricRegistry;
import com.redhat.red.build.koji.KojiClient;
import com.redhat.red.build.koji.KojiClientException;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.Locker;
import org.commonjava.cdi.util.weft.WeftManaged;
import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.ShutdownAction;
import org.commonjava.indy.action.StartupAction;
import org.commonjava.indy.koji.conf.IndyKojiConfig;
import org.commonjava.indy.metrics.conf.IndyMetricsConfig;
import org.commonjava.atlas.maven.ident.ref.ProjectRef;
import org.commonjava.util.jhttpc.auth.MemoryPasswordManager;
import org.commonjava.util.jhttpc.auth.PasswordManager;
import org.commonjava.util.jhttpc.auth.PasswordType;

import javax.annotation.PostConstruct;
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

    private Locker<ProjectRef> versionMetadataLocks;

    @Inject
    private IndyMetricsConfig indyMetricsConfig;

    @Inject
    private MetricRegistry metricRegistry;

    @Inject
    @WeftManaged
    @ExecutorConfig( named = "koji-queries", threads = 4 )
    private ExecutorService kojiExecutor;

    @Produces
    public KojiClient getKojiClient()
    {
        return kojiClient;
    }

    @KojiMavenVersionMetadataLocks
    @Produces
    @ApplicationScoped
    public Locker<ProjectRef> getVersionMetadataLocks()
    {
        return versionMetadataLocks;
    }

    @Override
    public void start() throws IndyLifecycleException
    {
        if ( !config.isEnabled() )
        {
            return;
        }

        kojiPasswordManager = new MemoryPasswordManager();
        if ( config.getProxyPassword() != null )
        {
            kojiPasswordManager.bind( config.getProxyPassword(), config.getKojiSiteId(), PasswordType.PROXY );
        }

        if ( config.getKeyPassword() != null )
        {
            kojiPasswordManager.bind( config.getKeyPassword(), config.getKojiSiteId(), PasswordType.KEY );
        }

        try
        {
            if ( indyMetricsConfig.isKojiMetricEnabled() )
            {
                kojiClient = new KojiClient( config, kojiPasswordManager, kojiExecutor, metricRegistry );
            }
            else
            {
                kojiClient = new KojiClient( config, kojiPasswordManager, kojiExecutor );
            }
        }
        catch ( KojiClientException e )
        {
            throw new IndyLifecycleException( "Failed to initialize Koji client.", e );
        }

        versionMetadataLocks = new Locker<>();
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
        return 100;
    }
}
