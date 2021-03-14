module.exports = (function (){
	var exec = (...args) => {
		cordova.exec.apply(this, args)
	}
	this.enter = function (width, height, success, error) {
		exec(success, error, "PIPPlugin", "enter", [width, height])
	}
	this.initializePip = function (success, error) {
		exec(success, error, "PIPPlugin", "initializePip", [])
	}
	this.isPip = function (success, error) {
		exec(success, error, "PIPPlugin", "isPip", [])
	}
	this.onPipModeChanged = function (success, error) {
		exec(success, error, "PIPPlugin", "onPipModeChanged", [])
	}
	this.isPipModeSupported = function (success, error) {
		exec(success, error, "PIPPlugin", "isPipModeSupported", [])
	}
	return this
})()
