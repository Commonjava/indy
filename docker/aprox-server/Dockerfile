FROM centos

MAINTAINER John Casey <jdcasey@commonjava.org>

RUN yum -y update
RUN yum -y install wget git tar which curl java-1.7.0-openjdk-devel

VOLUME /var/lib/aprox/storage /var/lib/aprox/data /var/log/aprox /etc/aprox /tmp/aprox /tmp/ssh-config

EXPOSE 8081
EXPOSE 8000

ADD start-aprox.py /usr/local/bin/start-aprox.py
RUN chmod +x /usr/local/bin/start-aprox.py

CMD "/usr/local/bin/start-aprox.py"
