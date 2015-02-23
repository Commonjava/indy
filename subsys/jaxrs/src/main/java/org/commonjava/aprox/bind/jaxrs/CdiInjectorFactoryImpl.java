package org.commonjava.aprox.bind.jaxrs;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

import org.jboss.resteasy.cdi.CdiInjectorFactory;
import org.slf4j.LoggerFactory;

public class CdiInjectorFactoryImpl
    extends CdiInjectorFactory
{

    @Override
    protected BeanManager lookupBeanManager()
    {
        final BeanManager bmgr = CDI.current()
                  .getBeanManager();

        LoggerFactory.getLogger( getClass() )
                     .info( "\n\n\n\nRESTEasy Using BeanManager: {}\n\n\n\n", bmgr );

        return bmgr;
    }

}
