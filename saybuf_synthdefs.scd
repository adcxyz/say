/***

This file shows possibilities of is a collection of synthdefs that manipulate
soundfiles recorded with Say/SayBuf.



*** SELECT ALL AND ENTER TO LOAD ALL SYNTHDEFS! ****


--- Manipulating speech synthesis params with Say:
1. rate manipulation within say soundfile preparation

--- SynthDefs for SayBuf which add buffer-level parameters
2. pitch inflections within buffer playback
3. ring modulation for spectral coloring/shift
4. time-strech/compression with granular transforms

---
1. For speech tempo manipulation within say, use the 'wordrate' parameter in soundfile preparation:
(type: \say, text: "tempo 1 2 3", wordrate: 90).play; // this is the minimum rate
(type: \say, text: "tempo 1 2 3", wordrate: 180).play; // this is default rate
(type: \say, text: "tempo 1 2 3", wordrate: 360).play; //
(type: \say, text: "tempo 1 2 3", wordrate: 720).play; // fastest rate

***/

/*** ///// SYNTHDEFS FOR playing with Say-Produced  BUFFERS


s.boot;
x = SayBuf.prepare((type: \say, text: "pitch inflections with say..."));
b = x.buf;

////// NOTE: depending on the voice, Say-Produced soundfiles begin and end with silence.
////// if timing is critical, these could be accounted for as offsets in the synthdefs.

x.buf.plot;
x.buf.loadToFloatArray(0, -1, { |arr| a = arr });

a.detectIndex { |el, i| el.abs > 0.01 }.postln / x.buf.sampleRate;  // 0.11 sec before start

a.reverse.detectIndex { |el, i| el.abs > 0.01 }.postln / x.buf.sampleRate;
a.reverse.detectIndex { |el, i| el.abs > 0.0001 }.postln / x.buf.sampleRate; // 0.2 seconds after end




///// 2. simple pitch inflections within buffer playback:


- playback rate -> like tape speed
- vibrato has rate and depth
- tilt: raises start and lowers end playback rate (or vice versa)

// straight:
(instrument: \saybuf, buf: b).play;

// rate - also works in \saybuf:
(instrument: \saybufPch, buf: b, rate: 1).play;
(instrument: \saybufPch, buf: b, rate: 1.2).play;
(instrument: \saybufPch, buf: b, rate: 1.5).play;
(instrument: \saybufPch, buf: b, rate: 0.6666).play;
(instrument: \saybufPch, buf: b, rate: -0.8).play;  // reverse!

// vibrato
(instrument: \saybufPch, buf: b, vib: 0.0).play;
(instrument: \saybufPch, buf: b, vib: 0.1).play;   // depth
(instrument: \saybufPch, buf: b, vib: 0.2).play;   // more depth
(instrument: \saybufPch, buf: b, vib: 0.2, vibRate: 2).play; // vibrato rate
(instrument: \saybufPch, buf: b, vib: 0.2, vibRate: 4).play;
(instrument: \saybufPch, buf: b, vib: 0.2, vibRate: 8).play;
(instrument: \saybufPch, buf: b, vib: 0.2, vibRate: 16).play;

// tilt
(instrument: \saybufPch, buf: b, tilt: -0.6).play; // start high and fall
(instrument: \saybufPch, buf: b, tilt: -0.3).play;
(instrument: \saybufPch, buf: b, tilt: -0.0).play; // flat
(instrument: \saybufPch, buf: b, tilt:  0.3).play; // rising
(instrument: \saybufPch, buf: b, tilt:  0.6).play; // rising more

***/

(
SynthDef(\saybufPch, { |out, amp = 0.1, pan, pos,
	buf, endPause = 0.2, rate = 1,
	tilt = 0, vib = 0.0, vibRate = 8|

	// subtract standard pause at buffer end
	var dur2 = BufDur.ir(buf) - endPause;
	var ratesig = Line.kr(rate - tilt, rate + tilt, dur2 * rate);
	var vibsig = SinOsc.ar(vibRate, 0, vib, 1);
	var startpos = (ratesig.sign * pos * BufSampleRate.ir(buf))
	// add tiny offset so neg rate does not reach doneAction immediately
	+ (rate.sign * 0.01)
	// ... and wrap to legal range
	.wrap(0, BufFrames.ir(buf) - 1);

	var snd = PlayBuf.ar(1, buf,
		BufRateScale.ir(buf) * ratesig * vibsig,
		startPos: startpos,
		doneAction: 2
	);

	Out.ar(out,
		Pan2.ar(snd, pan, amp)
	)
}).add;
);

/***
3. ringmodulation - fixed frequency SinOsc shifts spetrum

(instrument: \saybufPchRng ).play;

(instrument: \saybufPchRng, buf: b, amp: 0.5, ringFreq: 2000, ringMod: 0.2).play;
(instrument: \saybufPchRng, buf: b, amp: 0.5, ringFreq: 2000, ringMod: 0.5).play;
(instrument: \saybufPchRng, buf: b, amp: 0.5, ringFreq: 2000, ringMod: 0.8).play;
(instrument: \saybufPchRng, buf: b, amp: 0.5, ringFreq: 2000, ringMod: 1).play;

(instrument: \saybufPchRng, buf: b, amp: 0.5, ringFreq: 500, ringMod: 0.2).play;
(instrument: \saybufPchRng, buf: b, amp: 0.5, ringFreq: 500, ringMod: 0.5).play;
(instrument: \saybufPchRng, buf: b, amp: 0.5, ringFreq: 500, ringMod: 0.8).play;
(instrument: \saybufPchRng, buf: b, amp: 0.5, ringFreq: 500, ringMod: 1).play;

(instrument: \saybufPchRng, buf: b, amp: 0.5, ringFreq: 50, ringMod: 0.2).play;
(instrument: \saybufPchRng, buf: b, amp: 0.5, ringFreq: 50, ringMod: 0.5).play;
(instrument: \saybufPchRng, buf: b, amp: 0.5, ringFreq: 50, ringMod: 0.8).play;
(instrument: \saybufPchRng, buf: b, amp: 0.5, ringFreq: 50, ringMod: 1).play;

***/

(
SynthDef(\saybufPchRng, { |out, amp = 0.1, pan, pos,
	buf, endPause = 0.2, rate = 1,
	tilt = 0, vib = 0.0, vibRate = 8,
	ringFreq = 200, ringMod = 0.5|

	// subtract standard pause at buffer end
	var dur2 = BufDur.ir(buf) - endPause;
	var ratesig = Line.kr(rate - tilt, rate + tilt, dur2 * rate);
	var vibsig = SinOsc.ar(vibRate, 0, vib, 1);
	var startpos = (ratesig.sign * pos * BufSampleRate.ir(buf))
	// add tiny offset so neg rate does not reach doneAction immediately
	+ (rate.sign * 0.01)
	// ... and wrap to legal range
	.wrap(0, BufFrames.ir(buf) - 1);

	var ringsig = SinOsc.ar(ringFreq, 0, ringMod, 1-ringMod);

	var snd = PlayBuf.ar(1, buf,
		BufRateScale.ir(buf) * ratesig * vibsig,
		startPos: startpos,
		doneAction: 2);

	Out.ar(out,
		Pan2.ar(snd * ringsig, pan, amp)
	)
}).add;
);


///////// with granular treatment
/***
y = SayBuf.prepare((type: \say, text: "granular time-processing with saybuf ..."));
g = y.buf;
g.play;

// speed with which to move thru file
(instrument: \saybufGrn, buf: g, amp: 0.5, speed: 1).play;
(instrument: \saybufGrn, buf: g, amp: 0.5, speed: 2).play;
(instrument: \saybufGrn, buf: g, amp: 0.5, speed: 3).play;
(instrument: \saybufGrn, buf: g, amp: 0.5, speed: 4).play;
(instrument: \saybufGrn, buf: g, amp: 0.5, speed: 0.5).play;
(instrument: \saybufGrn, buf: g, amp: 0.5, speed: 0.2).play;

// rate and tilt also used:
(instrument: \saybufGrn, buf: g, amp: 0.5, speed: 0.5, tilt: 0.5).play;
(instrument: \saybufGrn, buf: g, amp: 0.5, speed: 0.5, tilt: -0.5).play;
(instrument: \saybufGrn, buf: g, rate: 3/2, speed: 0.5, amp: 0.5).play;
(instrument: \saybufGrn, buf: g, rate: 2/3, speed: 0.5, amp: 0.5).play;

// grDur sets grain duration (with fixed overlap of 2)
(instrument: \saybufGrn, buf: g, amp: 0.5, speed: 0.5, grDur: 0.08).play;
(instrument: \saybufGrn, buf: g, amp: 0.5, speed: 0.5, grDur: 0.06).play;
(instrument: \saybufGrn, buf: g, amp: 0.5, speed: 0.5, grDur: 0.04).play;
(instrument: \saybufGrn, buf: g, amp: 0.5, speed: 0.5, grDur: 0.02).play;
***/

(
SynthDef(\saybufGrn, { |out, amp = 0.1, pan, pos,
	buf, endPause = 0.2, rate = 1,
	tilt = 0, speed = 1, grDur = 0.1|

	var dur2 = BufDur.ir(buf) - endPause / speed;
	var ratesig = Line.kr(rate - tilt, rate + tilt, dur2 * rate);
	var snd, posSig;

	var startpos = (ratesig.sign * pos * BufSampleRate.ir(buf))
	// add tiny offset so neg rate does not reach doneAction immediately
	+ (rate.sign * 0.01)
	// ... and wrap to legal range
	.wrap(0, BufFrames.ir(buf) - 1);

	posSig = Line.kr(startpos / dur2, 1 + pos - 0.1,  dur2, doneAction: 2);

	snd = GrainBuf.ar(1,
		Impulse.ar(2 / grDur),
		grDur,
		buf,
		ratesig,
		posSig,
		4
	);

	Out.ar(out,
		Pan2.ar(snd, pan, amp)
	)
}).add;
);

///////// granular + bandpass filtering:
/***
y = SayBuf.prepare((type: \say, text: "granular time-processing with saybuf ..."));
g = y.buf;
g.play;


(instrument: \saybufGrnBpf, buf: g, amp: 0.5, filtFreq: 700).play;
(instrument: \saybufGrnBpf, buf: g, amp: 0.5, filtFreq: 700, filtBW: 0.5).play;
(instrument: \saybufGrnBpf, buf: g, amp: 0.5, filtFreq: 700, filtBW: 0.25).play;

// maybe distance could be modeled with this?
(instrument: \saybufGrnBpf, buf: g, amp: 0.5, filtFreq: 3000).play;
(instrument: \saybufGrnBpf, buf: g, amp: 0.5, filtFreq: 3000, filtBW: 0.5).play;
(instrument: \saybufGrnBpf, buf: g, amp: 0.5, filtFreq: 3000, filtBW: 0.25).play;

// too low does not work well
(instrument: \saybufGrnBpf, buf: g, amp: 0.5, filtFreq: 200).play;
(instrument: \saybufGrnBpf, buf: g, amp: 0.5, filtFreq: 200, filtBW: 0.5).play;
(instrument: \saybufGrnBpf, buf: g, amp: 0.5, filtFreq: 200, filtBW: 0.25).play;

***/

(
SynthDef(\saybufGrnBpf, { |out, amp = 0.1, pan, pos,
	buf, endPause = 0.2, rate = 1,
	tilt = 0, speed = 1, grDur = 0.1,
	filtFreq = 700, filtBW = 2|

	var dur2 = BufDur.ir(buf) - endPause / speed;
	var ratesig = Line.kr(rate - tilt, rate + tilt, dur2 * rate);
	var snd, posSig;

	var startpos = (ratesig.sign * pos * BufSampleRate.ir(buf))
	// add tiny offset so neg rate does not reach doneAction immediately
	+ (rate.sign * 0.01)
	// ... and wrap to legal range
	.wrap(0, BufFrames.ir(buf) - 1);

	posSig = Line.kr(startpos / dur2, 1 + pos - 0.1,  dur2, doneAction: 2);

	snd = GrainBuf.ar(1,
		Impulse.ar(2 / grDur),
		grDur,
		buf,
		ratesig,
		posSig,
		4
	);

	snd = BPF.ar(snd, filtFreq, filtBW);

	Out.ar(out,
		Pan2.ar(snd, pan, amp)
	)
}).add;
);

/**********

// emphasis by envelope
- where to put peak in time from 0 (start) to 1 (end),
- and how much louder/softer in dB :
Env([0, 6, 0], [0.3, 0.7]).plot;

(instrument: \saybufGrnBpfEnv, buf: b, envDB: 12, envTime: 0).play; //  at start, dimin
(instrument: \saybufGrnBpfEnv, buf: b, envDB: 12, envTime: 1).play; // at end, cresc
(instrument: \saybufGrnBpfEnv, buf: b, envDB: 12, envTime: 0.5).play; // peak in center
(instrument: \saybufGrnBpfEnv, buf: b, envDB: -12, amp: 0.5).play; // env dip in center

***********/



(
SynthDef(\saybufGrnBpfEnv, { |out, amp = 0.1, pan, pos,
	buf, endPause = 0.2, rate = 1,
	tilt = 0, speed = 1, grDur = 0.1,
	filtFreq = 700, filtBW = 2,
	envDB = 0, envTime = 0.5 |

	var dur2 = BufDur.ir(buf) - endPause / speed;
	var ratesig = Line.kr(rate - tilt, rate + tilt, dur2 * rate);

	var startpos = (ratesig.sign * pos * BufSampleRate.ir(buf))
	// add tiny offset so neg rate does not reach doneAction immediately
	+ (rate.sign * 0.01)
	// ... and wrap to legal range
	.wrap(0, BufFrames.ir(buf) - 1);

	var posSig = Line.kr(startpos / dur2, 1 + pos - 0.1,  dur2, doneAction: 2);

	var snd = GrainBuf.ar(1,
		Impulse.ar(2 / grDur),
		grDur,
		buf,
		ratesig,
		posSig,
		4
	);

	var env = Env([0, envDB, 0], [envTime, 1-envTime] * dur2).kr.dbamp;

	snd = BPF.ar(snd, filtFreq, filtBW) * env;

	Out.ar(out,
		Pan2.ar(snd, pan, amp)
	)
}).add;
);

