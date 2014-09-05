package org.commonjava.aprox.subsys.flatfile.conf;

public class ChangeSummary
{

    private final String changedBy;

    private final String description;

    public ChangeSummary( final String changedBy, final String description )
    {
        this.changedBy = changedBy;
        this.description = description;
    }

    public String getChangedBy()
    {
        return changedBy;
    }

    public String getDescription()
    {
        return description;
    }

    @Override
    public String toString()
    {
        return String.format( "ChangeSummary [changedBy=%s, description=%s]", changedBy, description );
    }

}
