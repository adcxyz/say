/// preproduce a lot of speech soundfiles,
/// load them into buffers,
// and use them in a little piece:


// where the files go
~saydir = "~/Desktop/nightmare/".standardizePath;
File.mkdir(~saydir);

(
/// render files once only:  say a lot of Class names to soundfiles
var num = 1000; // less than s.options.numBuffers
var t0 = Main.elapsedTime;
var listOfTexts = Object.allSubclasses.reject(_.isMetaClass).keep(num).collect { |cl| cl.name.asString };

listOfTexts.do { |text, i|
	var recstr = "-o %/%.aiff".format(~saydir, text);
	"% %\n".postf(i, text);
	(type: \say, text: text, lang: \en,
		// this tells the event to record to disk
		cmds: recstr,
		// and do only one at a time
		wait: true).play;
};
// check that say is still alive:
"rendering done...".say(lang: \en);

// should be same size:
[listOfTexts.size, (~saydir +/+ "*.aiff").pathMatch.size].postln;
"rendering % short soundfiles with say took: % seconds.\n".postf(
	listOfTexts.size,
	(Main.elapsedTime - t0).round(0.001)).postln;
)


//////// prepare piece here - enough buffers
s.options.numBuffers = 2048;
s.reboot;

(
// preload them all:
// note that reading small buffers is really fast:
~saydir = "~/Desktop/nightmare/".standardizePath;
~paths = (~saydir +/+ "*.aiff").pathMatch; ~paths.size.postln;
~bufs = ~paths.collect { |path| Buffer.read(s, path) };
)

/* test that bufs are there:
~bufs.size; // should be 1000
~bufs.choose.play;
*/

// and play the piece
(
Pdef(\nightmare,
	Pbind(
		\instrument, \saybuf,
		\buf, Pseq(~bufs),
		\dur, Pn(Pgeom(0.5, 0.992, 250), 4) * Pn(Pgeom(1, 0.99, 143), 7),
		\pan, Pwhite(-1.0, 1.0),
		\rate, Pbrown(0.9, 1.1, 0.02) * Pwrand([1, -1], [0.9, 0.1], inf)
	)
).play;
)


///////// for completeness, here is a test for writing files
///////// in parallel with Say.maxPIDs as speed limit:
/// looks good, but was not really faster in my tests.
(
~saydir = "~/Desktop/pipeline/".standardizePath;
File.mkdir(~saydir);

~t0 = Main.elapsedTime;
fork {
	var list = Object.allSubclasses.reject(_.isMetaClass).keep(120);
	list.do { |cl, i|
		var text = cl.name.asString;
		var recstr = "-o %/%.aiff".format(~saydir, text);
		"% %\n".postf(i, text);
		while { Say.pendingPIDs.size >= Say.maxPIDs } {
			0.02.wait;
			// ".".postln;
		};
		(type: \say, text: text, lang: \en, cmds: recstr).play;
	};
	0.1.wait;
	"rendering done.".say;
	// should be same size:
	[list.size, (~saydir +/+ "*.aiff").pathMatch.size].postln;
	(Main.elapsedTime - ~t0).postln;
};
)