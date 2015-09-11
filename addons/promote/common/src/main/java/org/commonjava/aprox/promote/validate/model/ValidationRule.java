package org.commonjava.aprox.promote.validate.model;

import org.commonjava.aprox.promote.model.ValidationResult;

/**
 * Created by jdcasey on 9/11/15.
 */
public interface ValidationRule
{
    // throws clause left intentionally open to simplify implementation with minimal attention to exception flavors.
    String validate( ValidationRequest request ) throws Exception;
}
