package org.commonjava.indy.koji.data;

/**
 * Created by ruhan on 3/26/18.
 */
public class DefaultKojiRepoNameParser
                implements KojiRepoNameParser
{
    private static final String KOJI_ = "koji-";

    private static final String KOJI_BINARY_ = "koji-binary-";

    /**
     * Retrieve what is behind koji- or koji-binary-
     * @param repoName
     * @return koji build nvr
     */
    public String parse( String repoName )
    {
        String prefix = null;
        if ( repoName.startsWith( KOJI_BINARY_ ) )
        {
            prefix = KOJI_BINARY_;
        }
        else if ( repoName.startsWith( KOJI_ ) )
        {
            prefix = KOJI_;
        }

        if ( prefix != null )
        {
            return repoName.substring( prefix.length() );
        }
        return null;
    }
}
