function(doc){
	if( doc.store.doctype == 'group' ){
		emit(doc.store.name,{'_id': doc._id});
	}
}
