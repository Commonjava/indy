title GET /group/test/org/foo/bar/1.0-redhat-1/bar-1.0-redhat-1.pom (resolve from Koji build)

actor User
#supported participant types: participant, actor, boundary, control, entity, database

boundary REST
participant KojiContentManagerDecorator
participant ContentManager
database Storage #lightgray
participant KojiClient
database Koji #lightblue
participant KojiConfig

linear
User->REST: GET\n<type>/<name>/<path>
REST->KojiContentManagerDecorator:retrieve(<key>,<path>)
linear off

KojiContentManagerDecorator->ContentManager:retrieve(<key>,<path>)
ContentManager->Storage:exists(<full-path>)
ContentManager<-Storage:<false>
KojiContentManagerDecorator<-ContentManager:<null>

abox over KojiContentManagerDecorator #red:RETURN <null>\n[**IF** <key.type> != group]
KojiContentManagerDecorator->KojiContentManagerDecorator:<GAV> = ArtifactPathInfo.parse(<path>)

abox over KojiContentManagerDecorator #lightgreen:CALL proxyKojiBuild(<GAV>)
KojiContentManagerDecorator->KojiClient:listBuildsContaining(<GAV>)
KojiClient<->Koji:XMLRPC
KojiContentManagerDecorator<-KojiClient:<builds>

rbox over KojiContentManagerDecorator #lightgray: Sort by completion timestamp, in ascending order
KojiContentManagerDecorator->KojiContentManagerDecorator:sort(<builds>)

loop for <build> in builds
rbox over KojiContentManagerDecorator: Builds with missing taskId's\nare binary imports.
KojiContentManagerDecorator->KojiContentManagerDecorator: continue if <build>.taskId == null
KojiContentManagerDecorator->KojiClient:listTags(<build.id>)
KojiClient<->Koji:XMLRPC
KojiContentManagerDecorator<-KojiClient:<tags>

loop for <tag> in tags, while <allowed> == false
KojiContentManagerDecorator->KojiConfig:isTagAllowed(<tag>)
KojiContentManagerDecorator<-KojiConfig:<allowed> = <true>
end

abox over KojiContentManagerDecorator #lightgreen:CALL createRemoteRepository(<build>)

abox over KojiContentManagerDecorator #red:RETURN <remote>\n[to: proxyKojiBuild()]

end

abox over KojiContentManagerDecorator #red:RETURN <remote>\n[to: retrieve()]

abox over KojiContentManagerDecorator #lightgreen:CALL adjustTargetGroup(<remote>, <group>)

KojiContentManagerDecorator->KojiConfig:getTargetGroup(<group>)
KojiContentManagerDecorator<-KojiConfig:<targetGroup>
KojiContentManagerDecorator->KojiContentManagerDecorator:<targetGroup>.addConstituent(<remote.key>)
KojiContentManagerDecorator->KojiContentManagerDecorator:store(<targetGroup>)

abox over KojiContentManagerDecorator #red:RETURN <targetGroup>\n[to: retrieve()]

KojiContentManagerDecorator->ContentManager:retrieve(<remote>, <path>)
ContentManager->Storage:download()
ContentManager<-Storage:<transfer>


linear
KojiContentManagerDecorator<-ContentManager:<transfer>
REST<-KojiContentManagerDecorator:<transfer>
User<-REST:<transfer.openInputStream()>
linear off

