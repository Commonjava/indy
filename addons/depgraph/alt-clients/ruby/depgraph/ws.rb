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
require 'optparse'
require_relative "constants"
require_relative "http"

module Depgraph
  class Workspace
    def initialize(http)
      @http = http
    end
    
    def create()
      puts "Creating new workspace..."
      ws = nil
      workspace = @http.post('ws/new'){|response|
        response = JSON.parse( response.body )
        ws = response['id']
      }
      
      ws
    end
    
    def delete(workspace)
      puts "Deleting workspace #{workspace}..."
      @http.delete("ws/#{workspace}")
    end
    
    def list()
      puts "Listing workspaces..."
      workspaces = []
      @http.get('ws'){|response|
        parsed = JSON.parse( response.body )
        parsed['items'].each{|ws|
          workspaces << ws['id']
        }
      }
      
      workspaces
    end
    
    def delete_all()
      list().each{|wsid|
        delete(wsid)
      }
    end
  end
  
  private
    class WSDirector

      def initialize( args )
        @options = {
          :verb => 'create',
          :host => HOST,
          :port => PORT,
          :context_path => CONTEXT_PATH,
        }

        OptionParser.new {|opts|

          opts.banner =<<-EOB

#{File.basename($0)} provides access to workspace features of the dependency-graphing addon for Indy.

Usage: 

#{File.basename($0)} [options] create
    Creates a new workspace

#{File.basename($0)} [options] list
    List all workspaces currently available

#{File.basename($0)} [options] delete <wsid>
    Delete the workspace associated with the given workspace-id

          EOB

          opts.on('-H', '--host=HOST', 'Indy hostname'){|host| @options[:host] = host}
          opts.on('-p', '--port=PORT', 'Indy port'){|port| @options[:port] = port.to_i}
          opts.on('-c', '--context=PATH', 'Indy context base-path (default: /indy)'){|context_path| @options[:context_path]=context_path}

          opts.separator ""

          opts.on('-h', '--help', 'Print this help message and exit'){
            puts "#{opts}\n\n"
            exit 0
          }

          arguments = opts.parse!(args)
          if ( !arguments )
            puts "ERROR: Invalid options!\n\n#{opts}\n\n"
            exit 1
          else
            @options[:verb] = arguments[0]

            if arguments[0] == 'delete' && arguments.length < 2
              puts "ERROR: Delete requires a workspace id.\n\n#{opts}\n\n"
              exit 2
            else
              @options[:wsid] = arguments[1]
            end
          end
        }

        @http = Http.new(@options[:host], @options[:port], @options[:context_path])
        @ws = Workspace.new(@http)
      end

      def run()
        if ( @options[:verb] == 'delete')
          @ws.delete( @options[:wsid] )
        else
          puts @ws.send(@options[:verb].to_sym)
        end
      end
    end
end

if ( $0 == __FILE__ )
  Depgraph::WSDirector.new(ARGV).run()
end

