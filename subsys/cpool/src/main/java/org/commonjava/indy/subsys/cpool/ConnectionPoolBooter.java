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
