title Promote lifecycle(through paths)

#participantspacing equal

actor Client
#supported participant types: participant, actor, boundary, control, entity, database

participant PromoteResource 
participant PromotionManager
participant PromotionValidator
participant PromoteValidationManager
participant StoreDataManager
participant [groovy] validationRules 


Client->PromoteResource:<<Request>> "/paths/promote"->promotePaths

note over Client,PromoteResource:Client starts request \nwith json stream

rbox over PromoteResource:<<PathsPromoteRequest>> \npathsReq = mapper.readValue(stream)

rbox over PromoteResource:baseUrl = uriInfo.getBaseUriBuilder()\n          .path(ContentAccessResource.class)\n          .build(req.sourceType,req.sourceName)
note over PromoteResource: This baseUrl points to the request.Source

PromoteResource->PromotionManager:prmotePaths(pathReq,baseUrl)

rbox over PromotionManager:paths = pathReq.getPaths \nsource = pathReq.getSource

alt paths empty
rbox over PromotionManager:downloadManager.listRecursively( source, "/")
else
rbox over PromotionManager: getTransfersForPaths( source, paths )
end

note over PromotionManager: get transfers based on paths

loop transfers
rbox over PromotionManager:pending.add(transfer.getPath)
end


PromotionManager->PromotionValidator:validate(pathReq,validationResult,baseUrl)
PromotionValidator->PromoteValidationManager:getRuleSetMatching(pathReq.getTargetKey())
PromotionValidator<-PromoteValidationManager:<<ValidationRuleSet>> ruleSet

rbox over PromotionValidator:ruleNames = ruleSet.getRuleNames()

alt pathReq.getPaths is not empty
rbox over PromotionValidator:tempRemote = new RemoteRepository(  "Promote_tmp_" + sourceName, baseUrl )\ntempRemote.setPathMaskPatterns( pathsReq.getPaths() )\nstore=tempRemote
PromotionValidator->StoreDataManager:storeArtifactStore( store, summary)
PromotionValidator<-StoreDataManager:<<success>>
else
PromotionValidator->StoreDataManager:getArtifactStore( pathsReq.getSource() )
PromotionValidator<-StoreDataManager:store
end

rbox over PromotionValidator:valReq = new ValidationRequest( pathsReq, ruleSet, toos, store )

loop ruleRef in ruleNames
PromotionValidator->PromoteValidationManager:getRuleMappingNamed( ruleName )
PromotionValidator<-PromoteValidationManager:ruleMapping
PromotionValidator->[groovy] validationRules:ruleMapping.getRule.validate(valReq)
PromotionValidator<-[groovy] validationRules:<<String>> error
  alt error has content
  rbox over PromotionValidator: validationResult.add(rule, error)
  end
end


alt pathReq.getPaths is not empty
PromotionValidator->StoreDataManager:deleteArtifactStore( store, system_user, summary)
end

PromotionManager<-PromotionValidator:<<ValidaionResult>> validationResult

alt validationResult.isValid
  par runPathsPromotion(pathsReq, pending, empty, empty, transfers)
    loop transfer in transfers
    rbox over PromotionManager:<<Transfer>>\n   tgtTxr = contentManager.getTransfer( targetStore, transfer.path, UPLOAD );
      alt tgtTxr not exists
      rbox over PromotionManager:contentManager.store( targetStore, transfer.path, transfer.inputstream, UPLOAD, eventMetadata)
      end
    end
  end
end

PromoteResource<-PromotionManager:<<PathsPromoteResult>> promoteResult


Client<-PromoteResource:<<Response>>
