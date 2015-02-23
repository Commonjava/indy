package org.commonjava.aprox.revisions.jaxrs.dto;

import java.util.List;

import org.commonjava.aprox.audit.ChangeSummary;

public class ChangeSummaryDTO
{

    private final List<ChangeSummary> items;

    public ChangeSummaryDTO( final List<ChangeSummary> items )
    {
        this.items = items;
    }

    public List<ChangeSummary> getItems()
    {
        return items;
    }

}
