function(doc){
	if ( ['group', 'repository', 'deploy_store'].indexOf( doc.store.doctype ) > -1 && doc.store.constituents ){
		for( idx in doc.store.constituents ){
			emit(doc.store.constituents[idx], {'_id': doc._id});
		}
	}
}
