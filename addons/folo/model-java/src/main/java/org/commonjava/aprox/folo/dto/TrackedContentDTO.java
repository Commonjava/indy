package org.commonjava.aprox.folo.dto;

import java.util.Set;

import org.commonjava.aprox.folo.model.TrackingKey;

public class TrackedContentDTO
{

    private TrackingKey key;

    private Set<TrackedContentEntryDTO> uploads;

    private Set<TrackedContentEntryDTO> downloads;

    public TrackedContentDTO()
    {
    }

    public TrackedContentDTO( final TrackingKey key, final Set<TrackedContentEntryDTO> uploads,
                              final Set<TrackedContentEntryDTO> downloads )
    {
        this.key = key;
        this.uploads = uploads;
        this.downloads = downloads;
    }

    public TrackingKey getKey()
    {
        return key;
    }

    public void setKey( final TrackingKey key )
    {
        this.key = key;
    }

    public Set<TrackedContentEntryDTO> getUploads()
    {
        return uploads;
    }

    public void setUploads( final Set<TrackedContentEntryDTO> uploads )
    {
        this.uploads = uploads;
    }

    public Set<TrackedContentEntryDTO> getDownloads()
    {
        return downloads;
    }

    public void setDownloads( final Set<TrackedContentEntryDTO> downloads )
    {
        this.downloads = downloads;
    }

}
