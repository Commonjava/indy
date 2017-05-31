package org.commonjava.indy.pkg.npm.model;

import org.commonjava.indy.model.core.PackageTypeDescriptor;

import static org.commonjava.maven.galley.io.SpecialPathConstants.PKG_TYPE_NPM;

/**
 * {@link PackageTypeDescriptor} implementation for NPM content.
 *
 * Created by yma on 5/23/17.
 */
public class NPMPackageTypeDescriptor
                implements PackageTypeDescriptor
{
    public static final String NPM_PKG_KEY = PKG_TYPE_NPM;

    public static final String NPM_CONTENT_REST_BASE_PATH = "/api/content/npm";

    public static final String NPM_ADMIN_REST_BASE_PATH = "/api/admin/stores/npm";

    @Override
    public String getKey()
    {
        return NPM_PKG_KEY;
    }

    @Override
    public String getContentRestBasePath()
    {
        return NPM_CONTENT_REST_BASE_PATH;
    }

    @Override
    public String getAdminRestBasePath()
    {
        return NPM_ADMIN_REST_BASE_PATH;
    }
}
