Say {
	classvar <voices, <voiceNames;
	classvar <allVoices, <allVoiceNames;
	classvar <fxVoices, <fxVoiceNames;
	classvar <allLangs, <allLangNames;

	*initClass {
		Platform.case(\osx,
			{
				Say.getVoices;
				Say.getLangs;
				Say.addSayEvent;
			}, {
				"The Quark 'say' is currently only available on osx.".postln
			}
		);
	}

	*getVoices {
		var rawLines = unixCmdGetStdOut("say -v ?").split($\n).reject(_.isEmpty);
		allVoices = rawLines.collect { |line|
			var pair = line.split($#);
			var firstBlankIndex = pair[0].indexOf($ );
			var name = pair[0].keep(firstBlankIndex);
			var langName = pair[0].drop(firstBlankIndex).reject(_ == $ );
			var lang = langName.keep(2).asSymbol;
			(name: name, langName: langName, lang: lang, exampleText: pair[1]);
		};
		allVoiceNames = allVoices.collect(_.name);
		this.filterVoices;
	}

	*getLangs {
		allLangs = SortedList[];
		allLangNames = SortedList[];
		allVoices.do { |dict|
			if (allLangNames.includesEqual(dict.langName).not) {
				allLangNames.add(dict.langName)
			};
			if (allLangs.includes(dict.lang).not) {
				allLangs.add(dict.lang)
			}
		}
	}

	*filterVoices {
		fxVoiceNames = [
			"Agnes", "Albert", "Bad", "Bahh", "Bells", "Boing",
			"Bubbles", "Bruce", "Cellos", "Deranged", "Fred",
			"Good", "Hysterical", "Junior", "Pipe",
			"Princess", "Ralph", "Trinoids", "Whisper", "Zarvox",
			"NONEXISTENTVOICE_shouldBeFiltered"
		];
		fxVoiceNames = fxVoiceNames.select { |name| this.isValidVoice(name) };

		fxVoices = fxVoiceNames.collect { |name| this.at(name) }.select(_.notNil);
		voices = allVoices.difference(fxVoices);
		voiceNames = voices.collect(_.name);
	}

	*isValidVoice { |name|
		^allVoiceNames.includesEqual(name.asString);
	}

	*at { |name|
		^allVoices.detect { |voice| voice.name == name.asString }
	}

	*isValidLang { |langName|
		^allVoices.any { |dict|
			dict.langName.asString.beginsWith(langName.asString)
		}
	}

	*voicesByLang { |langName, argVoices|
		^(argVoices ? voices ? allVoices).select { |dict|
			dict.langName.asString.beginsWith(langName.asString)
		}
	}

	*findVoice { |voiceOrIndex, lang|
		var voiceDict, voice;

		case
		{ voiceOrIndex.isNil } {
			voiceDict = Say.voicesByLang(lang.asSymbol).choose;
			if (voiceDict.notNil) {
				voice = voiceDict.name
			}
		}
		{ voiceOrIndex.isKindOf(Symbol) or: { voiceOrIndex.isKindOf(String) } } {
			voiceOrIndex = voiceOrIndex.asString;
			if (Say.isValidVoice(voiceOrIndex)) {
				voice = voiceOrIndex
			}
		}
		{ voiceOrIndex.isKindOf(SimpleNumber) } {
			voice = Say.voices[voiceOrIndex.asInteger];
			voice = voice !? { voice.name }
		};

		^voice
	}

	*addSayEvent {

		Event.addEventType(\say, {
			var str = "say", cond;

			if (this.isValidVoice(~voice).not) {
				~voice = Say.findVoice(~voice ? ~voiceOrIndex, ~lang);
			};
			if (~voice.notNil) { str = str + "-v" + ~voice };

			// support rate flag - more flags could be supported here as well
			~rate !? { str = str + "-r" + ~rate };
			// write to file could be here:
			~cmds !? { str = str + ~cmds };
			str = str + quote(~text ? "");
			// str.postcs;
			if (~wait == true) {
				if (thisThread.isKindOf(Routine)) {
					cond = Condition.new;
					unixCmd(str, {
						cond.unhang;
						~doneFunc.value;
					});
					cond.hang;
				} {
					// ugly wait by blocking ...
					unixCmdGetStdOut(str);
					~doneFunc.value;
				};
			} {
				unixCmd(str, ~doneFunc);
			}
		});
	}
}
