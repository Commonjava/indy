title PUT /hosted/test/org/foo/bar/1.0/bar-1.0.pom

actor User
#supported participant types: participant, actor, boundary, control, entity, database

participant ContentManager
database Storage #lightblue
participant ContentEventDispatcher
participant MavenMetadataManager
database MavenMetadataCache #lightgreen

User->ContentManager:PUT \n<type>/<name>/<path>
User->ContentManager:PUT \n<type>/<name>/<path>
ContentManager->ContentAdvisor:isStorable()\n<repo>\n<path>
ContentManager<-ContentAdvisor:<false>
ContentManager->Storage:store()\n<repo>\n<path>
ContentManager<-Storage:OK
ContentManager->ContentEventDispatcher:stored()\n<evt>
ContentEventDispatcher->MavenMetadataManager:stored()\n<evt>
MavenMetadataManager->MavenMetadataManager:parse() <path> = <ArtifactRef>
MavenMetadataManager->MavenMetadataCache:GA[TC]+=V
MavenMetadataManager<-MavenMetadataCache:<void>
ContentEventDispatcher<-MavenMetadataManager:<void>
ContentManager<-ContentEventDispatcher:<void>
User<-ContentManager:200 OK / 201 CREATED

