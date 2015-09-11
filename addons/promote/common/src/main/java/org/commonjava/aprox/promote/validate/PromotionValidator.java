package org.commonjava.aprox.promote.validate;

import org.commonjava.aprox.content.ContentManager;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.promote.model.PromoteRequest;
import org.commonjava.aprox.promote.model.ValidationRuleSet;
import org.commonjava.aprox.promote.validate.model.ValidationRequest;
import org.commonjava.aprox.promote.model.ValidationResult;
import org.commonjava.aprox.promote.validate.model.ValidationRuleMapping;

import javax.inject.Inject;
import java.util.List;

/**
 * Created by jdcasey on 9/11/15.
 */
public class PromotionValidator
{
    @Inject
    private ContentManager contentManager;

    @Inject
    private PromoteValidationsManager validationsManager;

    @Inject
    private StoreDataManager storeDataManager;

    protected PromotionValidator(){}

    public PromotionValidator( ContentManager contentManager, StoreDataManager storeDataManager, PromoteValidationsManager validationsManager )
    {
        this.contentManager = contentManager;
        this.storeDataManager = storeDataManager;
        this.validationsManager = validationsManager;
    }

    public void validate( PromoteRequest request, ValidationResult result )
            throws PromotionValidationException
    {
        ValidationRuleSet set =
                validationsManager.getRuleSetMatching( request.getTargetKey() );

        if ( set != null )
        {
            List<String> ruleNames = set.getRuleNames();
            if ( ruleNames != null && !ruleNames.isEmpty())
            {
                ValidationRequest req =
                        new ValidationRequest( request, set, new PromotionValidationTools( contentManager, storeDataManager ) );

                for ( String ruleName : ruleNames )
                {
                    ValidationRuleMapping rule = validationsManager.getRuleMappingNamed( ruleName );
                    if ( rule != null )
                    {
                        try
                        {
                            String error = rule.getRule().validate( req );
                            if ( error != null )
                            {
                                result.addValidatorError( rule.getName(), error );
                            }
                        }
                        catch ( Exception e )
                        {
                            if ( e instanceof PromotionValidationException )
                            {
                                throw (PromotionValidationException) e;
                            }

                            throw new PromotionValidationException(
                                    "Failed to run validation rule: {} for request: {}. Reason: {}", e, rule.getName(),
                                    request, e );
                        }
                    }
                }
            }
        }
    }
}
