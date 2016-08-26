#
# Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

FROM centos

MAINTAINER John Casey <jdcasey@commonjava.org>

VOLUME /var/lib/indy/storage /var/lib/indy/data /var/log/indy /etc/indy /tmp/indy /tmp/ssh-config

EXPOSE 8080 8081 8000

ENV GIT_SSL_NO_VERIFY true

# TODO: I don't think we can consolidate any of these, since the target is necessary for dumb-init 
ADD https://github.com/Yelp/dumb-init/releases/download/v1.1.1/dumb-init_1.1.1_amd64 /usr/local/bin/dumb-init
ADD maven/scripts/start-indy.py /usr/local/bin/start-indy.py

# TODO: root needed to run yum...?
#USER root

RUN chmod +x /usr/local/bin/* && \
	yum -y update && \
	yum -y install wget git tar which curl tree java-1.8.0-openjdk-devel && \
	yum clean all

ENTRYPOINT ["/usr/local/bin/dumb-init", "/usr/local/bin/start-indy.py"]

# Unpack the distro that was created elsewhere in this Indy build.

# For some reason, ADDing the tarball directly so it's unpacked using Docker
# doesn't work correctly. It results in some sort of classpath issue, so
# let's emulate what any user might do when installing the tarball.
COPY maven/tmp/indy.tar.gz /tmp/indy.tar.gz
RUN tar -zxvf /tmp/indy.tar.gz -C /opt

