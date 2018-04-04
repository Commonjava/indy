package org.commonjava.indy.koji.data;

/**
 * Created by ruhan on 3/26/18.
 */
public interface KojiRepoNameParser
{
    /**
     * Retrieve koji build nvr from the repository name
     * @param repoName
     * @return koji build nvr. null if can not parse it.
     */
    String parse(String repoName );
}
