+ Object {
	say { |voiceOrIndex, lang, wait = false, cmds|
		this.asString.say(voiceOrIndex, lang, wait, cmds);
	}
}

+ String {

	say { |voiceOrIndex, lang, wait = false, cmds|
		var event = (\type: \say, wait: wait, text: this, cmds: cmds);
		var voice = Say.findVoice(voiceOrIndex, lang);
		voice !? { event.put(\voice, voice) };
		^event.play;
	}

	// backwards compat
	speak {  |voiceOrIndex, wait = false, dict|
		this.say(voiceOrIndex, wait, dict);
	}
}
