#!/usr/bin/ruby
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


require 'fileutils'
require 'optparse'

class Launcher

  include FileUtils

  BASEDIR=File.dirname(File.dirname(File.expand_path(__FILE__)))

  def do_exec( cmd )
    puts "Running: '#{cmd}'..."
    system( cmd )
    
    result=$?
    if ( result != 0 )
      puts "#{cmd} exited with '#{result}'"
      exit result
    end
  end

  def run(args)

    config={
      :debug => 'n',
      :clean => true,
      :launch => true,
      :uidev => false,
      :flavor => 'savant',
      :args => [],
      :verbose => false,
    }

    OptionParser.new{|opts|
      opts.on('-d', '--debug=STYLE', 'setup java debug port 8000 (style: "y" or "n" for suspend style)'){|style| 
        if style == 'off'
          config.delete(:debug)
        else
          config[:debug] = style
        end
      }
      
      opts.on('-e', '--existing', 'keep existing unpacked directory if it exists'){config[:clean]=false}
      opts.on('-L', '--nolaunch', 'do not launch!'){config[:launch]=false}
      opts.on('-u', '--uidev', 'Link to the sources for the UI to enable live UI development' ){config[:uidev]=true}
      opts.on('-t', "--type=TYPE", "Type of launcher to run (min|easyprox|savant, default: savant)"){|type| config[:flavor] = type}
      opts.on('-p', '--port=PORT', "Port to use"){|port| config[:args] << "-p #{port}"}
      opts.on('-v', '--verbose', "Verbose debug output (NOTE: outputs to console ONLY)"){config[:verbose] = true}

      config[:args].concat(opts.parse!(args))
    }

    launcher = config[:flavor]
    puts "Launching: #{launcher}"

    target = File.join( BASEDIR, 'deployments/launchers', launcher, 'target' )
    launch_dir = File.join(target, "indy" )

    if config[:clean]
      puts "Deleting Indy launch dir..."
      rm_rf( launch_dir )
      
      glob = File.join( target, "indy-launcher-*-launcher.tar.gz" )

      puts "Looking for launcher archives: '#{glob}"
      archives = Dir.glob(glob)

      puts "Found matching archives: #{archives}"
      do_exec( "tar -zxvf #{archives[0]} -C #{target}" )
      rm_rf("#{target}/indy/etc/indy/logging") if config[:verbose]
    end

    if (config[:uidev])
      uibase = "#{launch_dir}/var/lib/indy/ui"
      
      mv( uibase, "#{uibase}.bak" )
      mkdir(uibase)
      Dir["#{BASEDIR}/uis/layover/app/*"].each{|file|
        fname = File.basename(file)
        ln_s( file, File.join(uibase, fname) )
      }
      
      addon_base = File.join(uibase, "layover/ui-addons")
      mkdir_p( addon_base ) unless File.directory?(addon_base)
      
      Dir["#{uibase}.bak/layover/ui-addons/*"].each{|addon_path|
        addon = File.basename(addon_path)
        addon_target = File.join( addon_base, addon )
        ln_s( "#{BASEDIR}/addons/#{addon}/common/src/main/ui/layover/ui-addons/#{addon}", addon_target )
      }
    end

    if ( config[:debug] )
      env_sh = "#{launch_dir}/etc/indy/env.sh"

      lines = []
      lines = File.readlines(env_sh) if ( File.exists?(env_sh))
      debug_line = "export JAVA_DEBUG_OPTS=\"-Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=#{config[:debug]}\""

      found = false
      lines.each{|line|
        if ( line =~ /export JAVA_DEBUG_OPTS.+/ )
          found = true
          break
        end
      }

      lines << debug_line unless found
      File.open(env_sh, 'w+'){|f|
        lines.each{|line|
          f.puts(line)
        }
      }
    end

    if ( config[:launch] )
      cmd = "#{launch_dir}/bin/indy.sh #{config[:args].join(' ')}"
      puts "Running: '#{cmd}'"
      exec( cmd )
    else
      cmd="cd #{launch_dir}"
      puts "Running: '#{cmd}'"
      exec(cmd)
    end
  end
end

Launcher.new().run(ARGV)
