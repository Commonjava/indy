#!/usr/bin/ruby

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
    }

    OptionParser.new{|opts|
      opts.on('-d', '--debug=STYLE', 'setup java debug port 8000 (style: "y" or "n" for suspend style)'){|style| config[:debug] = style}
      opts.on('-e', '--existing', 'keep existing unpacked directory if it exists'){config[:clean]=false}
      opts.on('-L', '--nolaunch', 'do not launch!'){config[:launch]=false}
      opts.on('-u', '--uidev', 'Link to the sources for the UI to enable live UI development' ){config[:uidev]=true}
      opts.on('-t', "--type=TYPE", "Type of launcher to run (min|easyprox|savant, default: savant)"){|type| config[:flavor] = type}

      config[:args] = opts.parse!(args)
    }

    launcher = config[:flavor]
    puts "Launching: #{launcher}"

    target = File.join( BASEDIR, 'deployments/launchers', launcher, 'target' )
    launch_dir = File.join(target, "aprox" )

    if config[:clean]
      puts "Deleting AProx launch dir..."
      rm_rf( launch_dir )
    end

    glob = File.join( target, "aprox-launcher-*-launcher.tar.gz" )

    puts "Looking for launcher archives: '#{glob}"
    archives = Dir.glob(glob)

    puts "Found matching archives: #{archives}"
    do_exec( "tar -zxvf #{archives[0]} -C #{target}" )

    if (config[:uidev])
      uibase = "#{launch_dir}/var/lib/aprox/ui"
      
      mv( uibase, "#{uibase}.bak" )
      mkdir(uibase)
      Dir["#{BASEDIR}/uis/layover/src/main/js/app/*"].each{|file|
        fname = File.basename(file)
        ln_s( file, File.join(uibase, fname) )
      }
      
      addon_base = File.join(uibase, "layover")
      mkdir( addon_base ) unless File.directory?(addon_base)
      
      Dir["#{uibase}.bak/layover/*"].each{|addon_path|
        addon = File.basename(addon_path)
        addon_target = File.join( addon_base, addon )
        ln_s( "#{BASEDIR}/addons/#{addon}/common/src/main/ui/layover/#{addon}", addon_target )
      }
    end

    if ( config[:debug] )
      env_sh = "#{launch_dir}/etc/aprox/env.sh"

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
      exec( "#{launch_dir}/bin/aprox.sh #{config[:args].join(' ')}" )
    else
      exec("cd #{launch_dir}")
    end
  end
end

Launcher.new().run(ARGV)
