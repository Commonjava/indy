require 'net/http'
require 'json/pure'

module Depgraph
  class Http
    include Net
    
    DG_BASEPATH = '/aprox/api/1.0/depgraph'
  
    def initialize( host, port )
      @http = HTTP.new(host, port)
      @http.read_timeout = 10800
    end
    
    def get(subpath, &block)
      req_path = "#{DG_BASEPATH}/#{subpath}"
      puts "Querying: #{req_path}"

      req = HTTP::Get.new( req_path )
    
      response = @http.request(req)

      if ( response.code == '200' || response.code == '201' )
        block.call(response.body)
      else
        raise "ERR: #{response.code} #{response.message}"
      end
    end
    
    def delete(subpath)
      req_path = "#{DG_BASEPATH}/#{subpath}"
      puts "Deleting: #{req_path}"

      req = HTTP::Delete.new( req_path )
    
      response = @http.request(req)

      if ( response.code != '200' )
        raise "ERR: #{response.code} #{response.message}"
      end
    end
  
    def post(subpath, payload=nil, &block)
      req_path = "#{DG_BASEPATH}/#{subpath}"
      puts "Posting: #{req_path}"

      if ( payload )
        req = HTTP::Post.new( req_path, initheader = {'Content-Type' =>'application/json'})
        req.body = JSON.pretty_generate(payload)
      else
        req = HTTP::Post.new( req_path )
      end
    
      response = @http.request(req)

      if ( response.code == '200' || response.code == '201' )
        block.call(response.body)
      else
        raise "ERR: #{response.code} #{response.message}"
      end
    end
  
  end
end