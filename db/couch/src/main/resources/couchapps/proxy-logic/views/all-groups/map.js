function(doc){
	if ( doc.store ){
		if( doc.store.doctype == 'group' ){
			emit(doc.store.name,{'_id': doc._id});
		}
	}
	else{
		if( doc.doctype == 'group' ){
			emit(doc.name,{'_id': doc._id});
		}
	}
}
