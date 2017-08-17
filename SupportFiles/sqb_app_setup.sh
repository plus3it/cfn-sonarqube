#!/bin/sh
# shellcheck disable=SC2015,SC2086
#
# Script to set up Sonarqube so that it consults an PostGreSQL
# database for activity-tracking.
#
#################################################################
PROGNAME="$(basename $0)"
SQBURL="${1:-UNDEF}"
PGSQLUSER=${SONARQUBE_DBUSER:-UNDEF}
PGSQLPASS=${SONARQUBE_DBPASS:-UNDEF}
PGSQLHOST=${SONARQUBE_DBHOST:-UNDEF}
PGSQLINST=${SONARQUBE_DBINST:-UNDEF}

# Define an error-handler
function err_exit {
   logger -s -p kern.crit -t "${PROGNAME}" "${1}"
   exit 1
}

# Ensure URL to ZIP file has been passed
if [[ ${SQBURL} = UNDEF ]]
then
   err_exit 'Failed to pass URL for Sonarqube archive to script.'
fi

# Ensure env vars have been passed
if [[ ${PGSQLUSER} = UNDEF ]] ||
   [[ ${PGSQLPASS} = UNDEF ]] ||
   [[ ${PGSQLHOST} = UNDEF ]] ||
   [[ ${PGSQLINST} = UNDEF ]]
then
   err_exit 'Failed to pass env vars from parent script'
fi

# Ensure we're properly rooted
# shellcheck disable=SC2046
cd $(getent passwd $LOGNAME | cut -d: -f 6) || err_exit "Change-dir failed."

# Fetch sonarqube ZIP-bundle
printf "Fetching %s..." "${SQBURL}"
curl -o /tmp/sonarqube.zip -skL "${SQBURL}" && \
   echo "Success." || \
   err_exit 'Failed to fetch Sonarqube ZIP.'

# Set more vars..
SQBROOT=$(unzip -qql /tmp/sonarqube.zip | head -n1 | 
          tr -s ' ' | cut -d' ' -f5-)
SQBPROP=${SQBROOT}/conf/sonar.properties

# De-archive Sonarqube ZIP-bundle
printf "Unzipping Sonarqube... "
unzip -qq /tmp/sonarqube.zip && echo "Success." || \
   err_exit 'Failed to de-archive Sonarqube ZIP.'

# Create null Sonar properties file with proper SEL contexts
# ...saving off "DIST" file if appropriate
if [[ -f ${SQBPROP} ]]
then
   mv "${SQBPROP}" "${SQBPROP}-DIST"
   touch "${SQBPROP}" || err_exit "Failed to create ${SQBPROP}"
   chcon --reference "${SQBPROP}-DIST" "${SQBPROP}"
elif [[ -d ${SQBROOT}/conf/ ]]
then
   touch "${SQBPROP}" || err_exit "Failed to create ${SQBPROP}"
   chcon --reference "${SQBPROP}/conf" "${SQBPROP}"
fi

# Write the updated sonarqube properties file
printf "Writing sonarqube properties file... "
cat << EOF > "${SQBPROP}"
sonar.jdbc.username=${PGSQLUSER}
sonar.jdbc.password=${PGSQLPASS}
sonar.jdbc.url=jdbc:postgresql://${PGSQLHOST}/${PGSQLINST}
EOF

# shellcheck disable=SC2181
if [[ $? -eq 0 ]]
then
   echo "Sonarqube configured."
else
   # shellcheck disable=SC2016
   err_exit 'Failed to update parms in ${SQBPROP}'
fi
