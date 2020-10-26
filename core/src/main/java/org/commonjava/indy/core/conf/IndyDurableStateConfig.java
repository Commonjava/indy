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
public class IndyDurableStateConfig
                extends MapSectionListener
                implements IndyConfigInfo
{

    public static final String SECTION_NAME = "durable-state";

    public static final String STORAGE_INFINISPAN = "infinispan";

    public static final String STORAGE_CASSANDRA = "cassandra";

    private String foloStorage;

    private String storeStorage;

    private String scheduleStorage;

    public String getFoloStorage()
    {
        return foloStorage;
    }

    public void setFoloStorage( String foloStorage )
    {
        this.foloStorage = foloStorage;
    }

    public String getStoreStorage()
    {
        return storeStorage;
    }

    public void setStoreStorage( String storeStorage )
    {
        this.storeStorage = storeStorage;
    }

    public String getScheduleStorage()
    {
        return scheduleStorage;
    }

    public void setScheduleStorage( String scheduleStorage )
    {
        this.scheduleStorage = scheduleStorage;
    }

    @Override
    public void parameter(String name, String value)
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.trace( "Got durable-state config parameter: '{}' with value: '{}'", name, value );
        switch ( name )
        {
            case "folo.storage":
            {
                this.foloStorage = value;
                break;
            }
            case "store.storage":
            {
                this.storeStorage = value;
                break;
            }
            case "schedule.storage":
            {
                this.scheduleStorage = value;
                break;
            }
            default: break;
        }

    }

    @Override
    public String getDefaultConfigFileName()
    {
        return "conf.d/durable-state.conf";
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread()
                     .getContextClassLoader()
                     .getResourceAsStream( "default-durable-state.conf" );
    }
}
