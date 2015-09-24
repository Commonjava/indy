/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.aprox.promote.validate;

import org.apache.commons.lang.StringUtils;
import org.commonjava.aprox.content.ContentManager;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.promote.model.PromoteRequest;
import org.commonjava.aprox.promote.model.ValidationRuleSet;
import org.commonjava.aprox.promote.validate.model.ValidationRequest;
import org.commonjava.aprox.promote.model.ValidationResult;
import org.commonjava.aprox.promote.validate.model.ValidationRuleMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.util.List;

/**
 * Created by jdcasey on 9/11/15.
 */
public class PromotionValidator
{
    @Inject
    private PromoteValidationsManager validationsManager;

    @Inject
    private PromotionValidationTools validationTools;

    protected PromotionValidator()
    {
    }

    public PromotionValidator( PromoteValidationsManager validationsManager, PromotionValidationTools validationTools )
    {
        this.validationsManager = validationsManager;
        this.validationTools = validationTools;
    }

    public void validate( PromoteRequest request, ValidationResult result )
            throws PromotionValidationException
    {
        ValidationRuleSet set = validationsManager.getRuleSetMatching( request.getTargetKey() );

        Logger logger = LoggerFactory.getLogger( getClass() );
        if ( set != null )
        {
            logger.debug( "Running validation rule-set for promotion: {}", set.getName() );

            result.setRuleSet( set.getName() );
            List<String> ruleNames = set.getRuleNames();
            if ( ruleNames != null && !ruleNames.isEmpty() )
            {
                ValidationRequest req = new ValidationRequest( request, set, validationTools );

                for ( String ruleRef : ruleNames )
                {
                    String ruleName = new File( ruleRef ).getName(); // flatten in case some path fragment leaks in...

                    ValidationRuleMapping rule = validationsManager.getRuleMappingNamed( ruleName );
                    if ( rule != null )
                    {
                        try
                        {
                            logger.debug( "Running promotion validation rule: {}", rule.getName() );
                            String error = rule.getRule().validate( req );
                            if ( StringUtils.isNotEmpty( error ) )
                            {
                                logger.debug( "Failed" );
                                result.addValidatorError( rule.getName(), error );
                            }
                            else
                            {
                                logger.debug( "Succeeded" );
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
