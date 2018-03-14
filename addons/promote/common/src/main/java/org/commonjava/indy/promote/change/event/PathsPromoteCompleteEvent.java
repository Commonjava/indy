package org.commonjava.indy.promote.change.event;

import org.commonjava.indy.promote.model.PathsPromoteResult;

/**
 * Created by ruhan on 3/13/18.
 */
public class PathsPromoteCompleteEvent extends PromoteCompleteEvent
{

    private PathsPromoteResult promoteResult;

    public PathsPromoteCompleteEvent( PathsPromoteResult result )
    {
        this.promoteResult = result;
    }

    public PathsPromoteResult getPromoteResult()
    {
        return promoteResult;
    }

    @Override
    public String toString()
    {
        return "PathsPromoteCompleteEvent{" + "promoteResult=" + promoteResult + '}';
    }
}
