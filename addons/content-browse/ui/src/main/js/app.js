/*
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict'

import React from 'react';
import {render} from 'react-dom';
import {styles} from './style.js';

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
    <ul style={styles.ItemListing}>
      {elems}
    </ul>
  );
}

const Footer = (props) => {
  let elems = props.sources && props.sources.map((src, index)=>(<li key={"footer"+index}><a className="source-link" title={src} href={src}>{src}</a></li>));
  return(
    <footer style={styles.Footer}>
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
      data: {}
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
    fetch("/api" + document.location.pathname, {
      method: "GET",
      credentials: 'same-origin',
      headers: {
        "Content-Type": "application/json",
      }
    }).then(response => {
      if(response.ok){
        response.json().then(data=>{          
          this.setState({
            isLoaded: true,
            data
          });
        });
      }else if(!response.ok){
        response.text().then(data=>{
          this.setState({
            isLoaded: true,
            error: data
          });         
        });
      }
    });    
  }
  
  render() {
    const { error, isLoaded, data } = this.state;
    if (error) {
      return <div>Error: {error}</div>;
    } else if (!isLoaded) {
      return <div>Loading...</div>;
    } else {
      document.title = `Directory listing for ${data.path} on ${this.getStoreKey().name}`;
      return (
        <div>
          <h2 style={styles.Header} key="title">Directory listing for {data.path} on {this.getStoreKey().name}</h2>
          <URLList key="urllist" parentUrl={data.parentUrl} urls={data.listingUrls} />
          <Footer key="footer" sources={data.sources} />
        </div>
      );
    }
  }
}

render(<URLPage/>, document.getElementById('root'));
