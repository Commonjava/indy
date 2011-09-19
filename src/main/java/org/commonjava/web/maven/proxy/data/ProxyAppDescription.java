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
        ALL_GROUPS( "all-groups" ), ALL_REPOSITORIES( "all-repositories" ), GROUP_REPOSITORIES(
            "group-repositories" ), REPOSITORY_GROUPS( "repository-groups" );

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
