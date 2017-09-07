#!/bin/bash
# shellcheck disable=SC2154,SC2086,SC2015
#
# Set up Sonarqube as a service that starts on boot
#################################################################
PROGNAME=$(basename $0)
while read -r SQENV
do
   # shellcheck disable=SC2163
   export "${SQENV}"
done < /etc/cfn/Sonarqube.envs
SONARUSER=${SONAR_USER:-UNDEF}
SONARINIT="/etc/systemd/system/sonar.service"

# Error-outputter
function err_exit {
   logger -p kern.crit -t $PROGNAME -s "${1}"
   exit 1
}

# Exit if we have no user
if [[ ${SONARUSER} = UNDEF ]]
then
   err_exit 'No SonarQube user defined'
fi

# Link Sonarqube into system-bin
printf "Linking Sonarqube startup script to /usr/bin..."
case $(uname -m) in
   x86_64) 
      SONARHOME="$(getent passwd ${SONARUSER} | cut -d: -f6)"
      SONARBIN="$(find ${SONARHOME} -type f -name sonar.sh | grep linux-x86-64)"
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
install -b -m 755 /dev/null "${SONARINIT}" || \
   err_exit 'Failed to create service control file.'
cat << EOF > "${SONARINIT}"
[Unit]
Description=SonarQube
After=network.target network-online.target
Wants=network-online.target

[Service]
ExecStart=/home/sonarqube/sonarqube/bin/linux-x86-64/sonar.sh start
ExecStop=/home/sonarqube/sonarqube/bin/linux-x86-64/sonar.sh stop
ExecReload=/home/sonarqube/sonarqube/bin/linux-x86-64/sonar.sh restart
PIDFile=/home/sonarqube/sonarqube/bin/linux-x86-64/SonarQube.pid
Type=forking
User=${SONARUSER}


[Install]
WantedBy=multi-user.target
EOF

# shellcheck disable=SC2181
if [[ $? -eq 0 ]]
then
   echo "Success."
else
   err_exit 'Failed to create service control file.'
fi


# Enable as systemd service
systemctl daemon-reload || err_exit 'Failed reloading systemd units'
printf "Enabling Sonarqube service... "
systemctl enable sonar.service && echo "Success" || \
  err_exit 'Failed enabling systemd service'
printf "Starting Sonarqube service... "
systemctl start sonar.service && echo "Success" || \
  err_exit 'Failed starting systemd service'

# Add Sonarqube backup cron job(s)
# shellcheck disable=SC2016
(printf '30 22 * * * aws s3 sync ${HOME} s3://%s/backup/$(date "+%%u")\n' \
 ${SONARQUBE_S3_BACKUP}) | crontab -u ${SONARUSER} -
