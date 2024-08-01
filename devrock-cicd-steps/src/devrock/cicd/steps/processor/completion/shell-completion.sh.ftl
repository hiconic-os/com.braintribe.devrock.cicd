# Shell completion script for ${cliCommand}

complete -F __${cliCommand}Completion ${cliCommand}
complete -F __${cliCommand}Completion ${cliCommand}.sh


__${cliCommand}Completion() {
<#noparse>	currentWord="${COMP_WORDS[COMP_CWORD]}";
	nWords=${#COMP_WORDS[@]};

	# ZSH for example does not add the empty string to the array for some reason
	if [ $COMP_CWORD -eq $nWords ];then
		COMP_WORDS+=( '' );
		nWords=${#COMP_WORDS[@]};
	fi

	if __startsWithColon $currentWord; then return; fi

	__resolveCommandNameAndPos;

	# if we have typed more words after specifying the command, we want to complete a parameter for the command
	if [ "$commandOffset" -gt "0" ]; then
		__suggestParameter;
	else
	    __suggestCommand;
	fi
}

__startsWithColon() {
	if [[ $1 == :* ]]; then true; else false; fi
}

__resolveCommandNameAndPos() {
	local i;
	for (( i=$nWords-2 ; i>=0 ; i-- )) ; do
		if [ "${COMP_WORDS[i]}" == ":" ]; then
			commandName="${COMP_WORDS[i+1]}";
			commandOffset=$(( $nWords-i-2 ))
			return;
		fi
	done

	commandName="${COMP_WORDS[1]}";
	commandOffset=$(( $nWords-2 ));
}

__suggestCommand() {
</#noparse>
	__suggest "${commandsList}";
<#noparse>
}

__suggestParameter() {
	if [ "$commandName" == "help" ]; then
		if [ "$commandOffset" == "1" ]; then
			__suggestHelp;
		fi
		return;
	fi

	__resolveParameterNameAndOffset;
	__resolveParameterTypeIfRelevant;
	__resolveNextStep;

	if [ "$nextStep" == "value" ]; then
		__suggestParameterValue;
	elif [ "$nextStep" == "parameter" ]; then
		__suggestParameterName;
	fi
}

__resolveParameterNameAndOffset() {
	parameterName="";
	parameterOffset="";

	# this resolves last valid parameter name for the purpose of suggesting the value, so it ignores current word
	# it is also used to check if previous word is a boolean or a collection and thus we can write another parameter right away
	local i;
	for (( i=$nWords-2 ; i>=0 ; i-- )) ; do
		if [ "${COMP_WORDS[i]}" == ":" ]; then return; fi

		if __startsWithDash "${COMP_WORDS[i]}"; then
			parameterName="${COMP_WORDS[i]}";
			parameterOffset=$(( $nWords-i-1 ))
			return;
		fi
	done
}

__resolveParameterTypeIfRelevant() {
	keyType="";
	valueType="";
	collectionType="";

	if [ -z "$parameterName" ]; then return; fi

</#noparse>
${resolveParameterTypeIfRelevant_Case}
<#noparse>
}

__resolveNextStep() {
	nextStep="none";
	currentWordType="";

	# no parameter yet, we are writing the first one
	if [ -z "$parameterName" ]; then
		if __startsWithDash $currentWord; then
			nextStep="parameter";
		fi
		return;
	fi

	# parameter exists but is not a collection
	if [ -z "$collectionType" ]; then
		# we are writing right after parameter name was specified
		if [ "$parameterOffset" == "1" ]; then
			# if it is a boolean and our words starts with dash, we allow another parameter right away as just he param name means we specify the value as true
			if ( [ "$valueType" == "boolean" ] && __startsWithDash $currentWord ); then
				nextStep="parameter";
			# this must mean we are writing a value
			else
				currentWordType="$valueType";
				nextStep="value"
			fi

		# if we are two positions after the param, we must be specifying another param
		elif [ "$parameterOffset" == "2" ]; then
			nextStep="parameter";
		fi
		return;
	fi

	# if we have a set or a list
	if [ "$collectionType" == "linear" ]; then
		# if there is at least one element, we allow next parameter to be specified
		if [ "$parameterOffset" -gt "1" ] && __startsWithDash $currentWord; then
			nextStep="parameter";
		# otherwise it must be a value
		else
			currentWordType="$valueType";
			nextStep="value";
		fi
	fi

	# if we have a map
	if [ "$collectionType" == "map" ]; then
		# if there is an even number of elements and at least 2, we allow next parameter to be specified
		if [ "$parameterOffset" -gt "2" ] && [ $(($parameterOffset%2)) -eq 1 ] && __startsWithDash $currentWord; then
			nextStep="parameter";
		else
			# otherwise we say odd number of positions after a map parameter name comes key, even number is value
			if [ $(($parameterOffset%2)) -eq 1 ]; then
				currentWordType="$keyType";
			else
				currentWordType="$valueType";
			fi
			nextStep="value";
		fi
		return;
	fi
}

__startsWithDash() {
	if [[ $1 == -* ]]; then true; else false; fi
}

__suggestParameterName() {
</#noparse>
${suggestParameterName_Case}
<#noparse>

	__removeUsedParamNames;
}

__removeUsedParamNames() {
	if [ "$nWords" -lt "4" ]; then
		return
	fi;

	local i;
	for (( i=$nWords-2 ; i>=0 ; i-- )) ; do
		if [ "${COMP_WORDS[i]}" == ":" ]; then
			return;
		elif __startsWithDash "${COMP_WORDS[i]}"; then
			__unsuggest "${COMP_WORDS[i]}";
		fi
	done
}

__suggestHelp() {
</#noparse>
	__suggest "${suggestHelp_CommandsList}";
<#noparse>
}

__suggestParameterValue() {
	case $currentWordType in
		boolean)
			__suggest "true false";;
		file)
			__suggestFile;;
		folder)
			__suggestFolder;;
</#noparse>
${suggestParameterValue_CustomCases}<#noparse>	esac
}

__suggest() {
	COMPREPLY+=($(compgen -W "$1" -- "$currentWord"));
}

__suggestFile() {
	__suggestFileOrFolder "-f";
}

__suggestFolder() {
	__suggestFileOrFolder "-d";
}

__suggestFileOrFolder() {
	case $SHELL in
		*/zsh) # compopt is not supported in ZSH
			;;
		*)
			compopt -o filenames;;
	esac
	IFS=$'\n'
    COMPREPLY+=($(compgen $1 -- "$currentWord"))
    IFS=$' \t\n'
}

__unsuggest() {
	# the extremely tedious way of removing $1 from COMPREPLY array
	local i new_array;
	for i in "${!COMPREPLY[@]}"; do
		if [ "${COMPREPLY[i]}" != "$1" ]; then
			new_array+=( "${COMPREPLY[i]}" );
		fi
	done
	COMPREPLY=("${new_array[@]}");
	unset new_array;
}
</#noparse>