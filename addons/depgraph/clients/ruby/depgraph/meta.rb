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


require 'json/pure'
require 'time'
require 'optparse'
require_relative "constants"
require_relative "http"
require_relative "ws"

module Depgraph
  class Metadata
    
    def initialize( ws, http, options, config )
      @ws = ws
      @http = http
      @options = options
      @config = config
      init_workspace()
      generate_config_file()
    end
    
    def collate()
      puts "collate started #{Time.now}"
    
      filename = "collate-#{@workspace}.json"
      @http.post('meta/collate', @config){|response|
        File.open(filename, 'w+'){|f|
          f.puts(response.body)
        }
      
        puts "Wrote collate to: #{filename}"
      }
    
      puts "collate ended #{Time.now}"
    end
    
    def shutdown()
      detach_workspace()
    end
    
    private
    def generate_config_file()
      File.open("#{@options[:config_prefix]}-#{@workspace}.json", 'w+'){|f|
        f.puts JSON.pretty_generate(@config)
      }
    end
    
    def init_workspace()
      if ( @options[:create_ws] )
        @workspace = @ws.create()
    
        @config['workspaceId'] = @workspace
      else
        @workspace = @config['workspaceId']
      end
      
      puts "Using workspace: #{@workspace}"
    end
    
    def detach_workspace(options)
      if ( options[:delete_ws] )
        @ws.delete( workspace )
      end
    end
  end
  
  private
  class MetadataDirector
  
    CONFIG_OUT_PREFIX = 'config'
    VERB = 'collate'
    OUTPUT_PREFIX = "#{VERB}"
  
    def initialize( args )
      @options = {
        :config_prefix => CONFIG_OUT_PREFIX,
        :output_prefix => OUTPUT_PREFIX,
        :verb => VERB,
        :host => HOST,
        :port => PORT,
        :context_path => CONTEXT_PATH,
      }
    
      OptionParser.new {|opts|
      
        opts.banner =<<-EOB

  #{File.basename($0)} provides access to some features of the dependency-graphing addon for Aprox.

  Usage: #{File.basename($0)} [options] <config-file>

        EOB
      
        opts.on('-c', '--context=PATH', 'Aprox context base-path (default: /aprox)'){|context_path| @options[:context_path]=context_path}
        opts.on('-C', '--config-prefix=PREFIX', 'Filename prefix for the configuration generated when a new workspace is created'){|prefix| @options[:config_prefix] = prefix}
        opts.on('-H', '--host=HOST', 'Aprox hostname'){|host| @options[:host] = host}
        opts.on('-O', '--output-prefix=PREFIX', 'Filename prefix for the output JSON'){|prefix| @options[:output_prefix] = prefix}
        opts.on('-p', '--port=PORT', 'Aprox port'){|port| @options[:port] = port.to_i}
        opts.on('-v', '--verb=VERB', 'Depgraph metadata endpoint to use (default: collate)'){|verb| @options[:verb] = verb}
        opts.on('-w', '--create-workspace', 'Create a new workspace to use in the call'){@options[:create_ws] = true}
        opts.on('-W', '--delete-workspace', 'Delete the workspace used in the call after it runs'){@options[:delete_ws] = true}
      
        opts.separator ""
      
        opts.on('-h', '--help', 'Print this help message and exit'){
          puts "#{opts}\n\n"
          exit 0
        }
      
        config_files = opts.parse!(args)
        if ( !config_files || config_files.length != 1 )
          puts "ERROR: You must specify exactly ONE config file!\n\n#{opts}\n\n"
          exit 1
        end
      
        @options[:config_file] = config_files[0]
      }
    
      config = File.open(@options[:config_file]){|f|
        json= f.read
        puts "Parsing:\n\n#{json}\n\n"
        JSON.parse(json)
      }
  
      http = Http.new(@options[:host], @options[:port], @options[:context_path])
      ws = Workspace.new(http)
      @meta = Metadata.new(ws, http, @options, config)
    end
  
    def run()
      puts @meta.send(@options[:verb].to_sym)
    end
  end
end

if ( $0 == __FILE__ )
  Depgraph::MetadataDirector.new(ARGV).run()
end


