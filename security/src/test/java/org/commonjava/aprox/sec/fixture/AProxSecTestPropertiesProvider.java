/*******************************************************************************
 * Copyright (C) 2011  John Casey
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see 
 * <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.sec.fixture;

import java.util.Properties;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.commonjava.couch.test.fixture.TestData;

@Singleton
public class AProxSecTestPropertiesProvider
{

    public static final String REPO_ROOT_DIR = "repo.root.dir";

    public static final String APROX_DATABASE_URL = "aprox.db.url";

    public static final String USER_DATABASE_URL = "user.db.url";

    @Produces
    @TestData
    public Properties getTestProperties()
    {
        final Properties props = new Properties();

        props.put( APROX_DATABASE_URL, "http://localhost:5984/test-aprox" );
        props.put( USER_DATABASE_URL, "http://localhost:5984/test-user" );
        props.put( REPO_ROOT_DIR, System.getProperty( REPO_ROOT_DIR, "target/repo-downloads" ) );

        return props;
    }

}
