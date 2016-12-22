title Schedule expiration lifecycle

#participantspacing equal

actor "Client" as CET
#supported participant types: participant, actor, boundary, control, entity, database

participant "<<JAX-RS>>StoreAdminHandler" as SAH
participant "AdminController" as AC
participant "MemoryStoreDataManager" as MSDM
participant "DefaultStoreEventDispatcher" as DSED
participant "TimeoutEventListener" as TEL
participant "ScheduleManager" as SM
participant "Infinispan" as ISPN
participant "CDIEventDispatcher" as CED
participant "ContentIndexObserver" as CIO



CET->SAH:<<Req:POST>> "/admin/{type: (hosted|group|remote)}/{name}"->create
note over CET,SAH:Client starts request \nwith json (auto converted to <<ArtifactStore>> store

rbox over SAH:user = securityManager.getUser()

SAH->AC:store(store,user,false)


AC->MSDM:storeArtifactStore(store, sum, false, metadata)

opt store(store, sum, false, true, metadata)
rbox over MSDM: preStore()
opt postStore()
MSDM->DSED:updated(type:UPDATE, meta, stores)
DSED->TEL:onStoreUpdate(event)
end
end

alt storeType:hosted
TEL->SM: rescheduleSnapshotTimeouts(store)
alt store.isAllowSnapshots
rbox over SM:  timeout= store.getSnapshotTimeoutSeconds
else
rbox over SM: timeout=-1
end
else storeType:remote
TEL->SM: rescheduleProxyTimeouts(store)
alt store.isPassthrough
rbox over SM: timeout= passthrough [system config]
else
rbox over SM:timeout=store.getCacheTimeoutSeconds
end
end

opt scheduleForStore(storeKey, jobType, path, payload[ContentExpiration], timeout)

rbox over SM: dataMap{"PAYLOAD":payload[json], "SCHEDULE_TIME":System.currentTimeMillis()}
rbox over SM: cacheKey = new new ScheduleKey( key, jobType, jobName[path] );
SM->ISPN:cache.put( cacheKey, dataMap, timeout, TimeUnit.SECONDS ) 

end


rbox over SM: wait for timeout

ISPN-->SM:expired(exprireEvent)

SM->CED:fire(<<SchedulerTriggerEvent>> event)

CED-->TEL:onExpirationEvent(event)
rbox over TEL:store storeManager.getArtifactStore( key );\nfileManager.delete( store, path, metadata);

CED-->CIO:invalidateExpiredContent(event)
rbox over CIO: remove path for store in index cache
