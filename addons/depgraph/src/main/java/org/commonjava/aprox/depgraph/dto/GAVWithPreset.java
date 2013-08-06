package org.commonjava.aprox.depgraph.dto;

import org.commonjava.aprox.depgraph.json.GAVWithPresetSer;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.web.json.ser.JsonAdapters;

@JsonAdapters( GAVWithPresetSer.class )
public class GAVWithPreset
{

    private final ProjectVersionRef gav;

    private final String preset;

    public GAVWithPreset( final ProjectVersionRef gav, final String preset )
    {
        this.gav = gav;
        this.preset = preset;
    }

    public ProjectVersionRef getGAV()
    {
        return gav;
    }

    public String getPreset()
    {
        return preset;
    }

}
