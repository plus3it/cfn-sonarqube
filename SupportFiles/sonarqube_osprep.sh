#!/bin/bash
# shellcheck disable=SC2015,SC2155
#
# Script to install Sonarqube and dependencies
#
#################################################################
# shellcheck disable=SC2086
PROGNAME="$(basename ${0})"
# Ensure we've got our CFn envs (in case invoking via other than CFn)
while read -r ENV
do
  # shellcheck disable=SC2163
  export "${ENV}"
done < /etc/cfn/Sonarqube.envs
SONARROOTDIR="${SONARQUBE_SHARE_MOUNT}"
# shellcheck disable=SC2153
SONARUSER="${SONAR_USER}"
SHARETYPE="${SONARQUBE_SHARE_TYPE}"
SHAREURI="${SONARQUBE_SHARE_URI}"
RPMDEPLST=(
      postgresql
      postgresql-jdbc
      unzip
   )


##
## Set up an error logging and exit-state
function err_exit {
   local ERRSTR="${1}"
   local SCRIPTEXIT=${2:-1}

   # Our output channels
   echo "${ERRSTR}" > /dev/stderr
   logger -t "${PROGNAME}" -p kern.crit "${ERRSTR}"

   # Need our exit to be an integer
   if [[ ${SCRIPTEXIT} =~ ^[0-9]+$ ]]
   then
      exit "${SCRIPTEXIT}"
   else
      exit 1
   fi
}


##
## Install any missing RPMs
function InstMissingRPM {
   local INSTRPMS=()

   # Check if we're missing any RPMs
   for RPM in "${RPMDEPLST[@]}"
   do
      printf "Cheking for presence of %s... " "${RPM}"
      if [[ $(rpm --quiet -q "$RPM")$? -eq 0 ]]
      then
         echo "Already installed."
      else
         echo "Selecting for install"
         INSTRPMS+=(${RPM})
      fi
   done

   # Install any missing RPMs
    if [[ ${#INSTRPMS[@]} -ne 0 ]]
   then
      echo "Will attempt to install the following RPMS: ${INSTRPMS[*]}"
      yum install -y "${INSTRPMS[@]}" || \
         err_exit "Install of RPM-dependencies experienced failures"
   else
      echo "No RPM-dependencies to satisfy"
   fi
}

##
## Configure firewalld
function FwSetup {
   local SELMODE=$(getenforce)

   # Relax SEL as necessary
   if [[ ${SELMODE} = Enforcing ]]
   then
      printf "Temporarily relaxing SELinux mode... "
      setenforce 0 && echo "Done" || \
        err_exit 'Failed to relax SELinux mode'
   fi

   # Update firewalld config
   printf "Creating firewalld service for Sonarqube... "
   firewall-cmd --permanent --new-service=sonarqube || \
     err_exit 'Failed to initialize sonarqube firewalld service'
   printf "Setting short description for Sonarqube firewalld service... "
   firewall-cmd --permanent --service=sonarqube \
     --set-short="Sonarqube Service Ports" || \
     err_exit 'Failed to add short service description'
   printf "Setting long description for Sonarqube firewalld service... "
   firewall-cmd --permanent --service=sonarqube \
     --set-description="Firewalld options supporting Sonarqube deployments" || \
     err_exit 'Failed to add long service description'
   printf "Adding port 9000/tcp to Sonarqube firewalld service... "
   firewall-cmd --permanent --service=sonarqube --add-port=9000/tcp || \
     err_exit 'Failed to add firewalld exception for 9000/tcp'
   printf "Adding port 9001/tcp to Sonarqube firewalld service... "
   firewall-cmd --permanent --service=sonarqube --add-port=9001/tcp || \
     err_exit 'Failed to add firewalld exception for 9001/tcp'
   printf "Activating service in permanent firewalld configuration... "
   firewall-cmd --permanent --add-service=sonarqube || \
     err_exit 'Failed to activate service'
   printf "Reloading firewalld configuration... "
   firewall-cmd --reload || err_exit "Failed reloading firewalld configuration."

   # Revert SEL-mode
   setenforce "${SELMODE}"
}


####
## Main
####

# Call setup functions
FwSetup
InstMissingRPM

