package org.commonjava.aprox.folo.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.aprox.action.AproxLifecycleException;
import org.commonjava.aprox.action.StartupAction;
import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.subsys.datafile.DataFile;
import org.commonjava.aprox.subsys.datafile.DataFileManager;

@Named
public class FoloLifecycleParticipant
    implements StartupAction
{

    private static final String FOLO_ID = "folo";

    private static final String FOLO_DIRECTORY_IGNORE = "folo";

    @Inject
    private DataFileManager dataFileManager;

    protected FoloLifecycleParticipant()
    {
    }

    public FoloLifecycleParticipant( final DataFileManager dataFileManager )
    {
        this.dataFileManager = dataFileManager;
    }

    @Override
    public String getId()
    {
        return FOLO_ID;
    }

    @Override
    public int getStartupPriority()
    {
        return 40;
    }

    @Override
    public void start()
        throws AproxLifecycleException
    {
        try
        {
            final DataFile dataFile = dataFileManager.getDataFile( ".gitignore" );
            final List<String> lines = dataFile.exists() ? dataFile.readLines() : new ArrayList<String>();
            if ( !lines.contains( FOLO_DIRECTORY_IGNORE ) )
            {
                lines.add( FOLO_DIRECTORY_IGNORE );

                dataFile.writeLines( lines, new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                                               "Adding artimon to ignored list." ) );
            }
        }
        catch ( final IOException e )
        {
            throw new AproxLifecycleException(
                                               "Failed while attempting to access .gitignore for data directory (trying to add artimon dir to ignores list).",
                                               e );
        }
    }

}
