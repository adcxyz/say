
+ String {
	say { |voiceOrIndex, wait = false, cmds|
		var event = (\type: \say, wait: wait, text: this, cmds: cmds);
		var voice;

		voiceOrIndex !? {
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
		event.play;
	}

	// backwards compat
	speak {  |voiceOrIndex, wait = false, dict|
		this.say(voiceOrIndex, wait, dict);
	}
}
