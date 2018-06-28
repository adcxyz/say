+ Object {
	say { |voiceOrIndex, lang, wait = false, cmds|
		this.asString.say(voiceOrIndex, lang, wait, cmds);
	}
}

+ String {

	say { |voiceOrIndex, lang, wait = false, cmds|
		var event = (\type: \say, voice: voiceOrIndex, lang: lang, wait: wait, text: this, cmds: cmds);
		^event.play;
	}

	// backwards compat
	speak {  |voiceOrIndex, wait = false, lang, dict|
		this.say(voiceOrIndex, lang, wait, dict);
	}
}
