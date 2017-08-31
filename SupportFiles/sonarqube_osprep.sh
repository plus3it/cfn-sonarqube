#!/bin/bash
# shellcheck disable=SC2015
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



####
## Main
####

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
       printf "%s\t%s\tnfs4\t" "${SHAREURI}" "${JIRADCHOME}" ;
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
       printf "%s\t%s\tglusterfs\t" "${SHAREURI}" "${JIRADCHOME}" ;
       printf "defaults\t0 0\n"
      ) >> /etc/fstab || err_exit "Failed to add NFS volume to fstab"
      ;;
esac

# Call Setup functions
InstMissingRPM
