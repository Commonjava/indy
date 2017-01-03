title rules: parsable-pom.groovy

participant "PromotionValidator" as PV
participant "ParsablePom" as PP
participant "PromotionValidationTools" as PVT
participant "[[galley]]MavenPomReader" as MPR
participant "[[atlas]]ArtifactPathInfo" as API

note right of PV:<<ValidationRequest>>request comes from \nnormal validation process, see validation\nprocess lifecycle for details
PV->PP:validate(request)

rbox over PP:logBuilder=new StringBuilder

loop path in request.getSourcePaths()
alt if path ends with ".pom"
PP->PVT:readLocalPom(it, request)
PVT->API:parse(path)
PVT<-API:<<ArtifactRef>>artifactRef
rbox over PVT:transfer = retrieve( request.getSourceRepository(), path );
PVT->MPR:readLocalPom(artifactRef.asProjectVersionRef(), transfer,  "*"[ALL_PROFILES])
PVT<-MPR:<<MavenPomView>>pomView
PP<-PVT:<<MavenPomView>>pomView
rbox over PP: add error log if any exceptions for above
end
end

PV<-PP:<<String>>errorLog or null