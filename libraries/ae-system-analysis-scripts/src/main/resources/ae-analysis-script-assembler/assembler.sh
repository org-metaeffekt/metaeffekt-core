
set -e

if [ -z "${1}" ] || [ -z "${2}" ] ; then
  echo "usage: \"${0}\" inputFile outputFile"
  exit 2
fi

if [ "${1}" = "${2}" ] ; then
  echo "refusing to overwrite the input file!"
  exit 2
fi

inputFile="${1}"
outputFile="${2}"

if [ -d "$outputFile" ] ; then
  # fail early, a directory was specified as an output file
  printf "specified outputFile is actually a directory. aborting.\n"
  exit 9
fi

if [ -f "${outputFile}" ] ; then
  printf "moving ${outputFile} to ${outputFile}.old\n"
  mv "${outputFile}" "$outputFile.old"
fi

# check that the input file ends with \n or \n\r before trying to assemble. avoids problems with line-by-line logic
hasNewline="$(tail "${inputFile}" | wc -l)"
if [ -z "${hasNewline}" ] || [ "${hasNewline}" = "0" ] ; then
  printf "input script does not end with a newline and so isn't a valid posix file.\n" 1>&2
  printf "can't process script files that don't end with newlines due to how this script works. exiting."
  exit 11
fi

while IFS=$'\n' read -r line <&3; do
  # check if line starts with the replacer string
  replacerStatus="0"
  case "${line}" in
    "#INCLUDESOURCEHERE-"*[a-zA-Z] | "# INCLUDESOURCEHERE-"*[a-zA-Z]) replacerStatus="1" ;;
    "#INCLUDESOURCEHERE"* | "# INCLUDESOURCEHERE"* ) replacerStatus="-1" ;;
    *) replacerStatus="0" ;;
  esac
  if [ "${replacerStatus}" = "-1" ] ; then
    printf "an INCLUDESOURCEHERE statement is invalid.\n" >&2
    printf "line: %s\nreplacerStatus: %s\n" "${line}" "${replacerStatus}" >&2
    printf -- "- valid include lines stand alone\n" >&2
    printf -- "- valid include lines contain [a-zA-Z] characters as an id for the library file to include." >&2
    exit 12
  elif [ "${replacerStatus}" = "0" ] ; then
    # this is a regular code line without a replacement directive. just send it.
    printf "%s\n" "${line}" >> "${outputFile}"
  elif [ "${replacerStatus}" = "1" ] ; then
    # get the ID from the end of our statement
    filenameForInclusion=$(printf "%s" "${line}" | sed 's/#INCLUDESOURCEHERE-//g; s/# INCLUDESOURCEHERE-//g' | tr -cd "[[:alpha:]]")
    # cat the referenced library file into the output file in this very place
    cat "../ae-system-analysis-scripts/extraction-scripts-lib/${filenameForInclusion}-functions.sh" >> "${outputFile}"
  fi
done 3< "${inputFile}"
