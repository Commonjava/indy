function(doc){
	if( doc.doctype == 'repository' ){
		emit(doc.name,{'_id': doc._id});
	}
}
