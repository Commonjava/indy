package org.commonjava.aprox.promote.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by jdcasey on 9/11/15.
 */
public class ValidationRuleSet
{
    private String name;

    private String storeKeyPattern;

    private List<String> ruleNames;

    private Map<String, String> validationParameters;

    public ValidationRuleSet(){}

    public ValidationRuleSet( String name, String storeKeyPattern, List<String> ruleNames, Map<String, String> validationParameters )
    {
        this.name = name;
        this.storeKeyPattern = storeKeyPattern;
        this.ruleNames = ruleNames;
        this.validationParameters = validationParameters;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        ValidationRuleSet that = (ValidationRuleSet) o;

        if ( !getName().equals( that.getName() ) )
        {
            return false;
        }
        if ( !getStoreKeyPattern().equals( that.getStoreKeyPattern() ) )
        {
            return false;
        }
        return !( getRuleNames() != null ?
                !getRuleNames().equals( that.getRuleNames() ) :
                that.getRuleNames() != null );

    }

    @Override
    public int hashCode()
    {
        int result = getName().hashCode();
        result = 31 * result + getStoreKeyPattern().hashCode();
        result = 31 * result + ( getRuleNames() != null ? getRuleNames().hashCode() : 0 );
        return result;
    }

    public String getName()
    {

        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getStoreKeyPattern()
    {
        return storeKeyPattern;
    }

    public void setStoreKeyPattern( String storeKeyPattern )
    {
        this.storeKeyPattern = storeKeyPattern;
    }

    public List<String> getRuleNames()
    {
        return ruleNames;
    }

    public void setRuleNames( List<String> ruleNames )
    {
        this.ruleNames = ruleNames;
    }

    public boolean matchesKey( String keyStr )
    {
        return storeKeyPattern == null || keyStr.matches( storeKeyPattern );
    }

    public Map<String, String> getValidationParameters()
    {
        return validationParameters;
    }

    public void setValidationParameters( Map<String, String> validationParameters )
    {
        this.validationParameters = validationParameters;
    }
}
