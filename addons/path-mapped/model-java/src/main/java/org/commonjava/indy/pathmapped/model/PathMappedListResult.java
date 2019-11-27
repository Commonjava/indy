package org.commonjava.indy.pathmapped.model;

public class PathMappedListResult
{
    private String packageType;

    private String type;

    private String name;

    private String path;

    private String[] list;

    public PathMappedListResult( String packageType, String type, String name, String path, String[] list )
    {
        this.packageType = packageType;
        this.type = type;
        this.name = name;
        this.path = path;
        this.list = list;
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

    public String[] getList()
    {
        return list;
    }

    public void setList( String[] list )
    {
        this.list = list;
    }
}
