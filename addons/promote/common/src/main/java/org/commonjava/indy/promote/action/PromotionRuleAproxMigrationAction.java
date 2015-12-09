package org.commonjava.indy.promote.action;

import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.MigrationAction;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.promote.conf.PromoteConfig;
import org.commonjava.indy.promote.validate.PromoteValidationsManager;
import org.commonjava.indy.promote.validate.model.ValidationRuleMapping;
import org.commonjava.indy.subsys.datafile.DataFile;
import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;

/**
 * Migrate promotion validation rules from aprox/AProx/Aprox packages and naming to indy/Indy.
 */
public class PromotionRuleAproxMigrationAction
        implements MigrationAction
{
    @Inject
    private DataFileManager ffManager;

    @Inject
    private PromoteConfig config;

    @Override
    public boolean migrate()
            throws IndyLifecycleException
    {
        DataFile dataDir = ffManager.getDataFile( config.getBasedir(), PromoteValidationsManager.RULES_DIR );

        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( "Scanning {} for promotion validation rules...", dataDir );
        int changed = 0;
        if ( dataDir.exists() )
        {
            final DataFile[] scripts = dataDir.listFiles( ( pathname ) -> {
                logger.trace( "Checking for promote validation rule script in: {}", pathname );
                return pathname.getName().endsWith( ".groovy" );
            } );

            logger.info( "Migrating promotion validation rules." );
            for ( final DataFile script : scripts )
            {
                try
                {
                    String scriptContent = script.readString();
                    String migrated = scriptContent.replaceAll( "A[Pp]rox", "Indy" ).replaceAll( "aprox", "indy" );
                    if ( !migrated.equals( scriptContent ) )
                    {
                        logger.info( "Migrating promotion validation rule in: {}", script.getPath() );
                        script.writeString( migrated, new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                                                         "Migrating to Indy packages / naming" ) );
                        changed++;
                    }
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                }
            }
        }

        return changed != 0;
    }

    @Override
    public int getMigrationPriority()
    {
        return 90;
    }

    @Override
    public String getId()
    {
        return "aprox-promotion-rules";
    }
}
