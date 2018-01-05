SayBuf {
	classvar <dir, <bufs;

	*initClass {
		dir = Platform.userAppSupportDir +/+ "saybuf";
		if (dir.pathMatch.isEmpty) {
			"SayBuf: making SayBuf.dir.".postln;
			File.mkdir(dir)
		};

		Class.initClassTree(SynthDef);
		Class.initClassTree(SynthDescLib);
		SynthDef(\saybuf, { |out, buf, rate = 1, amp = 0.1, pan|
			rate = BufRateScale.ir(buf) * rate;
			Out.ar(out,
				Pan2.ar(
					PlayBuf.ar(1, buf, rate, doneAction: 2),
					pan,
					amp
				)
			)
		}).add;
		this.clearDir;
	}

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
		filename = "temp_%_%.aif".format(bufID, shortText);
		path = (dir +/+ filename);

		sayEvent.putAll((
			type: \say,
			bufID: bufID,
			path: path,
			filename: filename,
			cmds: ~cmds ? "" + "-o" + quote(path)
		));

		sayEvent.put(\doneFunc, {
			sayEvent.put(\buf,
				Buffer.read(server, path, action: { |buf|
					sayEvent[\bufAction].value(buf, sayEvent)
				}, bufnum: bufID)
			)
		});

		// prepare by writing file here!
		defer { sayEvent.play };

		^sayEvent
	}
}
