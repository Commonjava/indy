package org.commonjava.indy.core.expire;

import java.util.Date;

/**
 * Created by jdcasey on 1/4/16.
 */
public class Expiration
{

    private String name;

    private String group;

    private Date expiration;

    public Expiration(){}

    public Expiration( String group, String name, Date expiration )
    {
        this.group = group;
        this.name = name;
        this.expiration = expiration;
    }

    public Expiration( String group, String name )
    {
        this.group = group;
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getGroup()
    {
        return group;
    }

    public void setGroup( String group )
    {
        this.group = group;
    }

    public Date getExpiration()
    {
        return expiration;
    }

    public void setExpiration( Date expiration )
    {
        this.expiration = expiration;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof Expiration ) )
        {
            return false;
        }

        Expiration that = (Expiration) o;

        if ( getName() != null ? !getName().equals( that.getName() ) : that.getName() != null )
        {
            return false;
        }
        return !( getGroup() != null ? !getGroup().equals( that.getGroup() ) : that.getGroup() != null );

    }

    @Override
    public int hashCode()
    {
        int result = getName() != null ? getName().hashCode() : 0;
        result = 31 * result + ( getGroup() != null ? getGroup().hashCode() : 0 );
        return result;
    }

    @Override
    public String toString()
    {
        return "Expiration{" +
                "name='" + name + '\'' +
                ", group='" + group + '\'' +
                ", expiration=" + expiration +
                '}';
    }
}
