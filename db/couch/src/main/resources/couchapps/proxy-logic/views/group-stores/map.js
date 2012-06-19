function(doc){
	if ( ['group', 'repository', 'deploy_store'].indexOf( doc.store.doctype ) > -1 && doc.store.constituents ){
		for( var i=0; i<doc.store.constituents.length; i++){
			emit([doc.store.name,i], {'_id': doc.store.constituents[i]});
		}
	}
}
