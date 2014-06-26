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
      :clean => true,
      :launch => true,
      :uidev => false,
      :flavor => 'savant',
    }

    OptionParser.new{|opts|
      opts.on('-e', '--existing', 'keep existing unpacked directory if it exists'){config[:clean]=false}
      opts.on('-L', '--nolaunch', 'do not launch!'){config[:launch]=false}
      opts.on('-u', '--uidev', 'Link to the sources for the UI to enable live UI development' ){config[:uidev]=true}
      opts.on('-t', "--type=TYPE", "Type of launcher to run (min|easyprox|savant, default: savant)"){|type| config[:flavor] = type}

      config[:args] = opts.parse!(args)
    }

    launcher = config[:flavor]
    puts "Launching: #{launcher}"

    target = File.join( BASEDIR, 'launchers', launcher, 'target' )
    launch_dir = File.join(target, "aprox-launcher-#{launcher}" )

    rm_rf( launch_dir ) if config[:clean]

    glob = File.join( target, "aprox-launcher-#{launcher}-*-launcher.tar.gz" )

    puts "Looking for launcher archives: '#{glob}"
    archives = Dir.glob(glob)

    puts "Found matching archives: #{archives}"
    do_exec( "tar -zxvf #{archives[0]} -C #{target}" )

    if (config[:uidev])
      mv( "#{launch_dir}/ui", "#{launch_dir}/ui.bak" )
      ln_s( "#{BASEDIR}/uis/layover/src/main/js/app", "#{launch_dir}/ui" )
    end

    if ( config[:launch] )
      exec( "#{launch_dir}/bin/aprox.sh #{config[:args].join(' ')}" )
    else
      exec("cd #{launch_dir}")
    end
  end
end

Launcher.new().run(ARGV)
