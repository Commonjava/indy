title PUT /hosted/test/org/foo/bar/maven-metadata.xml

actor User
#supported participant types: participant, actor, boundary, control, entity, database

participant ContentManager
participant ContentAdvisor
database Storage #lightblue
participant ContentEventDispatcher
participant MavenMetadataManager
database MavenMetadataCache #lightgreen

User->ContentManager:PUT \n<type>/<name>/<path>
ContentManager->ContentAdvisor:isStorable()\n<repo>\n<path>
ContentManager<-ContentAdvisor:<false>
ContentManager->ContentEventDispatcher:stored()\n<evt>
ContentEventDispatcher->MavenMetadataManager:stored()\n<evt>
MavenMetadataManager->MavenMetadataManager:parse() <path> = <null>
ContentEventDispatcher<-MavenMetadataManager:<void>
ContentManager<-ContentEventDispatcher:<void>
User<-ContentManager:200 OK / 201 CREATED