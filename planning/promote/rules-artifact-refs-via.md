title rules: artifact-refs-via.groovy

participant "PromotionValidator" as PV
participant "ArtifactRefAvailability" as ARA
participant "PromotionValidationTools" as PVT
participant "[[galley]]MavenModelProcessor" as MMP
participant "[[galley]]MavenPomReader" as MPR
participant "[[atlas]]ArtifactPathInfo" as API

note right of PV:<<ValidationRequest>>request comes from \nnormal validation process, see validation\nprocess lifecycle for details
PV->ARA:validate(request)

ARA->PVT:getValidationStoreKeys(request, true)
rbox over PVT:verifyStroes.add(request.getValidationParameter( "availableInStores" ))
rbox over PVT:verifyStroes.add(request.getSourceRepository())
rbox over PVT:verifyStroes.add(request.getTarget())

ARA<-PVT:verifyStoreKeys

rbox over ARA:logBuilder=new StringBuilder\ndc:ModelProcessorConfig[buildSection=false, managedDependencies=false]

loop path in request.getSourcePaths()
alt if path ends with ".pom"
ARA->PVT: getRelationshipsForPom(path, dc, request, verifyStoreKeys)
PVT->API:parse(path)
PVT<-API:<<ArtifactRef>>artifactRef
rbox over PVT:key = request.getSourceRepository().getKey();\ntransfer = retrieve( request.getSourceRepository(), path );
rbox over PVT: locations.add(transfer.getLocation);\nlocations.addAll([locations from verifyStoreKeys]);
PVT->MPR:read(artifactRef.asProjectVersionRef(), transfer, locations, "*"[ALL_PROFILES])
PVT<-MPR:<<MavenPomView>>pomView
PVT->MMP:readRelationships( pomView, uri[from source key], dc )
PVT<-MMP:<<EProjectDirectRelationships>>relation
ARA<-PVT:<<Set<ProjectRelationship>>>relations
loop rel in relations
alt rel is not system and optional dependency
rbox over ARA:<<ProjectVersionRef>>target = rel.getTarget()
 
ARA->PVT:toArtifactPath(target)
ARA<-PVT:<<String>>path
ARA->PVT:toArtifactPath(target.asPomArtifact())
ARA<-PVT:pomPath<<String>>

rbox over ARA:found=false;\nfoundPom=false;

loop storeKey in verifyStoreKeys

alt found is false
ARA->PVT:getTransfer(storeKey, path)
ARA<-PVT:<<Transfer>>transfer
rbox over ARA:  found=true if transfer exists
end

alt foundPom is false 
ARA->PVT:getTransfer(storeKey, pomPath)
ARA<-PVT:<<Transfer>>transfer
rbox over ARA: foundPom=true if transfer exists
end

rbox over ARA:add error log in logBuilder if found or foundPom is true

end
end
end
end
end


PV<-ARA:<<String>>errorLog or null