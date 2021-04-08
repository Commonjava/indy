package org.commonjava.indy.core.conf;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.propulsor.config.annotation.SectionName;
import org.commonjava.propulsor.config.section.MapSectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.io.InputStream;

@ApplicationScoped
@SectionName(IndyDurableStateConfig.SECTION_NAME)
public class IndyEventHandlerConfig
                extends MapSectionListener
                implements IndyConfigInfo
{

    public static final String SECTION_NAME = "event-handler";

    public static final String HANDLER_DEFAULT = "default";

    public static final String HANDLER_KAFKA = "kafka";

    private String fileEventHandler = HANDLER_DEFAULT;

    public String getFileEventHandler()
    {
        return fileEventHandler;
    }

    public void setFileEventHandler( String fileEventHandler )
    {
        this.fileEventHandler = fileEventHandler;
    }

    @Override
    public void parameter(String name, String value)
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.trace( "Got event-handler config parameter: '{}' with value: '{}'", name, value );
        switch ( name )
        {
            case "file.event.handler":
            {
                this.fileEventHandler = value;
                break;
            }
            default: break;
        }

    }

    @Override
    public String getDefaultConfigFileName()
    {
        return "conf.d/event-handler.conf";
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread()
                     .getContextClassLoader()
                     .getResourceAsStream( "default-event-handler.conf" );
    }
}
