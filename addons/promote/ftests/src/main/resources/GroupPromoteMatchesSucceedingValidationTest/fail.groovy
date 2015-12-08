package GroupPromoteMatchesSucceedingValidationTest

import org.commonjava.indy.promote.validate.model.ValidationRequest
import org.commonjava.indy.promote.validate.model.ValidationRule

class FailAll implements ValidationRule
{
    String validate( ValidationRequest request ) throws Exception
    {
        "No promotion for you!!! Come back two weeks!!!";
    }
}