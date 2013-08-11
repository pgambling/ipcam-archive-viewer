//-----------------------------------------------------------------------------
// Dependencies
//-----------------------------------------------------------------------------
var fs = require('fs');
var path = require('path');
var http = require('http');
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
// Camera image management
//-----------------------------------------------------------------------------
function generateFileName(date) {
  return date.toISOString().replace(/:/g, '').substr(0,17) + 'Z.jpg';
}

function filenameToDate(fname) {
  var dateStr = fname.split('.')[0];
  dateStr = dateStr.substr(0, 13) + ':' + dateStr.substr(13,2) + ':' + dateStr.substr(15);

  return new Date(dateStr);
}

function downloadSnapshot() {
  http.get(CONFIG.CAMERA_URL, function(response) {
    var now = new Date();
    var filePath = path.join(CONFIG.ARCHIVE_DIR, generateFileName(now));
    var file = fs.createWriteStream(filePath);

    response.pipe(file);

    file.on('finish', function() { file.close(); });
  });
}

function deleteOldImages() {
  var now = new Date();
  var earliestDateToKeep = now.setDate(now.getDate() - CONFIG.NUM_DAYS_ARCHIVE);

  generateSnapshotList(function(snapshots) {
    snapshots.forEach(function(file) {
      if (file.time < earliestDateToKeep) {
        fs.unlink(path.join(CONFIG.ARCHIVE_DIR, file.image));
      }
    });
  });
}

// TODO: This could be cached and updated only when new files arrive
//       rather than every page load.
function generateSnapshotList(callback) {
  // get the list of snapshot files
  fs.readdir(CONFIG.ARCHIVE_DIR, function(err, files) {
    if (err) throw new Error('Error reading snapshot directory!');

    var images = [];
    files.forEach(function(fname) {
      if (fname.substr(fname.length - 3) !== 'jpg') return;

      // use the filenames timestamp
      images.push({
        image: fname,
        time: filenameToDate(fname).getTime()
      });
    });

    // sort the snapshots chronological
    var sortedFiles = images.sort(function(a,b) {
      return b.time - a.time;
    });

    callback(sortedFiles);
  });
}

//-----------------------------------------------------------------------------
// Main Page
//-----------------------------------------------------------------------------
function buildIndex(snapshotList) {
  var html =
  '<!doctype html>' +
  '<html>' +
  '<head>' +
    '<meta charset="utf-8">' +
    '<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">' +
    '<title>' + CONFIG.TITLE + '</title>' +
    '<meta name="viewport" content="width=device-width">' +
    '<!-- HTML5 shim, for IE6-8 support of HTML elements -->' +
    '<!--[if lt IE 9]>' +
        '<script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>' +
    '<![endif]-->' +
    '<link rel="stylesheet" type="text/css" href="css/bootstrap-2.3.2.min.css">' +
    '<link rel="stylesheet" type="text/css" href="css/bootstrap-datepicker-20130809.css">' +
    '<link rel="stylesheet" type="text/css" href="css/main.css">' +
  '</head>' +
  '<body>' +
    '<div id="bodyContainer">' +
      '<h1 id="imageDateTime">Loading...</h1>' +
      '<div id="imageContainer">' +
        '<img id="currentSnapshot" src="' + snapshotList[0].image + '" alt="camera snapshot" />' +
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
    '<script src="js/bootstrap-datepicker-20130809.js"></script>' +
    '<script src="js/index.js"></script>' +
    '<script>window.APP.setSnapshotList(' + JSON.stringify(snapshotList) + ');</script>' +
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
  generateSnapshotList(function(snapshotList) {
    res.send(buildIndex(snapshotList));
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
// start polling camera
downloadSnapshot();
setInterval(downloadSnapshot, CONFIG.POLL_INTERVAL * 1000);

// delete old images once an hour
deleteOldImages();
setInterval(deleteOldImages, 60 * 60 * 1000);

// start web server
app.listen(CONFIG.PORT);
console.log('Listening on port ' + CONFIG.PORT);
