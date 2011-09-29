function(doc){
	if ( doc.doctype == 'group' && doc.constituents ){
		for( idx in doc.constituents ){
			emit(doc.name, {'_id': doc.constituents[idx]});
		}
	}
}
