title rules: project-artifacts.groovy

participant "PromotionValidator" as PV
participant "ProjectArtifacts" as PA
participant "PromotionValidationTools" as PVT
participant "[[atlas]]ArtifactPathInfo" as API

note right of PV:<<ValidationRequest>>request comes from \nnormal validation process, see validation\nprocess lifecycle for details
PV->PA:validate(request)

rbox over PA:classifierAndTypeSet = request.getValidationParameter("classifierAndTypeSet")\nlogBuilder=new StringBuilder

rbox over PA:tcs=[new SimpleTypeAndClassifier("$classifier", "$type")/*]
note over PA:construct a array "tcs" with [atlas]SimpleTypeAndClassifier. \nThe $classifier and $type are from "classifierAndTypeSet"

rbox over PA: projectTCs = [:]

loop path in request.getSourcePaths()
PA->PVT:getArtifact(path)
PVT->API:parse(path)
PVT<-API:<<ArtifactRef>>artifactRef
PA<-PVT:<<ArtifactRef>>ref
rbox over PA:gav = ref.asProjectVersionRef()\nprojectTCs[gav] = []\nfound << ref.getTypeAndClassifier()
end

loop entry in projectTCs
loop tc in tcs
alt !entry.value.contains(tc)
rbox over PA:add error log to logBuilder
end
end
end


PV<-PA:<<String>>errorLog or null