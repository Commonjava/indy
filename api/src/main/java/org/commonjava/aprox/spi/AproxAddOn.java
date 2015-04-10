package org.commonjava.aprox.spi;

import org.commonjava.aprox.model.spi.AproxAddOnID;

/**
 * Component that describes an add-on to AProx. This is currently most useful when the add-on provides UI elements, and is the only way to get new
 * parts added to the UI.
 */
public interface AproxAddOn
{

    /**
     * Retrieve an identifier for this add-on which contains various UI configurations, along with a basic name for the add-on.
     */
    AproxAddOnID getId();

}
