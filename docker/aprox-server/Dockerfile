FROM centos

MAINTAINER John Casey <jdcasey@commonjava.org>

RUN yum -y update
RUN yum -y install wget git tar which curl java-1.7.0-openjdk-devel

VOLUME /var/lib/aprox/storage /var/lib/aprox/data /var/log/aprox /etc/aprox

EXPOSE 8080

ADD start-aprox /usr/local/bin/start-aprox
RUN chmod +x /usr/local/bin/start-aprox

CMD "/usr/local/bin/start-aprox"
