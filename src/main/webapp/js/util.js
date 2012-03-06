// OrderedMap class maps numbers to values or sets
function OrderedMap() { 
	this.data = {}; 
}
OrderedMap.prototype.put = function(key, values) {
	this.data[key] = values;
}
OrderedMap.prototype.get = function(key) {
	return this.data[key];
}
/* returns an array with values of the keys in order */
OrderedMap.prototype.getOrderedKeys = function()  {
	var globalMin = -999999999, ordered = [];
	for (var counting in this.data) {
		var min = 9999999999;
		for (var key in this.data) {
			dkey = parseFloat(key);
			if(dkey<min && dkey>globalMin) min = dkey;
		}
		ordered.push(parseFloat(min));
		globalMin = min;
	}
	return ordered;
}