package org.commonjava.indy.koji.content;

import com.redhat.red.build.koji.KojiClient;
import org.commonjava.indy.content.ContentManager;
import org.commonjava.indy.data.StoreDataManager;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Created by jdcasey on 5/20/16.
 */
@Decorator
@ApplicationScoped
public class KojiContentManagerDecorator
{
    @Delegate
    @Inject
    private ContentManager contentManager;

    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private KojiClient kojiClient;
}
