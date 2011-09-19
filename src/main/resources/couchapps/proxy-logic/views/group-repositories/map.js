function(doc){
	if ( doc.doctype == 'group' && doc.constituents ){
		for( idx in doc.constituents ){
			emit(doc.name, {'_id': 'repository:' + doc.constituents[idx]});
		}
	}
}