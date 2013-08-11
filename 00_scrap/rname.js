var fs = require('fs');

var dir = fs.readdirSync('.');
dir.forEach(function(file) {
  var old = file.split('.')[0];
  var cdtDateStr = old.substr(0, 13) + ':' + old.substr(13,2) + ':' + old.substr(15) + '-0500';
  var cdtDate = new Date(cdtDateStr);
  var newName = cdtDate.toISOString().replace(/:/g, '').substr(0,17) + 'Z.jpg';
  fs.renameSync(old + '.jpg', newName);
});