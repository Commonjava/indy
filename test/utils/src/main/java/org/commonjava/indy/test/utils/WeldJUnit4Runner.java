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
package org.commonjava.indy.test.utils;

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

        // Use a "flat" deployment structure.
        // Bean archive isolation is supported (and enabled by default) from version 2.2.0.Final.
        // Previous versions only operated with the "flat" deployment structure.
        // ref http://docs.jboss.org/weld/reference/latest/en-US/html/environments.html#_bean_archive_isolation_2
        this.weld.property("org.jboss.weld.se.archive.isolation", false);

        this.container = weld.initialize();
        return container.select( getTestClass().getJavaClass() ).get();
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
