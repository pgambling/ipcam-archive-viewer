// start anonymous wrapper
;(function() {

'use strict';

window.APP = window.APP || {};

var SNAPSHOT_LIST = []; // This will be set by an injected script tag
var CURRENT_INDEX = 0;

//-----------------------------------------------------------------------------
// Timepicker
//-----------------------------------------------------------------------------
function buildTimepicker() {
  var html =
  '<div class="timepicker-widget">' +
  '<table>' +
  '<tr>' +
    '<td>' +
      '<a class="incrementHour" href="#"><em class="icon-chevron-up"></em></a>' +
    '</td>' +
    '<td class="separator">&nbsp;</td>' +
    '<td>' +
      '<a class="incrementMinute" href="#"><em class="icon-chevron-up"></em></a>' +
    '</td>' +
    '<td class="separator">&nbsp;</td>' +
    '<td class="meridian-column">' +
      '<a class="toggleMeridian" href="#"><em class="icon-chevron-up"></em></a>' +
    '</td>' +
  '</tr>' +
  '<tr>' +
    '<td>' +
      '<input class="timepicker-hour" maxlength="2" id="hour" type="text">' +
    '</td>' +
    '<td class="separator">:</td>' +
    '<td>' +
      '<input class="timepicker-minute" maxlength="2" id="minute" type="text">' +
    '</td>' +
    '<td class="separator">&nbsp;</td>' +
    '<td>' +
      '<input class="timepicker-meridian" maxlength="2" id="meridian" type="text">' +
    '</td>' +
  '</tr>' +
  '<tr>' +
    '<td>' +
      '<a class="decrementHour" href="#"><em class="icon-chevron-down"></em></a>' +
    '</td>' +
    '<td class="separator"></td>' +
    '<td>' +
      '<a class="decrementMinute" href="#"><em class="icon-chevron-down"></em></a>' +
    '</td>' +
    '<td class="separator">&nbsp;</td>' +
    '<td>' +
      '<a class="toggleMeridian" href="#"><em class="icon-chevron-down"></em></a>' +
    '</td>' +
  '</tr>' +
  '</table>' +
  '</div>';

  return html;
}

function toggleMeridian() {
  var meridianEl = $('#meridian');
  var meridian = meridianEl.val();
  if (meridian === 'AM') {
    meridianEl.val('PM');
  }
  else {
    meridianEl.val('AM');
  }
}

function pad(num) {
  if (num < 10) {
    return '0' + num;
  }

  return num.toString();
}

function updateDigit(inputId, action) {
  var inputEl = $('#' + inputId);
  var currentVal = parseInt(inputEl.val(), 10);
  var newVal = action(currentVal);

  inputEl.val(pad(newVal));
}

function doDecMinute(currentVal) {
  var newVal = currentVal-1;
  if (newVal < 0) {
    newVal = 59;
    decrementHour();
  }
  return newVal;
}

function doIncMinute(currentVal) {
  var newVal = currentVal+1;
  if (newVal > 59) {
    newVal = 0;
    incrementHour();
  }
  return newVal;
}

function doDecHour(currentVal) {
  var newVal = currentVal-1;
  if (newVal <= 0) {
    newVal = 12;
  }

  if (newVal === 11) {
    toggleMeridian();
  }

  return newVal;
}

function doIncHour(currentVal) {
  var newVal = currentVal+1;
  if (newVal > 12) {
    newVal = 1;
  }

  if (newVal === 12) {
    toggleMeridian();
  }

  return newVal;
}

function decrementMinute() {
  updateDigit('minute', doDecMinute);
}

function incrementMinute() {
  updateDigit('minute', doIncMinute);
}

function decrementHour() {
  updateDigit('hour', doDecHour);
}

function incrementHour() {
  updateDigit('hour', doIncHour);
}

function clickDecrementMinute(e) {
  decrementMinute();
  onDateTimeChange();
  e.preventDefault();
}

function clickIncrementMinute(e) {
  incrementMinute();
  onDateTimeChange();
  e.preventDefault();
}

function clickDecrementHour(e) {
  decrementHour();
  onDateTimeChange();
  e.preventDefault();
}

function clickIncrementHour(e) {
  incrementHour();
  onDateTimeChange();
  e.preventDefault();
}

function clickToggleMeridian(e) {
  toggleMeridian();
  onDateTimeChange();
  e.preventDefault();
}

function setTimePicker(time) {
  var hour = time.getHours();
  var meridian = 'AM';
  if (hour > 11) {
    meridian = 'PM';
  }
  if (hour > 12) {
    hour -= 12;
  }
  $('#hour').val(pad(hour));
  $('#minute').val(pad(time.getMinutes()));
  $('#meridian').val(meridian);
}

function getTimePicker() {
  var hour = parseInt($('#hour').val(), 10);
  var minute = parseInt($('#minute').val(), 10);
  var meridian = $('#meridian').val();

  if (meridian === 'PM' && hour < 12) {
    hour += 12;
  }

  if (meridian === 'AM' && hour === 12) {
    hour = 0;
  }

  return {
    hour: hour,
    minute: minute,
    meridian: meridian
  };
}

//-----------------------------------------------------------------------------
// Main Page Functionality
//-----------------------------------------------------------------------------

function initDatepicker() {
  var startDate = new Date(SNAPSHOT_LIST[SNAPSHOT_LIST.length-1].time);
  var endDate = new Date(SNAPSHOT_LIST[0].time);

  $('#datepicker').datepicker({
    startDate: startDate,
    endDate: endDate
  });
}

function setDateTime(time) {
  setTimePicker(time);
  $('#datepicker').datepicker('update', time);
}

function getDateTime() {
  var dt = $('#datepicker').datepicker('getDate');
  var time = getTimePicker();
  var hour = time.hour;
  dt.setHours(hour);
  dt.setMinutes(time.minute);
  return dt;
}

function onDateTimeChange() {
  var time = getDateTime().getTime();
  var earlier, later;

  for (var i=1, len=SNAPSHOT_LIST.length; i<len; i++) {
    later = SNAPSHOT_LIST[i-1].time;
    earlier = SNAPSHOT_LIST[i].time;

    if ((time > later) || (time < earlier)) continue;

    // pick the snapshot closest to the selected time
    if ((later-time) < (time-earlier)) { // choose later
      CURRENT_INDEX = i-1;
    }
    else { // choose earlier
      CURRENT_INDEX = i;
    }

    return update(false);
  }
}

function setSnapshotList(snapshotList) {
  SNAPSHOT_LIST = snapshotList;
}

function update(updateDateTimeInput) {
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

  if (updateDateTimeInput !== false) {
    setDateTime(time);
  }
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
  var bodyEl = $('body');

  bodyEl.on('click', '.toggleMeridian', clickToggleMeridian);
  bodyEl.on('click', '.decrementHour', clickDecrementHour);
  bodyEl.on('click', '.incrementHour', clickIncrementHour);
  bodyEl.on('click', '.decrementMinute', clickDecrementMinute);
  bodyEl.on('click', '.incrementMinute', clickIncrementMinute);

  bodyEl.on('click', '#later', clickLater);
  bodyEl.on('click', '#earlier', clickEarlier);

  $('#datepicker').on('changeDate', onDateTimeChange);

  $(document).keydown(keydown);
}

function init() {
  initDatepicker();
  $('#timepickerContainer').html(buildTimepicker());

  update();
  addEvents();
}
$(document).ready(init);

//-----------------------------------------------------------------------------
// Global Exports
//-----------------------------------------------------------------------------
APP.setSnapshotList = setSnapshotList;

}()); // end anonymous wrapper
