#!/usr/bin/env bash

set -euo pipefail

# Find the jar, and download it if needed.
GJF_VERSION=1.19.2
JAR_NAME="google-java-format-${GJF_VERSION}-all-deps.jar"
JAR_URL="https://github.com/google/google-java-format/releases/download/v${GJF_VERSION}/${JAR_NAME}"
JAR_DIR="${HOME}/.cache/google-java-format"
JAR="${JAR_DIR}/${JAR_NAME}"
if [ ! -f ${JAR} ]; then
  mkdir -p "${JAR_DIR}"
  wget "${JAR_URL}" -O "${JAR}"
fi

# Some OS X users have installed GNU find as gfind.
if [ "$(uname)" = "Darwin" ] && [ -x "$(command -v gfind)" ]; then
  GNU_FIND=gfind
else
  GNU_FIND=find
fi

if [ "${1:-}" = "--diff" ]; then
  # All java files in the diff
  git diff --cached --name-only --diff-filter=ACMR | (grep ".*java$" || echo -n) > files_to_check
else
  # All java files in the project
  ${GNU_FIND} projects -regex '.*/src/main/.*\.java' -or -regex '.*/src/test/.*\.java' > files_to_check
fi

if [ ! -s files_to_check ]; then
  echo "No files to check"
  rm files_to_check
  exit 0
fi

if [ "${1:-}" = "--check" ]; then
  ARGS="--dry-run --set-exit-if-changed"
else
  ARGS="--replace"
fi

# Run the check
java -jar ${JAR} ${ARGS} @files_to_check || FAIL="fail"
rm -f files_to_check
if [ "${FAIL:-}" = "fail" ]; then
  echo -e "\nThe files listed above are not formatted correctly. Use $0 to fix these issues. We recommend you install the Eclipse or IntelliJ plugin for google-java-format version ${GJF_VERSION}."
  echo
  echo "To never have to deal with this again, enable Batfish's pre-commit integration: https://pre-commit.com/#install"
  exit 1
fi
