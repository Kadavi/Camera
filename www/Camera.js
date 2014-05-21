var exec = require('cordova/exec');
var argscheck = require('cordova/argscheck')
var CheckCapture = {}
CheckCapture.getPicture = function(successCallback, failureCallback, options) {
		argscheck.checkArgs('fFO', 'CheckCapture.getPicture', arguments);
		var getValue = argscheck.getValue;
		var title = getValue(options.title, 'Title');
		var quality = getValue(options.quality, 100);
		var targetWidth = getValue(options.targetWidth, -1);
		var targetHeight = getValue(options.targetHeight, -1);
		var logoFilename = getValue(options.logoFilename);
		var description = getValue(options.description);
		var args = [title, quality, targetWidth, targetHeight, logoFilename, description];
		exec(successCallback, failureCallback, "CheckCapture", "takePicture", args);
	}

module.exports = CheckCapture;

