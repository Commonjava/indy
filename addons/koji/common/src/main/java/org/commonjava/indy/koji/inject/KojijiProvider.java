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
