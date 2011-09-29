function(doc){
	if ( ['group', 'repository', 'deploy_store'].indexOf( doc.doctype ) > -1 && doc.constituents ){
		for( idx in doc.constituents ){
			emit(doc.name, {'_id': doc.constituents[idx]});
		}
	}
}
