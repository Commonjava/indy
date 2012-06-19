function(doc){
	if( doc.store.doctype == 'repository' ){
		emit(doc.store.name,{'_id': doc._id});
	}
}
