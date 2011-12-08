/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.couch.data;

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
