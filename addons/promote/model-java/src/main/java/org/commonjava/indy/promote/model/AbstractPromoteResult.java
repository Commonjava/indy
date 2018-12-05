package org.commonjava.indy.promote.model;

import io.swagger.annotations.ApiModelProperty;

import java.util.UUID;

/**
 * Created by ruhan on 12/5/18.
 */
public class AbstractPromoteResult<T extends AbstractPromoteResult>
{
    //public static final String DONE = "DONE";

    public static final String DONE = "DONE";

    public static final String ACCEPTED = "ACCEPTED";

    @ApiModelProperty( "Unique promotion Id" )
    protected String promotionId = UUID.randomUUID().toString(); // default

    @ApiModelProperty( "Result code" )
    protected String resultCode = DONE; // default

    public String getPromotionId()
    {
        return promotionId;
    }

    public void setPromotionId( String promotionId )
    {
        this.promotionId = promotionId;
    }

    public String getResultCode()
    {
        return resultCode;
    }

    public void setResultCode( String resultCode )
    {
        this.resultCode = resultCode;
    }

    public T accepted()
    {
        this.resultCode = ACCEPTED;
        return (T) this;
    }
}
