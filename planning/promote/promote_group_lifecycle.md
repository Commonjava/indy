title Promote lifecycle(through group)

#participantspacing equal

actor Client
#supported participant types: participant, actor, boundary, control, entity, database

participant PromoteResource 
participant PromotionManager
participant PromotionValidator
participant PromoteValidationManager
participant StoreDataManager
participant [groovy] validationRules 


Client->PromoteResource:<<Request>> "/groups/promote"->promoteToGroup <grpReq>

note over Client,PromoteResource:Client starts request \nwith json (auto converted to <<GroupPromoteRequest>>


rbox over PromoteResource:user = securityManager.getUser()\nbaseUrl = uriInfo.getBaseUriBuilder()\n          .path(ContentAccessResource.class)\n          .build(req.sourceType,req.sourceName)
note over PromoteResource: This baseUrl points to the request.Source

PromoteResource->PromotionManager:prmoteToGroup(grpReq,user,baseUrl)


PromotionManager->StoreDataManager:hasArtifactStore( grpReq.getSource() )
PromotionManager<-StoreDataManager:<<boolean>> exists
alt exists is true
PromotionManager->StoreDataManager:getArtifactStore( grpReq.getTargetKey() )
PromotionManager<-StoreDataManager:<<Group>> target




PromotionManager->PromotionValidator:validate(grpReq,validationResult,baseUrl)
PromotionValidator->PromoteValidationManager:getRuleSetMatching(grpReq.getTargetKey())
PromotionValidator<-PromoteValidationManager:<<ValidationRuleSet>> ruleSet

rbox over PromotionValidator:ruleNames = ruleSet.getRuleNames()

PromotionValidator->StoreDataManager:getArtifactStore( grpReq.getSource() )
PromotionValidator<-StoreDataManager:store

rbox over PromotionValidator:valReq = new ValidationRequest( grpReq, ruleSet, toos, store )

loop ruleRef in ruleNames
PromotionValidator->PromoteValidationManager:getRuleMappingNamed( ruleName )
PromotionValidator<-PromoteValidationManager:ruleMapping
PromotionValidator->[groovy] validationRules:ruleMapping.getRule.validate(valReq)
PromotionValidator<-[groovy] validationRules:<<String>> error
  alt error has content
  rbox over PromotionValidator: validationResult.add(rule, error)
  end
end



PromotionManager<-PromotionValidator:<<ValidaionResult>> validationResult

alt validationResult.isValid
rbox over PromotionManager: target.addConstituent( grpReq.getSource() )
end

end


PromoteResource<-PromotionManager:<<PathsPromoteResult>> promoteResult


Client<-PromoteResource:<<Response>>
