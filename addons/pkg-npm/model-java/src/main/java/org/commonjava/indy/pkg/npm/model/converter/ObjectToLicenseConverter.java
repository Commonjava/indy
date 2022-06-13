package org.commonjava.indy.pkg.npm.model.converter;

import com.fasterxml.jackson.databind.util.StdConverter;
import org.commonjava.indy.pkg.npm.model.License;

import java.util.List;

public class ObjectToLicenseConverter extends StdConverter<Object, License> {

    @Override
    public License convert(Object o) {
        if (o instanceof List)
        {
            // Use SPDX expressions, ref https://docs.npmjs.com/cli/v7/configuring-npm/package-json
            // e.g, parse "[MIT, Apache2]" to "(MIT OR Apache2)"
            String license = o.toString().replaceAll("\\[|\\]", "");
            return new License("(" + license.replaceAll(",", " OR") + ")");
        }
        return new License(o.toString());
    }
}
