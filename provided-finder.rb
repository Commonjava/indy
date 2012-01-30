#!/usr/bin/env ruby

require 'set'

deps = Set.new
`mvn dependency:tree | grep provided`.each_line {|ln|
  line = ln.chomp
  if ( line =~ /\[INFO\][-|+\s\\]+([^:]+):([^:]+).*/ )
    deps << "#{$1}:#{$2}"
  end
}

sorted = Array.new
deps.each {|dep|
  sorted << dep
}

sorted.sort!

puts "<excludes>"
sorted.each {|dep|
  puts "  <exclude>#{dep}</exclude>"
}
puts "</excludes>"

