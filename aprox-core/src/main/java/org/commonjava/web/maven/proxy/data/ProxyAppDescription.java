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
package org.commonjava.web.maven.proxy.data;

import java.util.HashSet;
import java.util.Set;

import org.commonjava.couch.db.model.AppDescription;

public class ProxyAppDescription
    implements AppDescription
{

    public static final String APP_NAME = "proxy-logic";

    public enum View
    {
        ALL_GROUPS( "all-groups" ), ALL_REPOSITORIES( "all-repositories" ), ALL_DEPLOY_POINTS(
            "all-deploy-points" ), GROUP_STORES( "group-stores" ), STORE_GROUPS( "store-groups" );

        String name;

        private View( final String name )
        {
            this.name = name;
        }

        public String viewName()
        {
            return name;
        }
    }

    private static Set<String> viewNames;

    @Override
    public String getAppName()
    {
        return APP_NAME;
    }

    @Override
    public String getClasspathAppResource()
    {
        return APP_NAME;
    }

    @Override
    public Set<String> getViewNames()
    {
        if ( viewNames == null )
        {
            Set<String> names = new HashSet<String>();
            for ( View view : View.values() )
            {
                names.add( view.viewName() );
            }

            viewNames = names;
        }

        return viewNames;
    }

}
