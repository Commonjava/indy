/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.test.utils;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

public class WeldJUnit4Runner
    extends BlockJUnit4ClassRunner
{

    private Weld weld;

    private WeldContainer container;

    public WeldJUnit4Runner( final Class<?> klass )
        throws InitializationError
    {
        super( klass );
    }

    @Override
    protected Object createTest()
        throws Exception
    {
        this.weld = new Weld();
        this.container = weld.initialize();
        return container.instance()
                        .select( getTestClass().getJavaClass() )
                        .get();
    }

    @Override
    public void run( final RunNotifier notifier )
    {
        try
        {
            super.run( notifier );
        }
        finally
        {
            if ( container != null )
            {
                weld.shutdown();
            }
        }
    }

}
