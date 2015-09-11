package org.commonjava.aprox.promote.validate.model;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.content.StoreResource;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.promote.model.PathsPromoteRequest;
import org.commonjava.aprox.promote.model.PathsPromoteResult;
import org.commonjava.aprox.promote.model.PromoteRequest;
import org.commonjava.aprox.promote.model.ValidationRuleSet;
import org.commonjava.aprox.promote.validate.PromotionValidationException;
import org.commonjava.aprox.promote.validate.PromotionValidationTools;

import java.util.List;
import java.util.Set;

/**
 * Created by jdcasey on 9/11/15.
 */
public class ValidationRequest
{
    private final PromoteRequest promoteRequest;

    private final ValidationRuleSet ruleSet;

    private final PromotionValidationTools tools;

    private Set<String> requestPaths;

    public ValidationRequest( PromoteRequest promoteRequest, ValidationRuleSet ruleSet, PromotionValidationTools tools )
    {
        this.promoteRequest = promoteRequest;
        this.ruleSet = ruleSet;
        this.tools = tools;
    }

    public synchronized Set<String> getSourcePaths()
            throws PromotionValidationException
    {
        if ( requestPaths == null )
        {
            Set<String> paths = null;
            if ( promoteRequest instanceof PathsPromoteRequest )
            {
                paths = ((PathsPromoteRequest)promoteRequest).getPaths();
            }

            if ( paths == null )
            {
                ArtifactStore store = null;
                try
                {
                    store = tools.getArtifactStore( promoteRequest.getSource() );
                }
                catch ( AproxDataException e )
                {
                    throw new PromotionValidationException( "Failed to retrieve source ArtifactStore: {}. Reason: {}", e,
                                                            promoteRequest.getSource(), e.getMessage() );
                }

                if ( store != null )
                {
                    try
                    {
                        List<StoreResource> listing = tools.list( store, "/" );
                        if ( listing != null )
                        {
                            for ( StoreResource res : listing )
                            {
                                paths.add( res.getPath() );
                            }
                        }
                    }
                    catch ( AproxWorkflowException e )
                    {
                        throw new PromotionValidationException( "Failed to list paths in source: {}. Reason: {}", e,
                                                                promoteRequest.getSource(), e.getMessage() );
                    }
                }
            }
            requestPaths = paths;
        }

        return requestPaths;
    }

    public PromoteRequest getPromoteRequest()
    {
        return promoteRequest;
    }

    public ValidationRuleSet getRuleSet()
    {
        return ruleSet;
    }

    public PromotionValidationTools getTools()
    {
        return tools;
    }
}
