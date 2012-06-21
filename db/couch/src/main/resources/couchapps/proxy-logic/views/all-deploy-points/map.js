function(doc){
	if ( doc.store ){
		if( doc.store.doctype == 'deploy_point' ){
			emit(doc.store.name,{'_id': doc._id});
		}
	}
	else{
		if( doc.doctype == 'deploy_point' ){
			emit(doc.name,{'_id': doc._id});
		}
	}
}
