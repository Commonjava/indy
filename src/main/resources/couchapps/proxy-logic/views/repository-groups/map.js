function(doc){
	if ( doc.doctype == 'group' && doc.constituents ){
		for( idx in doc.constituents ){
			emit(doc.constituents[idx], {'_id': doc._id});
		}
	}
}
