SayBuf {
	classvar <dir, <bufs;

	*initClass {
		dir = Platform.userAppSupportDir +/+ "saybuf";
		if (dir.pathMatch.isEmpty) {
			"SayBuf: making SayBuf.dir.".postln;
			File.mkdir(dir)
		};
		bufs = ();
		this.clearDir;

		Class.initClassTree(SynthDef);
		Class.initClassTree(SynthDescLib);

		SynthDef(\saybuf, { |out, buf, rate = 1, amp = 0.1, pan, pos|
			rate = BufRateScale.ir(buf) * rate;
			// allow backwards playback: flip offset if negative
			pos = (rate.sign * pos * BufSampleRate.ir(buf))
			// add tiny offset so neg rate does not reach doneAction immediately
			+ (rate.sign * 0.01)
			// ... and wrap to legal range
			.wrap(0, BufFrames.ir(buf) - 1);
			Out.ar(out,
				Pan2.ar(
					PlayBuf.ar(1, buf, rate, startPos: pos, doneAction: 2),
					pan,
					amp
				)
			)
		}).add;

	}

	*freeBufs { bufs.do(this.freeBuf(_)) }

	*freeBuf { |buf|
		File.delete(buf.path);
		buf.free;
	}

	*clearDir {
		(SayBuf.dir +/+ "temp*").pathMatch.do { |p| File.delete(p) };
	}

	*prepare { |sayEvent, server, action|

		var bufID, shortText, filename, path, buffer;
		server = server ? Server.default;

		if (server.serverRunning.not) {
			"%: cannot prepare Event when server % is not ready.\n"
			.postf(this, server);
			^this
		};

		bufID = server.nextNodeID;
		shortText = sayEvent.text.asString.keep(20).collect { |char|
			if (char.isAlphaNum, char, $_)
		};
		filename = "temp_%_%_%.aif".format(bufID, sayEvent.voice, shortText);
		path = (dir +/+ filename);

		sayEvent.putAll((
			type: \say,
			bufID: bufID,
			path: path,
			filename: filename,
			cmds: ~cmds ? "" + "-o" + quote(path)
		));

		sayEvent.put(\doneFunc, {
			var buf = Buffer.read(server, path, action: { |buf|
				(action ? sayEvent[\bufAction]).value(buf, sayEvent)
			}, bufnum: bufID);
			sayEvent.put(\buf, buf);
			bufs.put(bufID, buf);
		});

		// prepare by writing file here!
		defer { sayEvent.play };

		^sayEvent
	}
}
