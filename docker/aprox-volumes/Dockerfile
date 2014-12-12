FROM centos

MAINTAINER John Casey <jdcasey@commonjava.org>

RUN yum -y update

VOLUME /var/lib/aprox/storage /var/lib/aprox/data /var/log/aprox /etc/aprox

ADD start-volumes /usr/local/bin/start-volumes
RUN chmod +x /usr/local/bin/start-volumes

CMD "/usr/local/bin/start-volumes"