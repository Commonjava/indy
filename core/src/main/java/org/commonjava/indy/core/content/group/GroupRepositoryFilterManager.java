package org.commonjava.indy.core.content.group;

import org.commonjava.indy.content.GroupRepositoryFilter;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.subsys.datafile.DataFile;
import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.commonjava.indy.subsys.template.ScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.commonjava.indy.subsys.template.ScriptEngine.SCRIPTS_SUBDIR;

@ApplicationScoped
public class GroupRepositoryFilterManager
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public static final String REPO_FILTER = "repofilter"; // groovy scripts are under "data/scripts/repofilter"

    @Inject
    private DataFileManager dataFileManager;

    @Inject
    private ScriptEngine scriptEngine;

    @Inject
    private Instance<GroupRepositoryFilter> filters;

    private List<GroupRepositoryFilter> groupRepositoryFilters = new ArrayList<>();

    public GroupRepositoryFilterManager()
    {
    }

    @PostConstruct
    void setup()
    {
        if ( filters != null )
        {
            filters.forEach( f -> groupRepositoryFilters.add( f ) );
        }
        loadFilterScripts();
        Collections.sort( groupRepositoryFilters, Collections.reverseOrder() ); // priority is important
        logger.info( "Group repository filters: {}", groupRepositoryFilters );
    }

    private void loadFilterScripts()
    {
        DataFile filtersDir = dataFileManager.getDataFile( SCRIPTS_SUBDIR, REPO_FILTER );
        logger.info( "Scanning {} for repo filters", filtersDir );
        if ( filtersDir.exists() )
        {
            DataFile[] scripts = filtersDir.listFiles( ( file ) -> file.getName().endsWith( ".groovy" ) );
            if ( scripts != null && scripts.length > 0 )
            {
                for ( final DataFile script : scripts )
                {
                    logger.info( "Loading repo filter: {}", script );
                    GroupRepositoryFilter filter = parseFilter( script );
                    if ( filter != null )
                    {
                        groupRepositoryFilters.add( filter );
                    }
                }
            }
            else
            {
                logger.info( "No repo filter scripts found in {}", filtersDir.getPath() );
            }
        }
    }

    private GroupRepositoryFilter parseFilter( DataFile script )
    {
        try
        {
            String spec = script.readString();
            return scriptEngine.parseScriptInstance( spec, GroupRepositoryFilter.class );
        }
        catch ( Exception e )
        {
            logger.error( "Parse repo filter failed", e );
            return null;
        }
    }

    /**
     * Get member repositories that might contain the first occurrence of the specified path.
     *
     * @param path
     * @param group
     * @param orderedConcreteStores concrete stores in this group and sub groups
     */
    public List<ArtifactStore> filterForFirstMatch( String path, Group group,
                                                    List<ArtifactStore> orderedConcreteStores )
    {
        long begin = System.currentTimeMillis();
        List<ArtifactStore> ret = orderedConcreteStores;
        for ( GroupRepositoryFilter repositoryFilter : groupRepositoryFilters )
        {
            if ( repositoryFilter.canProcess( path, group ) )
            {
                ret = repositoryFilter.filterForFirstMatch( path, group, ret );
            }
        }
        if ( ret != orderedConcreteStores )
        {
            logger.debug( "Filter stores for first match (elapse: {}), original: {}, ret: {}",
                          ( System.currentTimeMillis() - begin ), orderedConcreteStores, ret );
        }
        return ret;
    }

    /**
     * Get member repositories that might contain the specified path.
     *
     * @param path
     * @param group
     * @param orderedConcreteStores concrete stores in this group and sub groups
     */
    public List<ArtifactStore> filter( String path, Group group, List<ArtifactStore> orderedConcreteStores )
    {
        long begin = System.currentTimeMillis();
        List<ArtifactStore> ret = orderedConcreteStores;
        for ( GroupRepositoryFilter repositoryFilter : groupRepositoryFilters )
        {
            if ( repositoryFilter.canProcess( path, group ) )
            {
                ret = repositoryFilter.filter( path, group, ret );
            }
        }
        if ( ret != orderedConcreteStores )
        {
            logger.debug( "Filter stores (elapse: {}), original: {}, ret: {}", ( System.currentTimeMillis() - begin ),
                          orderedConcreteStores, ret );
        }
        return ret;
    }

}
