title rules: no-version-ranges.groovy

participant "PromotionValidator" as PV
participant "NoVersionRanges" as NVR
participant "PromotionValidationTools" as PVT
participant "[[galley]]MavenModelProcessor" as MMP
participant "[[atlas]]ArtifactPathInfo" as API

note right of PV:<<ValidationRequest>>request comes from \nnormal validation process, see validation\nprocess lifecycle for details
PV->NVR:validate(request)

NVR->PVT:getValidationStoreKeys(request, true)
rbox over PVT:verifyStroes.add(request.getValidationParameter( "availableInStores" ))
rbox over PVT:verifyStroes.add(request.getSourceRepository())
rbox over PVT:verifyStroes.add(request.getTarget())

NVR<-PVT:verifyStoreKeys

rbox over NVR:logBuilder=new StringBuilder\ndc:ModelProcessorConfig[buildSection=false, managedDependencies=false]

loop path in request.getSourcePaths()
alt if path ends with ".pom"
NVR->PVT: getRelationshipsForPom(path, dc, request, verifyStoreKeys)
PVT->API:parse(path)
PVT<-API:<<ArtifactRef>>artifactRef
rbox over PVT:key = request.getSourceRepository().getKey();\ntransfer = retrieve( request.getSourceRepository(), path );
rbox over PVT: locations.add(transfer.getLocation);\nlocations.addAll([locations from verifyStoreKeys]);
PVT->MMP:readRelationships( pomView, uri[from source key], dc )
PVT<-MMP:<<EProjectDirectRelationships>>relation
NVR<-PVT:<<Set<ProjectRelationship>>>relations

alt if reliations!=null
loop rel in relations
rbox over NVR: <<ProjectVersionRef>>target = rel.getTarget()

alt if !target.getVersionSpec().isRelease()
rbox over NVR: add error log
end

end
end


PV<-NVR:<<String>>errorLog or null