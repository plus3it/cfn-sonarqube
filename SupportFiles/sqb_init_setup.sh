#!/bin/bash
#
# Set up Sonarqube as a service that starts on boot
#################################################################
PROGNAME=$(basename $0)
SONARUSER=${SONAR_USER:-UNDEF}
SONARINIT="/etc/init.d/sonarqube"

# Error-outputter
function err_exit {
   logger -p kern.crit -t $PROGNAME -s "${1}"
   exit 1
}

# Link Sonarqube into system-bin
printf "Linking Sonarqube startup script to /usr/bin..."
case $(uname -m) in
   x86_64) 
      SONARHOME=$(getent passwd ${SONARUSER} | cut -d: -f6)
      SONARBIN=$(find ${SONARHOME} -type f | grep "linux-x86-64/sonar.sh")
      ln -s $SONARBIN /usr/bin/sonar || err_exit "Failed to create symlink"
      ;;
   i386|i486|i586|i686)
      echo "Unsupported platform: bailing... "
      exit 1
      ;;
   *)
      echo "Unknown platform: bailing... "
      exit 1
      ;;
esac

# Create Sonarqube init script
printf "Creating service file... "
install -b -m 755 /dev/null ${SONARINIT} || \
   err_exit 'Failed to create service control file.'
cat << EOF > ${SONARINIT}
#!/bin/sh
#
# rc file for SonarQube
#
# chkconfig: 345 96 10
# description: SonarQube system (www.sonarsource.org)
#
### BEGIN INIT INFO
# Provides: sonar
# Required-Start: $network
# Required-Stop: $network
# Default-Start: 3 4 5
# Default-Stop: 0 1 2 6
# Short-Description: SonarQube system (www.sonarsource.org)
# Description: SonarQube system (www.sonarsource.org)
### END INIT INFO
#################################################################

su - ${SONARUSER} -c "/usr/bin/sonar \$*"
EOF

if [[ $? -eq 0 ]]
then
   echo "Success."
else
   err_exit 'Failed to create service control file.'
fi


# Add Sonarqube service-entries
printf "Adding Sonarqube service... "
chkconfig --add sonarqube && echo "Success." || \
   err_exit 'Failed to add Sonarqube service.'
printf "Enabling Sonarqube service... "
chkconfig sonarqube on && echo "Success." || \
   err_exit 'Failed to enable Sonarqube service.'
printf "Starting Sonarqube service... "
service sonarqube start && echo "Success."  || \
   err_exit 'Failed to start Sonarqube service.'

# Add Sonarqube backup cron job(s)
(printf '30 22 * * * aws s3 sync ${HOME} s3://%s/backup/$(date "+%%u")\n' \
 ${SONARQUBE_S3_BACKUP}) | crontab -u ${SONARUSER} -
