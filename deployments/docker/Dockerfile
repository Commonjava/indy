#
# Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

EXPOSE 8080 8081 8000

# TODO: I don't think we can consolidate any of these, since the target is necessary for dumb-init 
ADD https://github.com/Yelp/dumb-init/releases/download/v1.1.1/dumb-init_1.1.1_amd64 /usr/local/bin/dumb-init

# TODO: root needed to run yum...?
USER root

RUN chmod +x /usr/local/bin/*
RUN	yum -y update
RUN	yum -y install wget git tar which curl tree java-1.8.0-openjdk-devel
RUN	yum clean all
RUN	groupadd -g 1000 indy
RUN	useradd -u 1000 -g indy --no-create-home -s /bin/false -d /opt/indy indy


# For some reason, ADDing the tarball directly so it's unpacked using Docker
# doesn't work correctly. It results in some sort of classpath issue, so
# let's emulate what any user might do when installing the tarball.
COPY target/dependency/indy-launcher-skinny.tar.gz /opt/indy-archives/indy-launcher-skinny.tar.gz
COPY target/dependency/indy-launcher-data.tar.gz /opt/indy-archives/indy-launcher-data.tar.gz
COPY target/dependency/indy-launcher-etc.tar.gz /opt/indy-archives/indy-launcher-etc.tar.gz

RUN	tar -zxvf /opt/indy-archives/indy-launcher-skinny.tar.gz -C /opt

RUN mkdir -p /indy/storage /indy/data /indy/logs /indy/etc /indy/ssh /opt/indy-git-etc
RUN chown -R indy:indy /indy/storage /indy/data /indy/logs /indy/etc /indy/ssh /opt/indy /opt/indy-git-etc
VOLUME /indy/storage /indy/data /indy/logs /indy/etc

ADD scripts/start-indy.py /usr/local/bin/start-indy.py
RUN chmod 755 /usr/local/bin/*

USER indy

ENTRYPOINT ["/usr/local/bin/dumb-init", "/usr/local/bin/start-indy.py"]
#ENTRYPOINT ["/bin/bash"]


