function(doc){
	if ( ['group', 'repository', 'deploy_store'].indexOf( doc.doctype ) > -1 && doc.constituents ){
		for( var i=0; i<doc.constituents.length; i++){
			emit([doc.name,i], {'_id': doc.constituents[i]});
		}
	}
}
