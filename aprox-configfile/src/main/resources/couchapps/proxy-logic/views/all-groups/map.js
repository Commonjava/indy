function(doc){
	if( doc.doctype == 'group' ){
		emit(doc.name,{'_id': doc._id});
	}
}
