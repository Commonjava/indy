function(doc){
	if( doc.store.doctype == 'deploy_point' ){
		emit(doc.store.name,{'_id': doc._id});
	}
}
