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
You can place individual *.conf files here and have them picked up in the AProx configuration, since the /etc/main.conf file uses:

Include conf.d/*.conf
