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

require 'net/http'
require 'json/pure'

module Depgraph
  class Http
    include Net
    
    DG_BASEPATH = 'api/depgraph'
  
    def initialize( host, port, context_path=CONTEXT_PATH )
      @host = host
      @port = port
      @http = HTTP.new(host, port)
      @http.read_timeout = 21600
      @context_path = context_path
    end
    
    def get(subpath, &block)
      req_path = "#{@context_path}#{DG_BASEPATH}/#{subpath}"
      puts "Querying: #{req_path}"

      req = HTTP::Get.new( req_path )
    
      response = @http.request(req)

      if ( response.code == '200' || response.code == '201' )
        block.call(response)
      else
        raise "ERR: #{response.code} #{response.message}"
      end
    end
    
    def delete(subpath)
      req_path = "#{@context_path}#{DG_BASEPATH}/#{subpath}"
      puts "Deleting: #{req_path}"

      req = HTTP::Delete.new( req_path )
    
      response = @http.request(req)

      if ( response.code != '200' )
        raise "ERR: #{response.code} #{response.message}"
      end
    end
  
    def post(subpath, payload=nil, &block)
      req_path = "#{@context_path}#{DG_BASEPATH}/#{subpath}"
      puts "Posting: http://#{@host}:#{@port}#{req_path}"

      if ( payload )
        req = HTTP::Post.new( req_path, initheader = {'Content-Type' =>'application/json'})
        req.body = JSON.pretty_generate(payload)
      else
        req = HTTP::Post.new( req_path )
      end
    
      response = @http.request(req)

      if ( response.code == '200' || response.code == '201' )
        block.call(response)
      else
        raise "ERR: #{response.code} #{response.message}"
      end
    end
  
  end
end
