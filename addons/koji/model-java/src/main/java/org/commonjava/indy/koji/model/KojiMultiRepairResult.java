package org.commonjava.indy.koji.model;

import io.swagger.annotations.ApiModelProperty;

import java.util.List;

public class KojiMultiRepairResult
{
    @ApiModelProperty( "Results for all stores where repair was attempted, including failures" )
    private List<KojiRepairResult> results;

    public List<KojiRepairResult> getResults()
    {
        return results;
    }

    public void setResults( final List<KojiRepairResult> results )
    {
        this.results = results;
    }
}
