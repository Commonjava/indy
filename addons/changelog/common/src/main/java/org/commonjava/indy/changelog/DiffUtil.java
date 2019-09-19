/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.changelog;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.algorithm.DiffException;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;

import java.util.Arrays;
import java.util.List;

public class DiffUtil
{
    private static final int DIFF_PATCH_CONTEXT_LINES = 3;

    private static final String VIRTUAL_OLD_FILE_PREFIX = "a/";

    private static final String VIRTUAL_NEW_FILE_PREFIX = "b/";

    private static final String LINE_BREAK = "\n";

    public static String diffPatch( final String fileName, final String changed, final String origin )
            throws DiffException
    {
        List<String> storeNewStrings = Arrays.asList( changed.split( LINE_BREAK ) );
        List<String> storeOriginStrings = Arrays.asList( origin.split( LINE_BREAK ) );
        Patch<String> patch = DiffUtils.diff( storeOriginStrings, storeNewStrings );
        List<String> patchDiff = UnifiedDiffUtils.generateUnifiedDiff( VIRTUAL_OLD_FILE_PREFIX + fileName,
                                                                       VIRTUAL_NEW_FILE_PREFIX + fileName,
                                                                       storeOriginStrings, patch,
                                                                       DIFF_PATCH_CONTEXT_LINES );
        StringBuilder builder = new StringBuilder();
        patchDiff.forEach( ps -> builder.append( ps ).append( LINE_BREAK ) );
        if ( builder.lastIndexOf( LINE_BREAK ) > 0 )
        {
            builder.deleteCharAt( builder.lastIndexOf( LINE_BREAK ) );
        }
        return builder.toString();
    }

    public static String recoverFromPatch( final String oldString, final String patchString )
            throws PatchFailedException
    {
        List<String> repoOldStrings = Arrays.asList( oldString.split( LINE_BREAK ) );
        List<String> patchStrings = Arrays.asList( patchString.split( LINE_BREAK ) );
        Patch<String> patchGen = UnifiedDiffUtils.parseUnifiedDiff( patchStrings );
        List<String> result = DiffUtils.patch( repoOldStrings, patchGen );
        StringBuilder newBuilder = new StringBuilder();
        result.forEach( newBuilder::append );
        return newBuilder.toString();
    }
}
