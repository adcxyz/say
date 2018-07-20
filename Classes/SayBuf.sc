SayBuf {
	classvar <>dir, <bufs, <bufring, <>usesRoundRobin = true, <rrIndex = 0;
	classvar <verbose = false;

	*initClass {
		dir = Platform.userAppSupportDir +/+ "saybuf";
		//	dir = "/tmp/saybuf";    // maybe faster on some systems?
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

		ServerBoot.add({ |server|
			this.prepRRBufs(server, 100);
		}, Server.default);
	}

	*prepRRBufs { |server, numBufs = 100|
		bufring = { Buffer(server) }.dup(numBufs);
	}

	*freeBufs {
		// leaves bufring untouched
		bufs.do(this.freeBuf(_));
	}

	*freeBuf { |buf, dt|
		// by default, remove after buffer duration + some safety time
		dt = dt ?? { buf.duration * 1.1 + (buf.server.latency ? 0) + 0.1 };
		defer ({
			if (verbose) {
				"freeing buf: % - %\n".postf(buf.bufnum, buf.path.basename);
			};
			File.delete(buf.path);
			bufs.removeAt(buf.bufnum);
			buf.free;
		}, dt)
	}

	*clearDir {
		(SayBuf.dir +/+ "temp*").pathMatch.do { |p| File.delete(p) };
	}

	*prepare { |sayEvent, server, action, postPrepTime = false|

		var bufID, shortText, filename, path, rrBuffer;
		var startTime = Main.elapsedTime;

		server = server ? Server.default;

		if (server.serverRunning.not) {
			"%: cannot prepare Event when server % is not ready.\n"
			.postf(this, server);
			^this
		};

		Say.fillVoice(sayEvent);


		if (usesRoundRobin) {
			rrBuffer = bufring.wrapAt(rrIndex);
			bufID = rrBuffer.bufnum;
			rrIndex = rrIndex + 1 % bufring.size;
		} {
			bufID = server.bufferAllocator.alloc;
		};

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
			var buf;
			var prepTimePost = { |bufff|
				var prepTime = (Main.elapsedTime - startTime).round(0.001);
				var prepRatio = (bufff.duration / prepTime).round(0.01);
				if (verbose) {
					"SayBuf: buffer ready after % sec - % x realtime.\n".postf(prepTime, prepRatio)
				}
			};

			if (verbose) {
				"SayBuf: file written after % secs.\n".postf((Main.elapsedTime - startTime).round(0.001));
			};

			if (usesRoundRobin) {
				buf = rrBuffer;
				buf.doOnInfo_({ |buf|
					if (postPrepTime) { prepTimePost.(buf) };
					(action ? sayEvent[\bufAction]).value(buf, sayEvent)
				});
				buf.allocRead(path, 0, -1, ["/b_query", buf.bufnum]);
			} {
				buf = Buffer.read(server, path, action: { |buf|
					if (postPrepTime) { prepTimePost.(buf) };
					(action ? sayEvent[\bufAction]).value(buf, sayEvent)
				}, bufnum: bufID);
				bufs.put(bufID, buf);
			};

			sayEvent.put(\buf, buf);
		});

		// prepare by writing file here,
		// will call sayEvent doneFunc when ready,
		// which loads buffer,
		// which calls sayEvent bufAction when ready
		defer { sayEvent.play };

		^sayEvent
	}
}
