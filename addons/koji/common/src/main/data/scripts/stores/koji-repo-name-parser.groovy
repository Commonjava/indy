package org.commonjava.indy.koji;

import org.commonjava.indy.koji.data.DefaultKojiRepoNameParser;

class RepoNameParser implements KojiRepoNameParser
{
    private DefaultKojiRepoNameParser defaultKojiRepoNameParser = new DefaultKojiRepoNameParser();

    /**
     * Retrieve what is behind koji- or koji-binary-
     * @param repoName
     * @return koji build nvr
     */
    @Override
    String parse(String repoName )
    {
        return defaultKojiRepoNameParser.parse( repoName );
    }

}