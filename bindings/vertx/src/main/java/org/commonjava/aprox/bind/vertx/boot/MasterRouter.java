package org.commonjava.aprox.bind.vertx.boot;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.commonjava.vertx.vabr.ApplicationRouter;
import org.commonjava.vertx.vabr.MultiApplicationRouter;

@ApplicationScoped
public class MasterRouter
    extends MultiApplicationRouter
{

    public static final String PREFIX = "";

    @Inject
    private Instance<AproxRouter> routers;

    protected MasterRouter()
    {
    }

    public MasterRouter( final List<ApplicationRouter> routers )
    {
        super( routers );
    }

    @PostConstruct
    public void initializeComponents()
    {
        logger.info( "\n\nCONSTRUCTING WEB ROUTES FOR ALL APPS IN APROX...\n\n" );

        final Set<AproxRouter> r = new HashSet<>();
        for ( final AproxRouter router : routers )
        {
            //            router.initializeComponents();
            logger.info( router.getClass()
                               .getName() );
            r.add( router );
        }

        bindRouters( r );

        logger.info( "...done" );
    }

}
