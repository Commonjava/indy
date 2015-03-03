package org.commonjava.aprox.autoprox.rest.dto;


public class RuleDTO
{

    private String name;

    private String spec;

    public RuleDTO()
    {
    }

    public RuleDTO( final String name, final String spec )
    {
        this.name = name;
        this.spec = spec;
    }

    public String getName()
    {
        return name;
    }

    public void setName( final String name )
    {
        this.name = name;
    }

    public String getSpec()
    {
        return spec;
    }

    public void setSpec( final String spec )
    {
        this.spec = spec;
    }

    @Override
    public String toString()
    {
        return String.format( "RuleDTO [name=%s]", name );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
        result = prime * result + ( ( spec == null ) ? 0 : spec.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final RuleDTO other = (RuleDTO) obj;
        if ( name == null )
        {
            if ( other.name != null )
            {
                return false;
            }
        }
        else if ( !name.equals( other.name ) )
        {
            return false;
        }
        if ( spec == null )
        {
            if ( other.spec != null )
            {
                return false;
            }
        }
        else if ( !spec.equals( other.spec ) )
        {
            return false;
        }
        return true;
    }

}
