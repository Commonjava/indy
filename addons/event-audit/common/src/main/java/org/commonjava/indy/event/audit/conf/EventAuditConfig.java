package org.commonjava.indy.event.audit.conf;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.InputStream;

@SectionName("event-audit")
@ApplicationScoped
public class EventAuditConfig implements IndyConfigInfo
{

    public static final boolean DEFAULT_ENABLED = true;

    private Boolean enabled;

    public EventAuditConfig()
    {

    }

    public boolean isEnabled()
    {
        return enabled == null ? DEFAULT_ENABLED : enabled;
    }

    public Boolean getEnabled()
    {
        return enabled;
    }

    @ConfigName( "enabled")
    public void setEnabled( final boolean enabled )
    {
        this.enabled = enabled;
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return new File( IndyConfigInfo.CONF_INCLUDES_DIR, "event-audit.conf" ).getPath();
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return null;
    }
}
