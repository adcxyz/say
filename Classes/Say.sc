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

	*isValidVoice { |name|
		name = name.asString;
		^Say.voices.any { |dict| dict.name == name }
	}

	*addSayEvent {
		Event.addEventType(\say, {
			var str = "say", cond;
			if (this.isValidVoice(~voice)) { str = str + "-v" + ~voice };
			// support rate flag - more flags could be supported here as well
			~rate !? { str = str + "-r" + ~rate };
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

