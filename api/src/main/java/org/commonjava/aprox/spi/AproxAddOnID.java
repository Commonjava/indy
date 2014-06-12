package org.commonjava.aprox.spi;

import java.util.ArrayList;
import java.util.List;

import org.commonjava.aprox.dto.UISection;

public final class AproxAddOnID
    implements Comparable<AproxAddOnID>
{

    private String name;

    private List<UISection> sections;

    private String initJavascriptHref;

    public String getInitJavascriptHref()
    {
        return initJavascriptHref;
    }

    public void setInitJavascriptHref( final String initJavascriptHref )
    {
        this.initJavascriptHref = initJavascriptHref;
    }

    public AproxAddOnID withInitJavascriptHref( final String initJavascriptHref )
    {
        this.initJavascriptHref = initJavascriptHref;
        return this;
    }

    public String getName()
    {
        return name;
    }

    public void setName( final String name )
    {
        this.name = name;
    }

    public AproxAddOnID withName( final String name )
    {
        this.name = name;
        return this;
    }

    public List<UISection> getSections()
    {
        return sections;
    }

    public void setSections( final List<UISection> sections )
    {
        this.sections = sections;
    }

    public AproxAddOnID withUISection( final UISection uiSection )
    {
        if ( sections == null )
        {
            sections = new ArrayList<UISection>();
        }

        sections.add( uiSection );

        return this;
    }

    @Override
    public String toString()
    {
        return "AproxAddOnID [" + name + "]";
    }

    @Override
    public int compareTo( final AproxAddOnID other )
    {
        return name.compareTo( other.name );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
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
        final AproxAddOnID other = (AproxAddOnID) obj;
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
        return true;
    }
}
