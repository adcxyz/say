Say {
	classvar <voices, <voiceNames;
	classvar <allVoices, <allVoiceNames;
	classvar <fxVoices, <fxVoiceNames;
	classvar <allLangs, <allLangNames;
	classvar <defaultVoiceName;

	classvar <pendingPIDs, <>maxPIDs = 10;

	classvar <clipRate = true;
	classvar <>minRate = 90, <>maxRate = 720;

	*killAll { thisProcess.platform.killAll("say") }

	*initClass {
		Platform.case(\osx,
			{
				fxVoiceNames = [
					"Agnes", "Albert", "Bad", "Bahh", "Bells", "Boing",
					"Bubbles", "Bruce", "Cellos", "Deranged", "Fred",
					"Good", "Hysterical", "Junior", "Pipe",
					"Princess", "Ralph", "Trinoids",
					"Vicki", "Victoria", "Whisper", "Zarvox",
					"NONEXISTENTVOICE_shouldBeFiltered"
				];

				Say.getVoices;
				Say.getLangs;
				Say.addSayEvent;
				Say.getDefaultVoice;
				pendingPIDs = List[];
			}, {
				"The Quark 'say' is only available on osx.".postln
			}
		);
	}

	*sayVoiceName { |name|
		var voiceDict = Say.at(name);
		if (voiceDict.notNil) {
			forkIfNeeded {
				(name + "-" + voiceDict.langName).say(name, wait: true);
			}
		}
	}

	*sayVoiceNames { |lang|
		var dicts = Say.voicesByLang(lang);
		if (dicts.isEmpty) { dicts = Say.voices };
		forkIfNeeded {
			dicts.do { |dict| Say.sayVoiceName(dict.name) }
		}
	}

	*getDefaultVoice {
		^defaultVoiceName =
		unixCmdGetStdOut("defaults read com.apple.speech.voice.prefs SelectedVoiceName")
		.select(_.isAlpha);
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

	*removeVoice { |name|
		var index = voices.detectIndex { |dict| dict.name == name };
		index !? { voices.removeAt(index) };
		voiceNames = voices.collect(_.name);
	}
	*addVoice { |name|
		var voicedict;
		if (voices.detect { |dict| dict.name == name }.notNil) {
			"voice % already present.\n".postf(name.cs);
			^this
		};

		voicedict = allVoices.detect { |dict| dict.name == name };
		voicedict !? {  voices = voices.add(voicedict) };
		voiceNames = voices.collect(_.name);
	}

	*filterVoices { |names|
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

	*fillVoice { |event|
		^event.put(\voice, Say.findVoice(event[\voice], event[\lang]))
	}

	*findVoice { |voiceOrIndex, lang, default = true|
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

		if (voice.isNil and: default) { voice = defaultVoiceName };
		^voice
	}

	*addSayEvent {

		Event.addEventType(\say, {
			var str = "say", cond, pid, thisEvent;

			thisEvent = currentEnvironment;

			if (this.isValidVoice(~voice).not) {
				~voice = Say.findVoice(~voice ? ~voiceOrIndex, ~lang);
			};
			if (~voice.notNil) { str = str + "-v" + ~voice };

			// support rate flag - more flags could be supported here as well
			~wordrate !? {
				if (clipRate) { ~wordrate = ~wordrate.clip(minRate, maxRate) };
				str = str + "-r" + ~wordrate
			};
			// write to file could be here:
			~cmds !? { str = str + ~cmds };
			str = str + quote(~text ? "");

			if (pendingPIDs.size > maxPIDs) {
				// emergency break to avoid choking the say utility:
				"*** SAY: too many events! not saying: %\n"
				.postf(currentEnvironment.text.cs);
			} {
				if (~wait == true) {
					if (thisThread.isKindOf(Routine)) {
						cond = Condition.new;
						pid = unixCmd(str, {
							cond.unhang;
							thisEvent[\doneFunc].value;
						});
						pendingPIDs.add(pid);
						cond.hang;
						pendingPIDs.remove(pid);
					} {
						currentEnvironment = thisEvent;
						// ugly wait by blocking ...
						unixCmdGetStdOut(str);
						thisEvent[\doneFunc].value;
					};
				} {
					pid = unixCmd(str, {
						thisEvent[\doneFunc].value;
						pendingPIDs.remove(pid);
					});
					pendingPIDs.add(pid);
				}
			}
		});
	}
		// utility to write a single say event as speech soundfile
	*write { |sayEvent, filename, filedir, action|
		var shortText, path;

		Say.fillVoice(sayEvent);

		// borrow dir from SayBuf if needed
		filedir = filedir ? SayBuf.dir;

		filename ?? {
			shortText = sayEvent.text.asString.keep(20).collect { |char| if (char.isAlphaNum, char, $_) };
			filename = "temp_%_%.aif".format(sayEvent.voice, shortText)
		};
		path = (filedir +/+ filename).postcs;

		sayEvent.putAll((
			type: \say,
			path: path,
			filename: filename,
			cmds: ~cmds ? "" + "-o" + quote(path),
			doneFunc: action)
		);
		// defer for file write, return immediately
		defer { sayEvent.play };
		^sayEvent
	}

}
