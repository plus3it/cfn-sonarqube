#!/bin/bash
# shellcheck disable=SC2015,SC2086
#
# Script to set up Sonarqube so that it consults an PostGreSQL
# database for activity-tracking.
#
#################################################################
PROGNAME="$(basename $0)"
while read -r SQENV
do
   # shellcheck disable=SC2163
   export "${SQENV}"
done < /etc/cfn/Sonarqube.envs
PGSQLUSER=${SONARQUBE_DBUSER:-UNDEF}
PGSQLPASS=${SONARQUBE_DBPASS:-UNDEF}
PGSQLHOST=${SONARQUBE_DBHOST:-UNDEF}
PGSQLINST=${SONARQUBE_DBINST:-UNDEF}
SONARHOME="$(getent passwd $SONAR_USER | cut -d: -f 6)"

# Define an error-handler
function err_exit {
   logger -s -p kern.crit -t "${PROGNAME}" "${1}"
   exit 1
}


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
cd "${SONARHOME}" || err_exit "Change-dir failed."

# Install Sonarqube binaries
printf "Attempting to install ${SONARQUBE_RPM} rpm... "
yum install -qy "${SONARQUBE_RPM}" && echo "Success" ||
  err_exit "Installation of ${SONARQUBE_RPM} rpm failed"

# Set more vars..
SQBPROP="$(rpm -ql ${SONARQUBE_RPM} | grep sonar.properties)"
SQBPLUGD="$(rpm -ql ${SONARQUBE_RPM} | grep extensions/plugins$)"

# Pull down extra plugins
echo "Pulling down plugins..."
aws s3 sync --delete ${SONARQUBE_PLUGIN_LOC} "${SQBPLUGD}" && 
 printf "\nDone downloading plugins!\n" ||
 printf "\nOne or more errores experienced during plugin-download\n"

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
sonar.security.realm=LDAP
ldap.url=${SONARQUBE_LDAP_URL}
ldap.realm=${SONARQUBE_LDAP_REALM}
ldap.StartTLS=${SONARQUBE_LDAP_USETLS}
ldap.bindDn=${SONARQUBE_LDAP_BINDDN}
ldap.bindPassword=${SONARQUBE_LDAP_BINDPASS}
ldap.user.baseDn=${SONARQUBE_LDAP_BASEDN_USERS}
ldap.user.request=${SONARQUBE_LDAP_QUERYSTRING}
EOF

# shellcheck disable=SC2181
if [[ $? -eq 0 ]]
then
   echo "Sonarqube configured."
else
   # shellcheck disable=SC2016
   err_exit 'Failed to update parms in ${SQBPROP}'
fi


