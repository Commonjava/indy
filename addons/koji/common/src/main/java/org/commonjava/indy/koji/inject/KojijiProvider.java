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
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.WeftManaged;
import org.commonjava.indy.koji.conf.IndyKojiConfig;
import org.commonjava.rwx.binding.error.BindException;
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
{
    @Inject
    private IndyKojiConfig config;

    private KojiClient kojiClient;

    private PasswordManager kojiPasswordManager;

    @Inject
    @WeftManaged
    @ExecutorConfig( named = "koji-queries", threads = 4 )
    private ExecutorService kojiExecutor;

    @PostConstruct
    public void setup()
    {
        kojiPasswordManager = new MemoryPasswordManager();
        if ( config.getProxyPassword() != null )
        {
            kojiPasswordManager.bind( config.getProxyPassword(), config.getKojiSiteId(), PasswordType.PROXY );
        }

        try
        {
            kojiClient = new KojiClient( config, kojiPasswordManager, kojiExecutor );
        }
        catch ( BindException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Produces
    public KojiClient getKojiClient()
    {
        return kojiClient;
    }
}
