package org.commonjava.indy.boot.jaxrs;

import org.commonjava.propulsor.deploy.resteasy.ResteasyAppConfig;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;

import static java.util.Arrays.asList;

@ApplicationScoped
public class IndyResteasyAppConfig implements ResteasyAppConfig
{

    @Override
    public List<String> getJaxRsMappings()
    {
        return asList( "/api*", "/api/*" );
    }
}
