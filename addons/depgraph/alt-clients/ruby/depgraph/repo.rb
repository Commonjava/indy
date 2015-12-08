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
  class Repository
    
    def initialize( ws, http, options, config )
      @ws = ws
      @http = http
      @options = options
      @config = config
      init_workspace()
      generate_config_file()
    end
    
    def urlmap()
      filename = "urlmap-#{@workspace}.json"
      @http.post('repo/urlmap', @config){|response|
        File.open(filename, 'w+'){|f|
          f.puts(response.body)
        }
      
        puts "Wrote urlmap to: #{filename}"
      }
    end
    
    def downlog()
      filename = "downlog-#{@workspace}.txt"
      @http.post('repo/downlog', @config){|response|
        File.open(filename, 'w+'){|f|
          f.puts(response.body)
        }
      
        puts "Wrote downlog to: #{filename}"
      }
    end
    
    def zip()
      filename = "repo-#{@workspace}.zip"
      File.open(filename, 'w+'){|f|
        @http.post('repo/zip', @config){|response|
          #response.read_body {|seg|
          #  f.write(seg)
          #}
          f.write(response.body)
        }
      }
    
      puts "Wrote repo.zip to: #{filename}"
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
  class RepositoryDirector
  
    CONFIG_OUT_PREFIX = 'config'
    VERB = 'urlmap'
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

  #{File.basename($0)} provides access to some features of the dependency-graphing addon for Indy.

  Usage: #{File.basename($0)} [options] <config-file>

        EOB
      
        opts.on('-c', '--context=PATH', 'Indy context base-path (default: /)'){|context_path| @options[:context_path]=context_path}
        opts.on('-C', '--config-prefix=PREFIX', 'Filename prefix for the configuration generated when a new workspace is created'){|prefix| @options[:config_prefix] = prefix}
        opts.on('-H', '--host=HOST', 'Indy hostname'){|host| @options[:host] = host}
        opts.on('-O', '--output-prefix=PREFIX', 'Filename prefix for the output JSON'){|prefix| @options[:output_prefix] = prefix}
        opts.on('-p', '--port=PORT', 'Indy port'){|port| @options[:port] = port.to_i}
        opts.on('-v', '--verb=VERB', 'Depgraph repository endpoint to use (default: urlmap)'){|verb| @options[:verb] = verb}
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
        JSON.parse(f.read)
      }
  
      http = Http.new(@options[:host], @options[:port], @options[:context_path])
      ws = Workspace.new(http)
      @repo = Repository.new(ws, http, @options, config)
    end
  
    def run()
      verb = @options[:verb]
      puts "#{verb} started #{Time.now}"
      begin
        puts @repo.send(verb.to_sym)
      rescue Exception => e
        puts e.message
        puts e.backtrace.inspect
      ensure
        puts "#{verb} ended #{Time.now}"
      end
    end
  end
end

if ( $0 == __FILE__ )
  Depgraph::RepositoryDirector.new(ARGV).run()
end


