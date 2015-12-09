#!/usr/bin/env ruby
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


INDY_BASEDIR = '/Users/jdcasey/workspace/server-apps/indy'
WAR_DIR = 'savant'
AS7_HOME = '/Users/jdcasey/apps/as7/current'
CONTROLLER_HOST_PORT = 'localhost:10999'

cmds =<<-EOC
  undeploy indy.war
  deploy #{INDY_BASEDIR}/wars/#{WAR_DIR}/target/indy.war
EOC

path = '/tmp/redeploy.commands'
File.delete( path ) if File.exists?(path)
File.open(path, 'w+'){|f|
  f.puts(cmds)
}

Dir.chdir(AS7_HOME) {
  system( "env JBOSS_HOME='#{AS7_HOME}' bin/jboss-cli.sh --controller=#{CONTROLLER_HOST_PORT} --connect --file=#{path}" )
  ret = $?
  puts "Exit status: #{ret.exitstatus}"
  exit ret.exitstatus
}

