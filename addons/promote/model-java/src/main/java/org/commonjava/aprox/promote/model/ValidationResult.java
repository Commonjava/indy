package org.commonjava.aprox.promote.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jdcasey on 9/11/15.
 */
public class ValidationResult
{
    private boolean valid = true;

    private Map<String, String> validatorErrors = new HashMap<>();

    public void addValidatorError( String validatorName, String message )
    {
        valid = false;
        validatorErrors.put( validatorName, message );
    }

    public boolean isValid()
    {
        return valid;
    }

    public void setValid( boolean valid )
    {
        this.valid = valid;
    }

    public Map<String, String> getValidatorErrors()
    {
        return validatorErrors;
    }

    public void setValidatorErrors( Map<String, String> validatorErrors )
    {
        this.validatorErrors = validatorErrors;
    }
}
