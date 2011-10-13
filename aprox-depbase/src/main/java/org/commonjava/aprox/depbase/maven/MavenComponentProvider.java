package org.commonjava.aprox.depbase.maven;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.mae.MAEException;
import org.apache.maven.mae.app.AbstractMAEApplication;
import org.apache.maven.mae.boot.embed.MAEEmbedderBuilder;
import org.apache.maven.model.building.ModelBuilder;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import com.google.inject.Provides;

@Singleton
@Component( role = MavenComponentProvider.class )
public class MavenComponentProvider
    extends AbstractMAEApplication
{

    @Requirement
    private ModelBuilder modelBuilder;

    @Provides
    @Named( "MAE" )
    public ModelBuilder getModelBuilder()
    {
        return modelBuilder;
    }

    @PostConstruct
    public void loadMAE()
    {
        try
        {
            super.load();
        }
        catch ( MAEException e )
        {
            throw new RuntimeException(
                                        "Failed to initialize MAE container. Cannot load Maven components. Reason: "
                                            + e.getMessage(), e );
        }
    }

    @Override
    public String getId()
    {
        return "DepBase-AProx-Bridge";
    }

    @Override
    public String getName()
    {
        return getId();
    }

    @Override
    protected void configureBuilder( final MAEEmbedderBuilder builder )
        throws MAEException
    {
        builder.withClassScanningEnabled( false );
        super.configureBuilder( builder );
    }

}
