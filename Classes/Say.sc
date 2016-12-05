Say {
	classvar <voices;

	*initClass {
		this.getVoices;
		this.addSayEvent;
	}

	*getVoices {
		var rawLines = unixCmdGetStdOut("say -v ?").split($\n).reject(_.isEmpty);
		voices = rawLines.collect { |line|
			var pair = line.split($#);
			var firstBlankIndex = pair[0].indexOf($ );
			var name = pair[0].keep(firstBlankIndex);
			var langSymbol = pair[0].drop(firstBlankIndex).reject(_ == $ );
			(name: name, langSymbol: langSymbol, exampleText: pair[1]);
		};
	}

	*isValidVoice { |name| ^Say.voices.any { |dict| dict.name == name } }

	*addSayEvent {
		Event.addEventType(\say, {
			var str = "say";
			if (this.isValidVoice(~voice)) { str = str + "-v" + ~voice };
			// support rate flag - more flags could be supported here as well
			~rate !? { str = str + "-r" + ~rate };
			~cmds !? { str = str + ~cmds };
			str = str + quote(~text ? "");
			str.postcs;
			if (~wait == true) {
				unixCmdGetStdOut(str);
			} {
				unixCmd(str);
			};
		});
	}
}

+ String {
	say { |index, wait = false, cmds|
		var event = (\type: \say, wait: wait, text: this, cmds: cmds);
		var voice;
		index !? {
			voice = Say.voices[index];
			voice = voice !? { voice.name };
			event.put(\voice, voice);
		};
		event.postcs.play;
	}

	// backwards compat
	speak {  |index, wait = false, dict|
		this.say(index, wait, dict);
	}
}
