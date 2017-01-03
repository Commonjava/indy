title rules: no-pre-existing-paths.groovy

participant "PromotionValidator" as PV
participant "NoPreExistingPaths" as NPEP
participant "PromotionValidationTools" as PVT
participant "[[atlas]]ArtifactPathInfo" as API

note right of PV:<<ValidationRequest>>request comes from \nnormal validation process, see validation\nprocess lifecycle for details
PV->NPEP:validate(request)

NPEP->PVT:getValidationStoreKeys(request, true)
rbox over PVT:verifyStroes.add(request.getValidationParameter( "availableInStores" ))
rbox over PVT:verifyStroes.add(request.getSourceRepository())
rbox over PVT:verifyStroes.add(request.getTarget())

NPEP<-PVT:verifyStoreKeys

rbox over NPEP:logBuilder=new StringBuilder

loop path in request.getSourcePaths()

NPEP->PVT:getArtifact(path)
PVT->API:parse(path)
PVT<-API:<<ArtifactRef>>artifactRef
NPEP<-PVT:<<ArtifactRef>>aref
alt aref exists
loop storeKey in verifyStoreKeys

NPEP->PVT:exists(storeKey, path)
NPEP<-PVT:exists

rbox over NPEP:add error log in logBuilder if exists is true

end
end
end

PV<-NPEP:<<String>>errorLog or null