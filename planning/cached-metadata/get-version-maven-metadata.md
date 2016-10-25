title GET /group/test/org/foo/bar/maven-metadata.xml (MISSING version list metadata)

actor User
#supported participant types: participant, actor, boundary, control, entity, database

participant ContentManager
participant ContentAdvisor
participant MavenMetadataGenerator
participant MavenMetadataManager
database MavenMetadataCache #lightgreen
participant KojiMetadataLookup
database Koji #blue

User->ContentManager: GET\n<type>/<name>/<path>

ContentManager->ContentAdvisor:getContentQuality()\n<path>
ContentManager<-ContentAdvisor:<METADATA>

ContentManager->MavenMetadataGenerator:generateGroupContent()\n<group-key>\n<path>

MavenMetadataGenerator->MavenMetadataManager:getCachedVersions()\n<group-member-keys>\n<GA>

rbox over MavenMetadataGenerator,MavenMetadataCache:**TODO:** How to tell version from artifact metadata?

MavenMetadataManager->MavenMetadataCache:search()\n<IN group-member-keyset>\n<G>\n<A>

MavenMetadataManager<-MavenMetadataCache:<VERSIONS> = <empty>

loop forEach MetadataLookup

MavenMetadataManager->KojiMetadataLookup:lookup()\n<group-member-keyset>\n<G>\n<A>

# try parsing parent dir into G:A and list builds.
KojiMetadataLookup->Koji:listBuildsContaining()\n<G=org/foo>\n<A=bar>
KojiMetadataLookup<-Koji:<KojiBuildInfo-list>

# process each build result
loop forEach KojiBuildInfo (B) in KojiBuildInfo-list

rbox over MavenMetadataManager,Koji: Check whether we can skip processing each archive in the build

KojiMetadataLookup->MavenMetadataManager:hasBuild()\n<koji>\n<B.getNVR()>

MavenMetadataManager->MavenMetadataCache:containsKey()\n<koji>\n<build-NVR>

MavenMetadataManager<-MavenMetadataCache:<false>

KojiMetadataLookup<-MavenMetadataManager:<false>

rbox over MavenMetadataManager,Koji: Can't Skip. Get all archives for the B.
KojiMetadataLookup->Koji:listBuildArchives()\n<KojiBuildInfo[i]>
KojiMetadataLookup<-Koji:<KojiBuildArchiveCollection (3)>

# process each archive
loop forEach KojiArchiveInfo (A) in KojiBuildArchiveCollection

rbox over KojiMetadataLookup: Process each archive\nentry from B,\n**IF** it's a\nMaven artifact reference.
KojiMetadataLookup->KojiMetadataLookup:toArtifactRef()\n<A> = <GAVTC>
KojiMetadataLookup->KojiMetadataLookup:GAVTC-list.add()\n<GAVTC>
KojiMetadataLookup->KojiMetadataLookup:build-GAVTC-map.put()\n<B.getNVR()>\n<GAVTC-list>

end

end

MavenMetadataManager<-KojiMetadataLookup:<content-map> = <build-GAVTC-map>
MavenMetadataManager->MavenMetadataCache:putAll()\n<koji>\n<content-map>
MavenMetadataManager<-MavenMetadataCache:<true>

loop forEach GAVTC-list in content-map.values()

loop forEach GAVTC in GAVTC-list

MavenMetadataManager->MavenMetadataManager:GA-match ? VERSIONS.add(V)

end

end

MavenMetadataGenerator<-MavenMetadataManager: <VERSIONS>
MavenMetadataGenerator->MavenMetadataGenerator: render()\n<G>\n<A>\n<VERSIONS>

ContentManager<-MavenMetadataGenerator:<stream>
User<-ContentManager:200\n<stream>
