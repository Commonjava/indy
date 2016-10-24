title PUT /hosted/test/org/foo/bar/1.0/bar-1.0.pom

actor User
#supported participant types: participant, actor, boundary, control, entity, database

participant ContentManager
database Storage #lightblue
participant FileEventDispatcher
participant MavenMetadataManager
database MavenMetadataCache

User->ContentManager:PUT \n<type>/<name>/<path>
ContentManager->Storage:store()\n<repo>\n<path>
ContentManager<-Storage:OK
ContentManager->FileEventDispatcher:stored()\n<evt>
FileEventDispatcher->MavenMetadataManager:stored()\n<evt>
MavenMetadataManager->MavenMetadataManager:parse() <path>
MavenMetadataManager->MavenMetadataCache:GA[TC]+=V
MavenMetadataManager<-MavenMetadataCache:<void>
FileEventDispatcher<-MavenMetadataManager:<void>
ContentManager<-FileEventDispatcher:<void>
User<-ContentManager:200 OK / 201 CREATED

