+ Object {
	say { |voiceOrIndex, lang, wait = false, cmds|
		this.asString.say(voiceOrIndex, lang, wait, cmds);
	}
}

+ String {

	say { |voiceOrIndex, lang, wait = false, cmds|
		var event = (\type: \say, wait: wait, text: this, cmds: cmds);
		var voice;

		if (voiceOrIndex.isNil) {
			if (lang.notNil) {
				voice = Say.voicesByLang(lang.asSymbol).choose;
				voice !? { event.put(\voice, voice.name) };
			}
		} {
			if (voiceOrIndex.isKindOf(Symbol)
			or: voiceOrIndex.isKindOf(String)) {
				voice = voiceOrIndex.asString;
			} {
				if (voiceOrIndex.isKindOf(SimpleNumber)) {
					voice = Say.voices[voiceOrIndex.asInteger];									voice = Say.voices[voiceOrIndex];
					voice = voice !? { voice.name };
				}
			};
			event.put(\voice, voice);
		};

		event.postln.play;
	}

	// backwards compat
	speak {  |voiceOrIndex, wait = false, dict|
		this.say(voiceOrIndex, wait, dict);
	}
}
