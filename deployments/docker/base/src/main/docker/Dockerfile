#
# Copyright (C) 2015 John Casey (jdcasey@commonjava.org)
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

RUN yum -y update
RUN yum -y install wget git tar which curl tree java-1.8.0-openjdk-devel

VOLUME /var/lib/indy/storage /var/lib/indy/data /var/log/indy /etc/indy /tmp/indy /tmp/ssh-config

EXPOSE 8080
EXPOSE 8081
EXPOSE 8000

ADD https://github.com/Yelp/dumb-init/releases/download/v1.1.1/dumb-init_1.1.1_amd64 /usr/local/bin/dumb-init
ADD maven/scripts/start-indy.py /usr/local/bin/start-indy.py
ADD maven/indy.tar.gz /opt/

RUN chmod +x /usr/local/bin/*

ENTRYPOINT ["/usr/local/bin/dumb-init", "/usr/local/bin/start-indy.py"]
