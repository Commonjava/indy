#!/usr/bin/env ruby

require 'json/pure'
require 'time'
require 'optparse'
require_relative "constants"
require_relative "http"
require_relative "ws"

module Depgraph
  class Render
    
    def initialize( ws, http, options, config )
      @ws = ws
      @http = http
      @options = options
      @config = config
      init_workspace()
      generate_config_file()
    end
    
    def tree()
      puts "tree started #{Time.now}"
    
      filename = "tree-#{@workspace}.json"
      @http.post('render/tree', @config){|response|
        File.open(filename, 'w+'){|f|
          f.puts(response.body)
        }
      
        puts "Wrote tree to: #{filename}"
      }
    
      puts "tree ended #{Time.now}"
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
  class RenderDirector
  
    CONFIG_OUT_PREFIX = 'config'
    VERB = 'tree'
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

  #{File.basename($0)} provides access to rendering features of the dependency-graphing addon for Aprox.

  Usage: #{File.basename($0)} [options] <config-file>

        EOB
      
        opts.on('-c', '--context=PATH', 'Aprox context base-path (default: /aprox)'){|context_path| @options[:context_path]=context_path}
        opts.on('-C', '--config-prefix=PREFIX', 'Filename prefix for the configuration generated when a new workspace is created'){|prefix| @options[:config_prefix] = prefix}
        opts.on('-H', '--host=HOST', 'Aprox hostname'){|host| @options[:host] = host}
        opts.on('-O', '--output-prefix=PREFIX', 'Filename prefix for the output JSON'){|prefix| @options[:output_prefix] = prefix}
        opts.on('-p', '--port=PORT', 'Aprox port'){|port| @options[:port] = port.to_i}
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
      @renderer = Render.new(ws, http, @options, config)
    end
  
    def run()
      puts @renderer.send(@options[:verb].to_sym)
    end
  end
end

if ( $0 == __FILE__ )
  Depgraph::RenderDirector.new(ARGV).run()
end


