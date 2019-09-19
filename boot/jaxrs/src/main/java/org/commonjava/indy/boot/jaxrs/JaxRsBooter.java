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
package org.commonjava.indy.boot.jaxrs;

import org.commonjava.atservice.annotation.Service;
import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.IndyLifecycleManager;
import org.commonjava.propulsor.boot.BootInterface;
import org.commonjava.propulsor.boot.BootOptions;
import org.commonjava.propulsor.boot.BootException;
import org.commonjava.propulsor.boot.Booter;
import org.commonjava.propulsor.boot.WeldBootInterface;
import org.commonjava.propulsor.deploy.DeployException;
import org.commonjava.propulsor.lifecycle.AppLifecycleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.commonjava.propulsor.boot.BootStatus.ERR_LOAD_BOOT_OPTIONS;
import static org.commonjava.propulsor.boot.BootStatus.ERR_START;

@Service( BootInterface.class )
public class JaxRsBooter
                extends Booter
                implements WeldBootInterface
{
    public static final String BOOT_DEFAULTS_PROP = "indy.boot.defaults";

    public static final String HOME_PROP = "indy.home";

    public static void main( final String[] args )
    {
        setUncaughtExceptionHandler();
        BootOptions boot;
        try
        {
            boot = loadFromSysProps( "indy", BOOT_DEFAULTS_PROP, HOME_PROP );
        }
        catch ( final BootException e )
        {
            e.printStackTrace();
            System.err.printf( "ERROR: %s", e.getMessage() );
            System.exit( ERR_LOAD_BOOT_OPTIONS );
            return;
        }

        try
        {
            if ( boot.parseArgs( args ) )
            {
                Booter booter = new JaxRsBooter();
                booter.runAndWait( boot );
            }
        }
        catch ( final BootException e )
        {
            e.printStackTrace();
            System.err.printf( "ERROR: %s", e.getMessage() );
            System.exit( ERR_START );
        }
    }

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public void startLifecycle() throws AppLifecycleException
    {
        logger.info( "Start lifecycle..." );
        IndyLifecycleManager lifecycleManager = getContainer().select( IndyLifecycleManager.class ).get();

        try
        {
            lifecycleManager.start();
            Runtime.getRuntime().addShutdownHook( new Thread( lifecycleManager.createShutdownRunnable() ) );
        }
        catch ( final IndyLifecycleException e )
        {
            logger.error( "\n\nFailed to startLifecycle: " + e.getMessage(), e );
            throw new AppLifecycleException( "startLifecycle failed", e);
        }
    }

    @Override
    public void deploy() throws DeployException
    {
        IndyDeployer deployer = getContainer().select( IndyDeployer.class ).get();
        if ( deployer == null )
        {
            logger.warn( "No deployer found!" );
            return;
        }
        logger.info( "Deployer: {}", deployer.getClass() );
        deployer.deploy( options );
    }

}
