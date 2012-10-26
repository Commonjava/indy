#!/usr/bin/env ruby

require 'optparse'
require 'json/pure'
require 'net/http'

class Loader
  include Net
  
  BASE_PATH = 'api/1.0/admin'
  DEPLOYS_PATH = "#{BASE_PATH}/deploys"
  GROUPS_PATH = "#{BASE_PATH}/groups"
  REPOS_PATH = "#{BASE_PATH}/repositories"
  
  def initialize( args = [] )
    @options={
      :url => 'http://localhost:9080/aprox',
    }
    
    OptionParser.new {|opts|
      opts.banner =<<-EOB

Usage: #{$0} [-d <deploy-file>] [-g <group-file>] [-r <repo-file>] [url]

Bulk-loads repository, deploy-point, group, and other data into an AProx instance.
NOTE: Default url is #{@options[:url]}


      EOB
      opts.on( '-d', "--deploys=FILE", "File containing deploy-points JSON" ){|f| @options[:deploys] = f}
      opts.on( '-g', "--groups=FILE", "File containing groups JSON" ){|f| @options[:groups] = f}
      opts.on( '-r', "--repos=FILE", "File containing repositories JSON" ){|f| @options[:repos] = f}
      
      arguments = opts.parse!( args )
      
      if ( arguments && arguments.length > 0 )
        @options[:url] = arguments[0]
      end
    }
  end #initialize
  
  def load
    setup_http
    load_json( DEPLOYS_PATH, @options[:deploys] ) if @options[:deploys]
    load_json( GROUPS_PATH, @options[:groups] ) if @options[:groups]
    load_json( REPOS_PATH, @options[:repos] ) if @options[:repos]
  end #load
  
  private
  def setup_http
    @uri = URI.parse( @options[:url] )
    @http = HTTP.new( @uri.host, @uri.port )
  end #setup_http
  
  def load_json( path, file )
    json = File.read( file )
    obj = JSON.parse( json )
    
    items = obj['items']
    items.each{|item|
      full_path = "#{@uri.path}/#{path}"
      item_json = JSON.generate( item )
      
      puts "\n\nPOST #{full_path}\n#{item_json}\n\n"
      
      post = HTTP::Post.new(full_path)
      post['Content-Type'] = "application/json"
      post.body = item_json
      
      response = @http.request( post )
      
      puts "#{item['key']} --> #{response.code} #{response.message}"
      if ( response.code.to_i != 200 && response.code.to_i != 201 )
        raise "Error: #{response.code}"
      end
    }
  end #load
  
end #class

Loader.new(ARGV).load