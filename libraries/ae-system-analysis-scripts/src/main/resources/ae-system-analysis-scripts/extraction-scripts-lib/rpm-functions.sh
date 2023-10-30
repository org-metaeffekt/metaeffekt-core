
runRpmExtract()
{
  if [ -z "$1" ] ; then
    echo "outDir (passed to function as \$1) was empty! aborting to not write output to root. This may be a bug."
    exit 1
  fi

  local outDir="$1"

  # list packages
  rpm -qa --qf '| %{NAME} | %{VERSION} | %{LICENSE} |\n' | sort > "${outDir}"/packages_rpm.txt

  # list packages names (no version included)
  rpm -qa --qf '%{NAME}\n' | sort > "${outDir}"/packages_rpm-name-only.txt

  # query package metadata and covered files
  packagenames=`cat "${outDir}"/packages_rpm-name-only.txt`
  for package in $packagenames
  do
    rpm -qi "$package" > "${outDir}"/package-meta/"${package}"_rpm.txt
    rpm -q "$package" --qf "[%{FILENAMES}\n]" | sort > "${outDir}"/package-files/"${package}"_files.txt
    # rpm doesn't support NUL-delimiters in query formats. trust that rpm disallows insane filenames.

    # query package's dependencies. record all types (like weak and backward) of dependencies if possible.
    packageDD="${outDir}/package-deps/${package}"
    mkdir -p "$packageDD"
    rpm -q --requires "$package" > "${packageDD}/requires.txt"
    rpm -q --recommends "$package" > "${packageDD}/recommends.txt"
    rpm -q --suggests "$package" > "${packageDD}/suggests.txt"

    rpm -q --supplements "$package" > "${packageDD}/supplements.txt"
    rpm -q --enhances "$package" > "${packageDD}/enhances.txt"
    rpm -q --provides "$package" > "${packageDD}/provides.txt"
  done
}

checkRpmFunctionsPresent()
{
  # dummy function to fail early if functions where not included correctly
  :
}
