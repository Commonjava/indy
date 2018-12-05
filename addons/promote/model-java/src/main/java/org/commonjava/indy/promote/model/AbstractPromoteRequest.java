package org.commonjava.indy.promote.model;

import io.swagger.annotations.ApiModelProperty;

/**
 * Created by ruhan on 12/5/18.
 */
public abstract class AbstractPromoteRequest<T extends PromoteRequest> implements PromoteRequest
{
    @ApiModelProperty( value="Asynchronous call. A callback url is needed when it is true." )
    protected boolean async;

    @ApiModelProperty( value="Callback Url which is used to send the promotion result." )
    protected String callbackUrl;

    @Override
    public boolean isAsync()
    {
        return async;
    }

    @Override
    public String getCallbackUrl()
    {
        return callbackUrl;
    }

    public void setAsync( boolean async )
    {
        this.async = async;
    }

    public void setCallbackUrl( String callbackUrl )
    {
        this.callbackUrl = callbackUrl;
    }
}
