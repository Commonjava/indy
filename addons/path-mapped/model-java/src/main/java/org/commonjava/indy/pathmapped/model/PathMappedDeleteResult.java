package org.commonjava.indy.pathmapped.model;

public class PathMappedDeleteResult
{
    private String packageType;

    private String type;

    private String name;

    private String path;

    boolean result;

    public PathMappedDeleteResult()
    {
    }

    public PathMappedDeleteResult( String packageType, String type, String name, String path, boolean result )
    {
        this.packageType = packageType;
        this.type = type;
        this.name = name;
        this.path = path;
        this.result = result;
    }

    public String getPackageType()
    {
        return packageType;
    }

    public void setPackageType( String packageType )
    {
        this.packageType = packageType;
    }

    public String getType()
    {
        return type;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath( String path )
    {
        this.path = path;
    }

    public boolean isResult()
    {
        return result;
    }

    public void setResult( boolean result )
    {
        this.result = result;
    }
}
