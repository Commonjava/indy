#!/usr/bin/ruby

require 'fileutils'

def do_exec( cmd )
  puts "Running: '#{cmd}'..."
  system( cmd )
  
  result=$?
  if ( result != 0 )
    puts "#{cmd} exited with '#{result}'"
    exit result
  end
end

launcher = 'savant'
if ( ARGV.length > 0 && File.exists?("launchers/#{ARGV[0]}") )
  launcher = ARGV.shift
end

clean = true
launch = true
ARGV.each{|arg|
  if ( arg == '--existing' )
    ARGV.shift
    clean = false
  elsif ( arg == '--nolaunch' )
    ARGV.shift
    launch = false
  end
}

puts "Launching: #{launcher}"

base = File.join( FileUtils.pwd(), 'launchers', launcher, 'target' )
launch_dir = File.join(base, "aprox-launcher-#{launcher}" )
do_exec( "rm -rf #{launch_dir}" ) if clean

glob = File.join( base, "aprox-launcher-#{launcher}-*-launcher.tar.gz" )
puts "Looking for launcher archives: '#{glob}"
archives = Dir.glob(glob)
puts "Found matching archives: #{archives}"

do_exec( "tar -zxvf #{archives[0]} -C #{base}" )
if ( launch )
  exec( "#{launch_dir}/bin/aprox.sh #{ARGV.join(' ')}" )
else
  exec("cd #{launch_dir}")
end

