#-------------------------------------------------------------------------------
# Copyright (c) 2014 Red Hat, Inc..
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the GNU Public License v3.0
# which accompanies this distribution, and is available at
# http://www.gnu.org/licenses/gpl.html
# 
# Contributors:
#     Red Hat, Inc. - initial API and implementation
#-------------------------------------------------------------------------------
This is where groovy templates are stored for generating dynamic html for different parts of the browsable system. The default template is actually loaded from the classpath (embedded in one of the jars), but you can override any template here.

Currently, the templates available for override are:

- directory-listing.groovy

    This is the listing for a content directory in a repository, group, or deploy-point. It's meant to generate an index.html file containing links to all files in the directory.
