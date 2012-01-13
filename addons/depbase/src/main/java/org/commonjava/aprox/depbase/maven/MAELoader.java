package org.commonjava.aprox.depbase.maven;

import org.apache.maven.mae.MAEException;
import org.apache.maven.mae.app.AbstractMAEApplication;
import org.apache.maven.mae.boot.embed.MAEEmbedderBuilder;
import org.apache.maven.mae.internal.container.ComponentSelector;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.resolution.ModelResolver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Component( role = MAELoader.class )
public class MAELoader
    extends AbstractMAEApplication
{

    @Requirement
    private ModelBuilder modelBuilder;

    public MAELoader()
    {
        try
        {
            super.load();
        }
        catch ( final MAEException e )
        {
            throw new RuntimeException( "Failed to initialize MAE container. Cannot load Maven components. Reason: "
                + e.getMessage(), e );
        }
    }

    public ModelBuilder getModelBuilder()
    {
        return modelBuilder;
    }

    @Override
    public String getId()
    {
        return "depbase-aprox";
    }

    @Override
    public String getName()
    {
        return "DepBase-AProx-Bridge";
    }

    @Override
    protected void configureBuilder( final MAEEmbedderBuilder builder )
        throws MAEException
    {
        builder.withClassScanningEnabled( false );
        super.configureBuilder( builder );
    }

    @Override
    public ComponentSelector getComponentSelector()
    {
        return new ComponentSelector().setSelection( ModelResolver.class, "aprox" );
    }

}
