#!/usr/bin/env ruby

APROX_BASEDIR = '/Users/jdcasey/workspace/server-apps/aprox'
WAR_DIR = 'savant'
AS7_HOME = '/Users/jdcasey/apps/as7/current'
CONTROLLER_HOST_PORT = 'localhost:10999'

cmds =<<-EOC
  undeploy aprox.war
  deploy #{APROX_BASEDIR}/wars/#{WAR_DIR}/target/aprox.war
EOC

path = '/tmp/redeploy.commands'
File.delete( path ) if File.exists?(path)
File.open(path, 'w+'){|f|
  f.puts(cmds)
}

Dir.chdir(AS7_HOME) {
  system( "bin/jboss-cli.sh --controller=#{CONTROLLER_HOST_PORT} --connect --file=#{path}" )
  exit $?
}

