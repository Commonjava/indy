function(doc){
	if( doc.doctype == 'deploy_point' ){
		emit(doc.name,{'_id': doc._id});
	}
}
