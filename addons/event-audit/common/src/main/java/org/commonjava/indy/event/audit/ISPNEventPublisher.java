package org.commonjava.indy.event.audit;

import org.commonjava.auditquery.fileevent.FileEvent;
import org.commonjava.auditquery.fileevent.FileEventPublisher;
import org.commonjava.auditquery.fileevent.FileEventPublisherException;
import org.commonjava.auditquery.fileevent.FileGroupingEvent;
import org.commonjava.indy.subsys.infinispan.CacheHandle;

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
