(function($) {
	"use strict";

	function FileUpload(originalInput, options) {
		var self = this;
		this.internalChange = this.internalChange.bind(this);
		this.internalDragOver = this.internalDragOver.bind(this);
		this.internalDrop = this.internalDrop.bind(this);
		this.internalKeyPress = this.internalKeyPress.bind(this);
		this.internalProgress = this.internalProgress.bind(this);
		this.internalStop = this.internalStop.bind(this);

		this.originalInput = $(originalInput).on('change', this.internalChange);
		this.options = $.extend({}, FileUpload.defaultOptions, options);
		if (this.options.dropSelector)
			$(this.options.dropSelector).on('dragover', this.internalDragOver).on('drop', this.internalDrop);
		if (this.options.abortOnEscape)
			$(document).on('keypress', this.internalKeyPress);

		// seulement pendant l'upload
		this.uploadFiles = null;
		this.uploadTotalSize = null;
		this.uploadStartTime = null;
		this.uploadPercent = null;
		this.uploadAjax = null;
	}

	$.extend(FileUpload.prototype, {
		internalChange: function(event) {
			this.start(event.target.files);
		},
		internalCheckFileDragEvent: function(event) {
			if (!event.originalEvent || !event.originalEvent.dataTransfer || !event.originalEvent.dataTransfer.types)
				return false;
			if (event.originalEvent.dataTransfer.types.indexOf)
				return event.originalEvent.dataTransfer.types.indexOf('Files') >= 0; // OK pour FF et Chrome
			return event.originalEvent.dataTransfer.types.contains('Files'); // OK pour Edge
		},
		internalDragOver: function(event) {
			if (this.internalCheckFileDragEvent(event)) {
				event.preventDefault();
			}
		},
		internalDrop: function(event) {
			if (this.internalCheckFileDragEvent(event)) {
				event.preventDefault();
				this.start(event.originalEvent.dataTransfer.files);
			}
		},
		internalKeyPress: function(event) {
			if (this.uploadAjax && event.originalEvent.key === 'Escape')
				this.abort();
		},
		internalProgress: function(event) {
			var e = event.originalEvent;
			// console.log('progress');
			if (this.options.onprogress && e.lengthComputable) {
				var percent = Math.floor(e.loaded * 100.0 / e.total);
				if (percent > this.uploadPercent) {
					// console.log('progress', percent);
					this.uploadPercent = percent;
					this.options.onprogress(this.uploadFiles, e.total, Date.now() - this.uploadStartTime, e.loaded, percent);
				}
			}
		},
		internalStop: function() {
			if (this.options.onstop)
				this.options.onstop(this.uploadFiles, this.uploadTotalSize, Date.now() - this.uploadStartTime);
			this.uploadFiles = null;
			this.uploadTotalSize = null;
			this.uploadStartTime = null;
			this.uploadPercent = null;
			this.uploadAjax = null;
		},
		start: function(files) {
			var self = this;
			var defer = this.options.onstart ? this.options.onstart(files) : $.Deferred().resolve();
			defer.then(function() {
				// Send files and extraParams as FormData
				var formData = new FormData();

				// Add extraParams, if specified
				if (self.options.extraParams) {
					var extraParams;
					if (typeof self.options.extraParams === 'function')
						extraParams = self.options.extraParams(files);
					else
						extraParams = self.options.extraParams;
					for (var p in extraParams) {
						if (extraParams.hasOwnProperty(p))
							formData.append(p, extraParams[p]);
					}
				}

				// Add files
				var paramName = self.originalInput.attr('name');
				for (var i = 0; i < files.length; i++) {
					formData.append(paramName, files[i], files[i].name);
				}

				// Create a native XMLHttpRequest to access progress 
				var xhr = $.ajaxSettings.xhr();
				$(xhr.upload).on('progress', self.internalProgress);

				// OK, start upload
				self.uploadFiles = files;
				self.uploadStartTime = Date.now();
				self.uploadTotalSize = Array.prototype.reduce.apply(files, [function(total, file) { return total + file.size; }, 0]);
				self.uploadPercent = 0;
				self.uploadAjax = $.ajax({
					method: self.options.method,
					url: self.options.url,
					data: formData,
					dataType: 'json',
					contentType: false,
					processData: false,
					cache: false,
					timeout: 0,
					xhr: function() { return xhr; } 
				});
				if (self.options.onerror)
					self.uploadAjax.fail(function() {
						self.options.onerror(files);
					});
				self.uploadAjax.always(self.internalStop);

				// To test abort, uncomment the next line
				// setTimeout(function() { self.abort(); }, 1000);

			}, function() {
				// console.log('upload canceled by user');
			});
		},
		abort: function() {
			if (this.uploadAjax)
				this.uploadAjax.abort();
		},
		destroy: function() {
			this.originalInput.off('change', this.start);
			if (this.options.dropSelector)
				$(this.options.dropSelector).off('dragover', this.internalDragOver).off('drop', this.internalDrop);
			if (this.options.abortOnEscape)
				$(document).off('keypress', this.internalKeyPress);
		}
	});

	FileUpload.defaultOptions = {
		/* mandatory string or function(input)->string to get the server url where file(s) are uploaded to */
		url: null,
		/* optional string option to specify HTTP request method (default method is 'POST') */
		method: 'POST',
		/* optional jquery selector to activate file drop (default behaviour is to support file drop on the whole document, use "null" to disable file drop support) */
		dropSelector: document,
		/* enable ou disable upload abort on escape keyup */
		abortOnEscape: true,
		/* optional object or function(files)->object giving user the ability to add contextual information in uploaded form */
		extraParams: null,
		/* optional callback function(files)->Promise called before the upload starts, giving user the ability to (temporary) delay upload (for instance to check if file exists and ask confirmation) */
		onstart: null,
		/* optional callback function(files, total, duration, bytes, percent) called to follow upload progress */
		onprogress: null,
		/* optional callback function(files) called to indicate that something went wrong */
		onerror: null,
		/* optional callback function(files, total, duration) called when upload is over */
		onstop: null,
	};

	$.addPlugin('fileupload', FileUpload);

})(jQuery);
