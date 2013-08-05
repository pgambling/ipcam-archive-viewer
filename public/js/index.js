// start anonymous wrapper
;(function() {

'use strict';

window.APP = window.APP || {};

var SNAPSHOT_LIST = []; // This will be set by an injected script tag
var CURRENT_INDEX = 0;

function setSnapshotList(snapshotList) {
  SNAPSHOT_LIST = snapshotList;
}

function update() {
  if (CURRENT_INDEX >= SNAPSHOT_LIST.length) {
    CURRENT_INDEX = SNAPSHOT_LIST.length - 1;
  }

  if (CURRENT_INDEX < 0) {
    CURRENT_INDEX = 0;
  }

  var snapshot = SNAPSHOT_LIST[CURRENT_INDEX];
  $('img#currentSnapshot').attr('src', snapshot.image);

  var time = new Date(snapshot.time);
  $('h1#imageDateTime').text(time.toDateString() + ' ' + time.toLocaleTimeString());

  $('button#earlier').prop('disabled', CURRENT_INDEX === SNAPSHOT_LIST.length-1);
  $('button#later').prop('disabled', CURRENT_INDEX === 0);
}

function clickLater() {
  CURRENT_INDEX--;
  update();
}

function clickEarlier() {
  CURRENT_INDEX++;
  update();
}

function keydown(e) {
  if (e.which === 37) { // left
    clickEarlier();
  }
  else if (e.which === 39) { // right
    clickLater();
  }
}

function addEvents() {
  $('button#later').click(clickLater);
  $('button#earlier').click(clickEarlier);

  $(document).keydown(keydown);
}

function init() {
  update();
  addEvents();
}
$(document).ready(init);

//-----------------------------------------------------------------------------
// Global Exports
//-----------------------------------------------------------------------------
APP.setSnapshotList = setSnapshotList;

}()); // end anonymous wrapper
