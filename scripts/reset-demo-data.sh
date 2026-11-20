#!/usr/bin/env bash
# Explicit operator action only; never call from deployment or application startup.
set -euo pipefail
if [[ $# -lt 1 ]]; then
  echo 'Usage: bash scripts/reset-demo-data.sh /absolute/path/pandora-api.jar [--preview | --apply --confirm RESET-DEMO-DATA --backup-dir /private/backups]' >&2
  exit 2
fi
app_jar=$(realpath "$1")
shift
script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
java_cmd="${JAVA_HOME:+${JAVA_HOME}/bin/}java"
jar_cmd="${JAVA_HOME:+${JAVA_HOME}/bin/}jar"
command -v "$java_cmd" >/dev/null
command -v "$jar_cmd" >/dev/null
: "${PANDORA_DEMO_JDBC_URL:?Set the SAME JDBC URL used by the deployed application}"
driver_entry=''
while IFS= read -r entry; do
  case "$PANDORA_DEMO_JDBC_URL:$entry" in
    jdbc:h2:*:BOOT-INF/lib/h2-[0-9]*.jar | jdbc:mysql:*:BOOT-INF/lib/mysql-connector-j-[0-9]*.jar)
      [[ -z "$driver_entry" ]] || { echo 'Multiple JDBC drivers matched; inspect the application JAR.' >&2; exit 2; }
      driver_entry="$entry" ;;
  esac
done < <("$jar_cmd" tf "$app_jar")
[[ -n "$driver_entry" ]] || { echo 'No matching JDBC driver found in the Spring Boot JAR.' >&2; exit 2; }
# This trap only removes a verified, newly created temporary directory.
temp_dir=$(mktemp -d "${TMPDIR:-/tmp}/pandora-reset.XXXXXXXX")
trap 'rm -rf -- "$temp_dir"' EXIT
(cd -- "$temp_dir" && "$jar_cmd" xf "$app_jar" "$driver_entry")
"$java_cmd" -Dfile.encoding=UTF-8 --source 17 --class-path "$temp_dir/$driver_entry" "$script_dir/ResetDemoData.java" "$@"
