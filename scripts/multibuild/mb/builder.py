#
# Copyright (C) 2011-2017 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import os
import requests
import json
from threading import Thread
from urlparse import urlparse
import time
import mb.util

SETTINGS = """
<?xml version="1.0"?>
<settings>
  <localRepository>%(dir)s/local-repo</localRepository>
  <mirrors>
    <mirror>
      <id>indy</id>
      <mirrorOf>*</mirrorOf>
      <url>%(url)s/api/folo/track/%(id)s/group/%(id)s</url>
    </mirror>
  </mirrors>
  <proxies>
    <proxy>
      <id>indy-httprox</id>
      <active>true</active>
      <protocol>http</protocol>
      <host>%(host)s</host>
      <port>%(proxy_port)s</port>
      <username>%(id)s+tracking</username>
      <password>foo</password>
      <nonProxyHosts>%(host)s</nonProxyHosts>
    </proxy>
  </proxies>
  <profiles>
    <profile>
      <id>resolve-settings</id>
      <repositories>
        <repository>
          <id>central</id>
          <url>%(url)s/api/folo/track/%(id)s/group/%(id)s</url>
          <releases>
            <enabled>true</enabled>
          </releases>
          <snapshots>
            <enabled>false</enabled>
          </snapshots>
        </repository>
      </repositories>
      <pluginRepositories>
        <pluginRepository>
          <id>central</id>
          <url>%(url)s/api/folo/track/%(id)s/group/%(id)s</url>
          <releases>
            <enabled>true</enabled>
          </releases>
          <snapshots>
            <enabled>false</enabled>
          </snapshots>
        </pluginRepository>
      </pluginRepositories>
    </profile>
    
    <profile>
      <id>deploy-settings</id>
      <properties>
        <altDeploymentRepository>%(id)s::default::%(url)s/api/folo/track/%(id)s/hosted/%(id)s</altDeploymentRepository>
      </properties>
    </profile>
    
  </profiles>
  <activeProfiles>
    <activeProfile>resolve-settings</activeProfile>
    
    <activeProfile>deploy-settings</activeProfile>
    
  </activeProfiles>
</settings>
"""

POST_HEADERS = {'content-type': 'application/json', 'accept': 'application/json'}

class Builder(Thread):
    def __init__(self, queue, reports):
        Thread.__init__(self)
        self.queue = queue
        self.reports = reports

    def run(self):
        while True:
            try:
                (builddir, indy_url, proxy_port, delay) = self.queue.get()

                parsed = urlparse(indy_url)
                params = {'dir': builddir, 'url':indy_url, 'id': os.path.basename(builddir), 'host': parsed.hostname, 'port': parsed.port, 'proxy_port': proxy_port}

                self.setup(builddir, params);

                if delay > 0:
                  print "Delay: %s seconds" % delay
                  time.sleep(delay)

                self.build(builddir)
                self.seal_folo_report(params)
                self.reports.put((builddir,params['url'], params['id']))

                folo_report = self._pull_folo_report(params)
                self.promote_by_path(folo_report, params)

                self.cleanup_build_group(params)

                self.promote_by_group(params)

            except (KeyboardInterrupt,SystemExit,Exception) as e:
                print e
                break
            finally:
                self.queue.task_done()

    def promote_by_path(self, folo_report, params):
        """Run by-path promotion of downloaded content"""
        to_promote = {}

        downloads = folo_report.get('downloads')
        if downloads is not None:
          for download in downloads:
            key = download['storeKey']
            mode = download['accessChannel']
            if mode == 'MAVEN_REPO' and key.startswith('remote:'):
              path = download['path']

              paths = to_promote.get(key)
              if paths is None:
                paths = []
                to_promote[key]=paths

              paths.append(path)

        print "Promoting dependencies from %s sources into hosted:shared-imports" % len(to_promote.keys())
        for key in to_promote:
          req = {'source': key, 'target': 'hosted:shared-imports', 'paths': to_promote[key]}
          resp = requests.post("%(url)s/api/promotion/paths/promote" % params, json=req, headers=POST_HEADERS)
          resp.raise_for_status()

    def promote_by_group(self, params):
        """Run by-group promotion of uploaded content"""

        print "Promoting build output in hosted:%(id)s to membership of group:builds" % params
        req = {'source': 'hosted:%(id)s' % params, 'targetGroup': 'builds'}
        resp = requests.post("%(url)s/api/promotion/groups/promote" % params, json=req, headers=POST_HEADERS)
        resp.raise_for_status()


    def _pull_folo_report(self, params):
        """Pull the Folo tracking report associated with the current build"""

        print "Retrieving folo tracking report for: %(id)s" % params
        resp = requests.get("%(url)s/api/folo/admin/%(id)s/record" % params)
        resp.raise_for_status()

        return resp.json()

    def seal_folo_report(self, params):
        """Seal the Folo tracking report after the build completes"""

        print "Sealing folo tracking report for: %(id)s" % params
        resp = requests.post("%(url)s/api/folo/admin/%(id)s/record" % params, data={})
        resp.raise_for_status()

    def cleanup_build_group(self, params):
        """Remove the group created specifically to channel content into this build,
           since we're done with it now.
        """

        print "Deleting temporary group:%(id)s used for build time only" % params
        resp = requests.delete("%(url)s/api/admin/group/%(id)s" % params)
        resp.raise_for_status()

    def build(self, builddir):
        mb.util.run_cmd("mvn -DskipTests -f %(d)s/pom.xml -s %(d)s/settings.xml clean deploy 2>&1 | tee %(d)s/build.log" % {'d': builddir}, fail=False)

    def setup(self, builddir, params):
        """Create the hosted repo and group, then pull the Indy-generated Maven
           settings.xml file tuned to that group."""

        params['shared_name'] = 'shared-imports'
        params['builds_name'] = 'builds'
        params['brew_proxies'] = 'brew_proxies'

        # Create the shared-imports hosted repo if necessary
        resp = requests.head('%(url)s/api/admin/hosted/%(shared_name)s' % params)
        if resp.status_code == 404:
          shared = {
              'type': 'hosted', 
              'key': "hosted:%(shared_name)s" % params, 
              'disabled': False, 
              'doctype': 'hosted', 
              'name': params['shared_name'], 
              'allow_releases': True
          }

          print "POSTing: %s" % json.dumps(shared, indent=2)

          resp = requests.post("%(url)s/api/admin/hosted" % params, json=shared, headers=POST_HEADERS)
          resp.raise_for_status()

        # Create the builds group if necessary
        resp = requests.head('%(url)s/api/admin/group/%(builds_name)s' % params)
        if resp.status_code == 404:
          builds_group = {
              'type': 'group', 
              'key': "group:%(builds_name)s" % params, 
              'disabled': False, 
              'doctype': 'group', 
              'name': params['builds_name'], 
          }

        # Create the builds group if necessary
        resp = requests.head('%(url)s/api/admin/group/%(brew_proxies)s' % params)
        if resp.status_code == 404:
          brew_proxies = {
              'type': 'group', 
              'key': "group:%(brew_proxies)s" % params, 
              'disabled': False, 
              'doctype': 'group', 
              'name': params['brew_proxies'], 
          }

          print "POSTing: %s" % json.dumps(brew_proxies, indent=2)

          resp = requests.post("%(url)s/api/admin/group" % params, json=brew_proxies, headers=POST_HEADERS)
          resp.raise_for_status()

        # Create the hosted repo for this build
        hosted = {
            'type': 'hosted', 
            'key': "hosted:%(id)s" % params, 
            'disabled': False, 
            'doctype': 'hosted', 
            'name': params['id'], 
            'allow_releases': True
        }

        print "POSTing: %s" % json.dumps(hosted, indent=2)

        resp = requests.post("%(url)s/api/admin/hosted" % params, json=hosted, headers=POST_HEADERS)
        resp.raise_for_status()

        # Create the group for this build
        group = {
            'type': 'group', 
            'key': "group:%(id)s" % params, 
            'disabled': False, 
            'doctype': 'group', 
            'name': params['id'], 
            'constituents': [
                "hosted:%(id)s" % params, 
                'group:builds',
                'group:brew_proxies',
                'hosted:shared-imports',
                'group:public'
            ]
        }

        print "POSTing: %s" % json.dumps(group, indent=2)

        resp = requests.post("%(url)s/api/admin/group" % params, json=group, headers=POST_HEADERS)
        resp.raise_for_status()

        # Write the settings.xml we need for this build
        with open("%s/settings.xml" % builddir, 'w') as f:
            f.write(SETTINGS % params)
