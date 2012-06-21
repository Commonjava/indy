function(doc){
	if ( doc.store ){
		if( doc.store.doctype == 'repository' ){
			emit(doc.store.name,{'_id': doc._id});
		}
	}
	else{
		if( doc.doctype == 'repository' ){
			emit(doc.name,{'_id': doc._id});
		}
	}
}
