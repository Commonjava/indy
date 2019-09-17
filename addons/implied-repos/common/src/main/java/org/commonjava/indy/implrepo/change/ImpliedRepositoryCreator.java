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
package org.commonjava.indy.implrepo.change;

import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.subsys.template.ScriptEngine;
import org.commonjava.atlas.maven.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.maven.model.view.RepositoryView;
import org.slf4j.Logger;

/**
 * Responsible for creating new {@link RemoteRepository} instances for use with repository declarations detected in
 * Maven POM files that have been stored in the system, whether via upload (hosted repo) or via download (remote repo).
 * This interface will be implemented by a Groovy script, and accessed by way of the
 * {@link org.commonjava.indy.subsys.template.ScriptEngine#parseStandardScriptInstance(ScriptEngine.StandardScriptType, String, Class)} method.
 *
 * Created by jdcasey on 8/17/16.
 */
public interface ImpliedRepositoryCreator
{
    RemoteRepository createFrom( ProjectVersionRef implyingGAV, RepositoryView repositoryView, Logger logger );
}
