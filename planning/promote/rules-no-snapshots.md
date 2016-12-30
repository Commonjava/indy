title rules: no-snapshots.groovy

participant "PromotionValidator" as PV
participant "NoSnapshots" as NS
participant "PromotionValidationTools" as PVT
participant "[[galley]]MavenModelProcessor" as MMP
participant "[[galley]]MavenPomReader" as MPR
participant "[[atlas]]ArtifactPathInfo" as API

note right of PV:<<ValidationRequest>>request comes from \nnormal validation process, see validation\nprocess lifecycle for details
PV->NS:validate(request)

NS->PVT:getValidationStoreKeys(request, true)
rbox over PVT:verifyStroes.add(request.getValidationParameter( "availableInStores" ))
rbox over PVT:verifyStroes.add(request.getSourceRepository())
rbox over PVT:verifyStroes.add(request.getTarget())

NS<-PVT:verifyStoreKeys

rbox over NS:logBuilder=new StringBuilder\ndc:ModelProcessorConfig[buildSection=true, \n                        managedDependencies=true,\n                        managedPlugins=true]

loop path in request.getSourcePaths()
alt if path ends with ".pom"
NS->PVT:getArtifact(path)
PVT->API:parse(path)
API->PVT:<<ArtifactRef>>aref
PVT->NS:<<ArtifactRef>>ref
rbox over NS: add error log if ref is null
NS->PVT: getRelationshipsForPom(path, dc, request, verifyStoreKeys)
PVT->API:parse(path)
PVT<-API:<<ArtifactRef>>artifactRef
rbox over PVT:key = request.getSourceRepository().getKey();\ntransfer = retrieve( request.getSourceRepository(), path );
rbox over PVT: locations.add(transfer.getLocation);\nlocations.addAll([locations from verifyStoreKeys]);
PVT->MPR:read(artifactRef.asProjectVersionRef(), transfer, locations, "*"[ALL_PROFILES])
PVT<-MPR:<<MavenPomView>>pomView
PVT->MMP:readRelationships( pomView, uri[from source key], dc )
PVT<-MMP:<<EProjectDirectRelationships>>relation
NS<-PVT:<<Set<ProjectRelationship>>>relations

alt if reliations!=null
loop rel in relations
rbox over NS: <<ProjectVersionRef>>target = rel.getTarget()

alt if !target.getVersionSpec().isRelease()
rbox over NS: add error log
end

end
end


PV<-NS:<<String>>errorLog or null