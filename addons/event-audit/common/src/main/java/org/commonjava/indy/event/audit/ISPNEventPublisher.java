package org.commonjava.indy.event.audit;

import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
import org.commonjava.propulsor.content.audit.FileEventPublisher;
import org.commonjava.propulsor.content.audit.FileEventPublisherException;
import org.commonjava.propulsor.content.audit.model.FileEvent;
import org.commonjava.propulsor.content.audit.model.FileGroupingEvent;

import javax.inject.Inject;

public class ISPNEventPublisher implements FileEventPublisher
{

    @Inject
    @FileEventCache
    CacheHandle<String, FileEvent> fileEventCache;

    @Inject
    @FileGroupingEventCache
    CacheHandle<String, FileGroupingEvent> fileGroupingEventCache;

    @Override
    public void publishFileEvent( FileEvent fileEvent ) throws FileEventPublisherException
    {
        fileEventCache.put( fileEvent.getEventId().toString(), fileEvent );
    }

    @Override
    public void publishFileGroupingEvent( FileGroupingEvent fileGroupingEvent ) throws FileEventPublisherException
    {
        fileGroupingEventCache.put( fileGroupingEvent.getEventId().toString(), fileGroupingEvent );
    }
}
