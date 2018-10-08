'use strict'

import React from 'react';
import {render} from 'react-dom';
import $ from 'jquery';
import {styles} from './style.js'

const URLList  = (props)=> {
  let elems = [];
  if(props.parentUrl){
    let parentUrl = props.parentUrl.replace("/api/browse", "/browse");
    elems.push(<li key="parent"><a href={parentUrl}>..</a></li>);
  }
  if(props.urls){
    props.urls.forEach((urlResult, index)=>{
      let source = `sources:\n${urlResult.sources.join("\n")}`;
      let url = urlResult.listingUrl.replace("/api/browse", "/browse");
      let paths = urlResult.path.split('/');
      let path = urlResult.path.endsWith("/")? paths[paths.length-2] + "/" : paths[paths.length-1];
      elems.push(<li key={"urlList"+index}><a className="item-link" title={source} href={url} path={urlResult.path}>{path}</a></li>);
    });
  }
  return (
    <ul className="item-listing">
      {elems}
    </ul>
  );
}

const Footer = (props) => {
  let elems = props.sources && props.sources.map((src, index)=>(<li key={"footer"+index}><a className="source-link" title={src} href={src}>{src}</a></li>));
  return(
    <footer>
      <p>Sources for this page:</p>
      <ul>
        {elems}
      </ul>
    </footer>);
}

class URLPage extends React.Component {
  constructor(props){
    super(props);
    this.state = {
      error: null,
      isLoaded: false,
      items: []
    };
  }

  getStoreKey(){
    let storeElems = this.state.data.storeKey.split(":");
    return {
      "packageType": storeElems[0],
      "type": storeElems[1],
      "name": storeElems[2]
    }
  }

  componentDidMount() {
    $.getJSON({
      url: "/api" + document.location.pathname,
      type: "GET",
      responseType: "application/json",
      contentType: "application/json",
      dataType: "json"
    }).done((response) => {
      let result = response;
      this.setState({
        isLoaded: true,
        data: result
      });
    }).fail((jqxhr, textStatus, error) => {
      this.setState({
        isLoaded: true,
        error: error
      });
    });
  }
  
  render() {
    const { error, isLoaded, items } = this.state;
    if (error) {
      return <div>Error: {error}</div>;
    } else if (!isLoaded) {
      return <div>Loading...</div>;
    } else {
      return (
        <div>
          <h2 key="title">Directory listing for {this.state.data.path} on {this.getStoreKey().name}</h2>
          <URLList key="urllist" parentUrl={this.state.data.parentUrl} urls={this.state.data.listingUrls} />
          <Footer key="footer" sources={this.state.data.sources} />
        </div>
      );
    }
  }
}

render(<URLPage/>, document.getElementById('root'));
