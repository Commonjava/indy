package org.commonjava.aprox.depgraph.dto;

public class ExtraCT
{
    private String classifier;

    private String type;

    public String getClassifier()
    {
        return classifier;
    }

    public String getType()
    {
        return type == null ? "jar" : type;
    }

    public void setClassifier( final String classifier )
    {
        this.classifier = classifier;
    }

    public void setType( final String type )
    {
        this.type = type;
    }

}
