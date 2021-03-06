/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") +  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
var VideoUtil = (function() {
	var self = {};
	function _getVid(uid) {
		return "video" + uid;
	}
	function _isSharing(c) {
		return 'sharing' === c.type && c.screenActivities.indexOf('sharing') > -1;
	}
	function _isSharing(c) {
		return 'sharing' === c.type && c.screenActivities.indexOf('sharing') > -1;
	}
	function _hasAudio(c) {
		return c.activities.indexOf('broadcastA') > -1;
	}
	function _hasVideo(c) {
		return c.activities.indexOf('broadcastV') > -1;
	}
	self.getVid = _getVid;
	self.isSharing = _isSharing;
	self.hasAudio = _hasAudio;
	self.hasVideo = _hasVideo;
	return self;
})();
var Video = (function() {
	var self = {}, c, box, v, vc, t, swf, size, vol;

	function _getName() {
		return c.user.firstName + ' ' + c.user.lastName;
	}
	function _resetSize(_w, _h) {
		var w = _w || size.width, h = _h || size.height;
		v.dialog("option", "width", w).dialog("option", "height", t.height() + h + 2);
		_setSize(w, h);
	}
	function _setSize(w, h) {
		vc.width(w).height(h);
		swf.attr('width', w).attr('height', h);
	}
	function _init(_box, _uid, _c) {
		c = _c;
		box = _box;
		size = {width: c.width, height: c.height};
		var _id = VideoUtil.getVid(c.uid)
			, name = _getName()
			, _w = c.self ? Math.max(300, c.width) : c.width
			, _h = c.self ? Math.max(200, c.height) : c.height;
		box.append($('#user-video').clone().attr('id', _id).attr('title', name).data(self));
		v = $('#' + _id);
		v.dialog({
			classes: {
				'ui-dialog': 'ui-corner-all video user-video'
			}
			, width: _w
			, minWidth: 40
			, minHeight: 50
			, autoOpen: true
			, modal: false
			, resizeStop: function(event, ui) {
				var w = ui.size.width - 2, h = ui.size.height - t.height() - 4;
				_setSize(w, h);
				swf[0].vidResize(w, h);
			}
		}).dialogExtend({
			icons: {
				'collapse': 'ui-icon-minus'
			}
			, closable: VideoUtil.isSharing(c)
			, collapsable: true
			, dblclick: "collapse"
		});
		t = v.parent().find('.ui-dialog-titlebar').attr('title', name);
		if (!VideoUtil.isSharing(c)) {
			v.parent().find('.ui-dialog-titlebar-buttonpane').append($('#video-volume-btn').children().clone());
			var volume = v.parent().find('.dropdown-menu.video.volume');
			vol = v.parent().find('.ui-dialog-titlebar-volume').click(function(e) {
				e.stopImmediatePropagation();
				volume.toggle();
			}).dblclick(function(e) {
				e.stopImmediatePropagation();
			});
			var handle = v.parent().find('.slider .handle');
			v.parent().find('.slider').slider({
				orientation: 'vertical'
				, range: 'min'
				, min: 0
				, max: 100
				, value: 60
				, slide: function(event, ui) {
				}
				, create: function() {
					handle.text($(this).slider("value"));
				}
				, slide: function(event, ui) {
					handle.text(ui.value);
				}
			});
			if (!VideoUtil.hasAudio(c)) {
				vol.hide();
			}
			//TODO add mute, ADD refresh
		}
		vc = v.find('.video');
		vc.width(_w).height(_h);
		//broadcast
		var o = VideoManager.getOptions();
		if (c.self) {
			o.cam = c.cam;
			o.mic = c.mic;
			o.mode = 'broadcast';
			o.uid = _uid;
			o.av = c.activities.join();
		} else {
			o.mode = 'play';
			o.uid = c.uid;
		}
		o.width = c.width;
		o.height = c.height;
		o.sid = c.sid;
		o.uid = c.uid;
		o.broadcastId = c.broadcastId;
		swf = initVideo(vc, _id + '-swf', o);
		swf.attr('width', _w).attr('height', _h);
	}
	function _update(_c) {
		c = _c;
		if (VideoUtil.hasAudio(c)) {
			vol.show();
		} else {
			vol.hide();
		}
		swf[0].update(c);
	}

	self.update = _update;
	self.init = _init;
	self.resetSize = _resetSize;
	self.client = function() { return c; };
	return self;
});
var VideoManager = (function() {
	var self = {}, box, options, share;

	function _init(_options) {
		options = _options;
		VideoSettings.init(self.getOptions());
		box = $('.room.box');
		share = box.find('.icon.shared.ui-button');
	}
	function _update(c) {
		var _id = VideoUtil.getVid(c.uid)
			, av = VideoUtil.hasAudio(c) || VideoUtil.hasVideo(c)
			, v = $('#' + _id);
		if (av && v.length != 1 && !!c.self) {
			Video().init(box, options.uid, c);
		} else if (av && v.length == 1) {
			v.data().update(c);
		} else if (!av && v.length == 1) {
			_closeV(v);
		}
	}
	function _closeV(v) {
		if (v.dialog('instance') !== undefined) {
			v.dialog('destroy');
		}
		v.remove();
	}
	function _play(c) {
		if (VideoUtil.isSharing(c)) {
			_highlight(share
					.attr('title', share.data('user') + ' ' + c.user.firstName + ' ' + c.user.lastName + ' ' + share.data('text'))
					.data('uid', c.uid)
					.show(), 10);
			share.tooltip().off('click').click(function() {
				var v = $('#' + VideoUtil.getVid(c.uid))
				if (v.length != 1) {
					Video().init(box, options.uid, c);
				} else {
					v.dialog('open');
				}
			});
		} else if ('sharing' !== c.type) {
			Video().init(box, options.uid, c);
		}
	}
	function _close(uid) {
		var _id = VideoUtil.getVid(uid), v = $('#' + _id);
		if (v.length == 1) {
			_closeV(v);
		}
		if (uid === share.data('uid')) {
			share.off('click').hide();
		}
	}
	function _highlight(el, count) {
		if (count < 0) {
			return;
		}
		el.addClass('ui-state-highlight', 2000, function() {
			el.removeClass('ui-state-highlight', 2000, function() {
				_highlight(el, --count);
			});
		});
	}

	self.getOptions = function() { return JSON.parse(JSON.stringify(options)); };
	self.init = _init;
	self.update = _update;
	self.play = _play;
	self.close = _close;
	self.resetSize = function(uid) { $('#' + VideoUtil.getVid(uid)).data().resetSize(); };
	return self;
})();
function setRoomSizes() {
	var sb = $(".room.sidebar.left")
		, w = $(window).width() - sb.width() - 8
		, h = $(window).height() - $('#menu').height() - 3
		, p = sb.find('.tabs');
	sb.height(h);
	var hh = h - 5;
	p.height(hh);
	$(".user.list", p).height(hh - $("ul", p).height() - $(".user.header", p).height() - 5);
	var holder = $('.room.holder');
	if (sb.width() > 230) {
		holder.addClass('big').removeClass('small');
	} else {
		holder.removeClass('big').addClass('small');
	}
	if (!!WbArea) {
		WbArea.resize(sb.width() + 5, w, h);
	}
}
function roomReload(event, ui) {
	window.location.reload();
}
function roomClosed(jqEvent, msg) {
	roomUnload();
	$(".room.holder").remove();
	$("#chatPanel").remove();
	var dlg = $('#disconnected-dlg');
	dlg.dialog({
		modal: true
		, close: roomReload
		, buttons: [
			{
				text: dlg.data('reload')
				, icons: {primary: "ui-icon-refresh"}
				, click: function() {
					$(this).dialog("close");
				}
			}
		]
	});
}
function roomLoad() {
	$(".room.sidebar.left").ready(function() {
		setRoomSizes();
	});
	$(window).on('resize.openmeetings', function() {
		setRoomSizes();
	});
	$(".room.sidebar.left").resizable({
		handles: "e"
		, stop: function(event, ui) {
			setRoomSizes();
		}
	});
	Wicket.Event.subscribe("/websocket/closed", roomClosed);
	Wicket.Event.subscribe("/websocket/error", roomClosed);
}
function roomUnload() {
	$(window).off('resize.openmeetings');
	Wicket.Event.unsubscribe("/websocket/closed", roomClosed);
	Wicket.Event.unsubscribe("/websocket/error", roomClosed);
	if (!!WbArea) {
		WbArea.destroy();
	}
	VideoSettings.close();
	$('.ui-dialog.user-video').remove();
}
function startPrivateChat(el) {
	Chat.addTab('chatTab-u' + el.parent().parent().data("userid"), el.parent().parent().find('.user.name').text());
	Chat.open();
	$('#chatMessage .wysiwyg-editor').click();
}
/***** functions required by SIP   ******/
function sipBtnClick() {
	var txt = $('.sip-number');
	txt.val(txt.val() + $(this).data('value'));
}
function sipBtnEraseClick() {
	var txt = $('.sip-number');
	var t = txt.val();
	if (!!t) {
		txt.val(t.substring(0, t.length -1));
	}
}
function sipGetKey(evt) {
	var k = -1;
	if (evt.keyCode > 47 && evt.keyCode < 58) {
		k = evt.keyCode - 48;
	}
	if (evt.keyCode > 95 && evt.keyCode < 106) {
		k = evt.keyCode - 96;
	}
	return k;
}
function sipKeyDown(evt) {
	var k = sipGetKey(evt);
	if (k > 0) {
		$('#sip-dialer-btn-' + k).addClass('ui-state-active');
	}
}
function sipKeyUp(evt) {
	var k = sipGetKey(evt);
	if (k > 0) {
		$('#sip-dialer-btn-' + k).removeClass('ui-state-active');
	}
}

/***** functions required by SWF   ******/
function audioActivity(uid, active) {
	var u = $('#user' + uid + ' .audio-activity.ui-icon');
	if (active) {
		u.addClass("speaking");
	} else {
		u.removeClass("speaking");
	}
}
function typingActivity(uid, active) {
	var u = $('#user' + uid + ' .typing-activity.ui-icon');
	if (active) {
		u.addClass("typing");
	} else {
		u.removeClass("typing");
	}
}
