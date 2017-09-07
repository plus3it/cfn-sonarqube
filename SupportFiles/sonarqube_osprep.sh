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

# Ensure persistent data storage is valid
function ValidShare {
   SHARESRVR="${SHAREURI/\:*/}"
   SHAREPATH=${SHAREURI/${SHARESRVR}\:\//}

   echo "Attempting to validate share-path"
   printf "\t- Attempting to mount %s... " "${SHARESRVR}"
   if [[ ${SHARETYPE} = glusterfs ]]
   then
      mount -t "${SHARETYPE}" "${SHARESRVR}":/"${SHAREPATH}" /mnt && echo "Success" ||
        err_exit "Failed to mount ${SHARESRVR}"
   elif [[ ${SHARETYPE} = nfs ]]
   then
      mount -t "${SHARETYPE}" "${SHARESRVR}":/ /mnt && echo "Success" ||
        err_exit "Failed to mount ${SHARESRVR}"
      printf "\t- Looking for %s in %s... " "${SHAREPATH}" "${SHARESRVR}"
      if [[ -d /mnt/${SHAREPATH} ]]
      then
         echo "Success"
      else
         echo "Not found."
         printf "Attempting to create %s in %s... " "${SHAREPATH}" "${SHARESRVR}"
         mkdir /mnt/"${SHAREPATH}" && echo "Success" ||
           err_exit "Failed to create ${SHAREPATH} in ${SHARESRVR}"
      fi
   fi

   printf "Cleaning up... "
   umount /mnt && echo "Success" || echo "Failed"
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
## Enable NFS-client pieces
function NfsClientStart {
   local NFSCLIENTSVCS=(
            rpcbind
            nfs-server
            nfs-lock
            nfs-idmap
         )

    # Enable and start services
    for SVC in "${NFSCLIENTSVCS[@]}"
    do
       printf "Enabling %s... " "${SVC}"
       systemctl enable "${SVC}" && echo "Success!" || \
          err_exit "Failed to enable ${SVC}"
       printf "Starting %s... " "${SVC}"
       systemctl start "${SVC}" && echo "Success!" || \
          err_exit "Failed to start ${SVC}"
    done
}

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

# Call firewall setup tasks
FwSetup

# Modify some behaviors depending on Sonarqube-home's share-type
case "${SHARETYPE}" in
   UNDEF)
      ;;
   nfs)
      RPMDEPLST+=(
            nfs-utils
            nfs4-acl-tools
         )
      (
       printf "%s\t%s\tnfs4\t" "${SHAREURI}" "${SONARROOTDIR}" ;
       printf "rw,relatime,vers=4.1,rsize=1048576,wsize=1048576," ;
       printf "namlen=255,hard,proto=tcp,timeo=600,retrans=2\t0 0\n"
      ) >> /etc/fstab || err_exit "Failed to add NFS volume to fstab"
      ;;
   glusterfs)
      RPMDEPLST+=(
            glusterfs
            glusterfs-fuse
            attr
         )
      (
       printf "%s\t%s\tglusterfs\t" "${SHAREURI}" "${SONARROOTDIR}" ;
       printf "defaults\t0 0\n"
      ) >> /etc/fstab || err_exit "Failed to add NFS volume to fstab"
      ;;
esac

# Call Setup functions
InstMissingRPM

# Start NFS Client services as necessary
if [[ $(rpm --quiet -q nfs-utils)$? -eq 0 ]]
then
   NfsClientStart
fi

# Mount persistent Sonarqube home directory
if [[ -d ${SONARROOTDIR} ]]
then
   echo "${SONARROOTDIR} already exists: skipping create"
else
   printf "Attempting to create %s... " "${SONARROOTDIR}"
   mkdir "${SONARROOTDIR}" && echo "Success!" || \
      err_exit "Failed to create Sonarqube root-dir"
fi

printf "Mounting %s... " "${SONARROOTDIR}"
mount "${SONARROOTDIR}" && echo "Done." || \
  err_exit "Failed to mount ${SONARROOTDIR}"

printf "Ensuring %s is writeable by %s... " "${SONARROOTDIR}" "${SONARUSER}"
chmod 001777 "${SONARROOTDIR}" && \
  echo "Done." || \
  err_exit "Failed setting perms on ${SONARROOTDIR}"
