//-----------------------------------------------------------------------------
// Dependencies
//-----------------------------------------------------------------------------
var fs = require('fs');
var async = require('async');
var express = require('express');

//
// Load the app config settings from file
//
if(! fs.existsSync('./config.json')) {
  console.error('ERROR: config.json not found.');
  console.error('HINT: Use example_config.json to start a new one.');
  process.exit(1);
}
var CONFIG = require('./config.json');

//-----------------------------------------------------------------------------
// Main Page
//-----------------------------------------------------------------------------
function generateSnapshotList(callback) {
  // get the list of snapshot files
  fs.readdir(CONFIG.ARCHIVE_DIR, function(err, files) {
    if (err) throw new Error('Error reading snapshot directory!');

    var images = files.filter(function(fname) {
      return fname.substr(fname.length - 3) === 'jpg';
    });

    // get the modify time of each snapshot
    var addModifyTime = function(image, callback) {
      fs.stat(CONFIG.ARCHIVE_DIR + '/' + image, function(err, stat) {
        var result = { image: image, time: stat.mtime.getTime() };
        callback(err, result);
      });
    };
    async.map(images, addModifyTime, function(err, results) {
      // sort the snapshots chronological
      var sortedFiles = results.sort(function(a,b) {
        return b.time - a.time;
      });

      callback(JSON.stringify(sortedFiles));
    });
  });
}

function buildIndex(json) {
  var html =
  '<!doctype html>' +
  '<html>' +
  '<head>' +
    '<meta charset="utf-8">' +
    '<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">' +
    '<title>Front Door Archive</title>' +
    '<meta name="viewport" content="width=device-width">' +
    '<!-- HTML5 shim, for IE6-8 support of HTML elements -->' +
    '<!--[if lt IE 9]>' +
        '<script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>' +
    '<![endif]-->' +
    '<link rel="stylesheet" type="text/css" href="css/bootstrap-2.3.2.min.css">' +
    '<link rel="stylesheet" type="text/css" href="css/bootstrap-datepicker-1.1.3.css">' +
    '<link rel="stylesheet" type="text/css" href="css/main.css">' +
  '</head>' +
  '<body>' +
    '<div id="bodyContainer">' +
      '<h1 id="imageDateTime">Loading...</h1>' +
      '<div id="imageContainer">' +
        '<img id="currentSnapshot" src="#" alt="camera snapshot" />' +
        '<div id="controls">' +
          '<button id="earlier" class="btn btn-large">&laquo Earlier</button>' +
          '<button id="later" class="btn btn-large">Later &raquo</button>' +
        '</div>' +
      '</div>' +
      '<div id="datetimeInputContainer">' +
        '<div id="datepicker"></div>' +
        '<div id="timepickerContainer"></div>' +
      '</div>' +
    '</div>' +
    '<script src="js/jquery-1.10.1.min.js"></script>' +
    '<script src="js/bootstrap-datepicker-1.1.3.js"></script>' +
    '<script src="js/index.js"></script>' +
    '<script>window.APP.setSnapshotList(' + json + ');</script>' +
  '</body>' +
  '</html>';

  return html;
}


//-----------------------------------------------------------------------------
// Create Express App
//-----------------------------------------------------------------------------
var app = express();

app.use(express.compress());

//-----------------------------------------------------------------------------
// Page Routes
//-----------------------------------------------------------------------------
app.get('/', function(req, res) {
  generateSnapshotList(function(json) {
    res.send(buildIndex(json));
  });
});

//-----------------------------------------------------------------------------
// Static Files
//-----------------------------------------------------------------------------
var oneYear = 365*24*60*60*1000;
// app static assets
app.use(express.static(__dirname + '/public', { maxAge: oneYear } ));
// snapshot archive
app.use(express.static(CONFIG.ARCHIVE_DIR, { maxAge: oneYear } ));

// anything else 404s
app.use(function(req, res) {
  res.send(404);
});


//-----------------------------------------------------------------------------
// App Startup
//-----------------------------------------------------------------------------
app.listen(CONFIG.PORT);
console.log('Listening on port ' + CONFIG.PORT);
