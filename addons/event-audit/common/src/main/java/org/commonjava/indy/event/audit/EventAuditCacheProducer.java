package org.commonjava.indy.event.audit;

import org.commonjava.auditquery.fileevent.FileEvent;
import org.commonjava.auditquery.fileevent.FileGroupingEvent;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class EventAuditCacheProducer
{

    private final static String EVENT_AUDIT = "event-audit";

    private final static String GROUP_EVENT_AUDIT = "group-event-audit";

    @Inject
    CacheProducer cacheProducer;

    @FileEventCache
    @Produces
    @ApplicationScoped
    public CacheHandle<String, FileEvent> fileEventCacheCfg()
    {
        return cacheProducer.getCache( EVENT_AUDIT );
    }

    @FileGroupingEventCache
    @Produces
    @ApplicationScoped
    public CacheHandle<String, FileGroupingEvent> fileGroupingEventCacheCfg()
    {
        return cacheProducer.getCache( GROUP_EVENT_AUDIT );
    }
}
