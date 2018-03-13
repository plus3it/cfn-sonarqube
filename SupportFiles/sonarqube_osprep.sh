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
# shellcheck disable=SC2153,SC2034
SONARYUMDEF="${SONARQUBE_YUM_REPO_URL}"
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
## Install Sonarqube yum definition
function SqRepoSetup {

   SAVPWD="${PWD}"

   echo "Installing yum repo-definition for sonarqube RPM..."

   cd /etc/cfn/files || err_exit "The chdir operation failed"

   printf "Downloading sonarqube's repo-def... "
   # Choose download method based on URL
   if [[ $( echo ${SONARYUMDEF} | grep -q s3:// )$? -eq 0 ]]
   then
      aws s3 cp ${SONARYUMDEF} . && echo "Succeeded" ||
       err_exit "Failed to download ${SONARYUMDEF}"
   elif [[ $( echo ${SONARYUMDEF} | grep -qE "http://|https://" )$? -eq 0 ]]
   then
      curl -OskL ${SONARYUMDEF} && echo "Succeeded" ||
       err_exit "Failed to download ${SONARYUMDEF}"
   else
      err_exit "Unable to identify dowload method for ${SONARYUMDEF}"
   fi

   # Choose install method based on file-type
   if [[ $( file "${SONARYUMDEF//*\/}" | grep -q ' RPM ' )$? -eq 0 ]]
   then
      printf "Using yum to install repo-def... "
      yum install -q -y "${SONARYUMDEF//*\/}" && echo "Success" ||
        err_exit "Failed to install sonarqube's repo-def"
   elif [[ $( file "${SONARYUMDEF//*\/}" | grep -q ' ASCII ' )$? -eq 0 ]]
   then
      printf "Copying repo-def to system-location... "
      cp "${SONARYUMDEF//*\/}" /etc/yum.repos.d  && echo "Success" ||
        err_exit "Failed to install sonarqube's repo-def"
   else
      err_exit "Failed to identify install-type for ${SONARYUMDEF//*\/}"
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
SqRepoSetup
