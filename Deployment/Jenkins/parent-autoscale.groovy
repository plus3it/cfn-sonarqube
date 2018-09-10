pipeline {

    agent any

    options {
        buildDiscarder(
            logRotator(
                numToKeepStr: '5',
                daysToKeepStr: '30',
                artifactDaysToKeepStr: '30',
                artifactNumToKeepStr: '3'
            )
        )
        disableConcurrentBuilds()
        timeout(time: 60, unit: 'MINUTES')
    }

    environment {
        AWS_DEFAULT_REGION = "${AwsRegion}"
        AWS_CA_BUNDLE = '/etc/pki/tls/certs/ca-bundle.crt'
        REQUESTS_CA_BUNDLE = '/etc/pki/tls/certs/ca-bundle.crt'
    }

    parameters {
        string(name: 'AwsRegion', defaultValue: 'us-east-1', description: 'Amazon region to deploy resources into')
        string(name: 'AwsCred', description: 'Jenkins-stored AWS credential with which to execute cloud-layer commands')
        string(name: 'GitCred', description: 'Jenkins-stored Git credential with which to execute git commands')
        string(name: 'GitProjUrl', description: 'SSH URL from which to download the Jenkins git project')
        string(name: 'GitProjBranch', description: 'Project-branch to use from the Jenkins git project')
        string(name: 'CfnStackRoot', description: 'Unique token to prepend to all stack-element names')
        string(name: 'TemplateUrl', description: 'S3-hosted URL for the EC2 template file')
        string(name: 'BucketTemplate', description: 'URI for the template that creates SonarQubes S3 bucket(s)')
        string(name: 'Ec2Template', description: 'URI for the template that creates the EC2 instance to host the SonarQube application')
        string(name: 'EfsTemplate', description: 'URI for the template that creates the EFS service to host SonarQubes persistent data')
        string(name: 'ElbTemplate', description: 'URI for the template that creates the ELB providing public-to-private reverse-proxy service')
        string(name: 'IamTemplate', description: 'URI for the template that creates SonarQubes IAM role(s)')
        string(name: 'RdsTemplateUri', description: 'URI for the template that creates SonarQubes RDS database')
        string(name: 'SgTemplateUri', description: 'URI for the template that creates SonarQubes security groups')
        string(name: 'SonarOsPrepUrl', description: 'URL of the script ready the OS for Sonarqube installation')
        string(name: 'SonarAppSetupUrl', description: 'URL of the Sonarqube setup script')
        string(name: 'PluginsBucket', description: 'S3 bucket hosting Sonarqube installations feature-extending plugins')
        string(name: 'PluginFolder', description: 'S3-path containing Sonarqube plugins')
        string(name: 'AdminPubkeyURL', description: 'URL the file containing the admin users SSH public keys')
        string(name: 'PipRpm', defaultValue: 'python2-pip', description: 'Name of preferred pip RPM')
        string(name: 'ServiceTld', defaultValue: 'amazonaws.com', description: 'TLD of the created EFS-endpoint')
        string(name: 'SonarqubeListenerCert', defaultValue: '', description: 'Name/ID of the ACM-managed SSL Certificate to protect public listener')
        string(name: 'BackendTimeout', defaultValue: '600', description: 'How long - in seconds - back-end connection may be idle before attempting session-cleanup')
        string(name: 'DbInstanceName', description: 'Instance-name of the SonarQube database')
        string(name: 'DbNodeName', description: 'NodeName of the SonarQube database')
        string(name: 'DbAdminName', description: 'Name of the SonarQube master database-user')
        string(name: 'DbAdminPass', description: 'Password of the SonarQube master database-user')
        string(name: 'DbInstanceType', defaultValue: 'db.t2.small', description: 'Amazon RDS instance type')
        string(name: 'DbDataSize', defaultValue: '5', description: 'Size in GiB of the RDS table-space to create')
        string(name: 'DbSnapshotId', defaultValue: '', description: '(Optional) RDS snapshot-ARN to clone new database from')
        string(name: 'EpelRepo', defaultValue: 'epel', description: 'Name of networks EPEL repo')
        string(name: 'SonarqubeYumRepo', defaultValue: 'http://downloads.sourceforge.net/project/sonar-pkg/rpm/sonar.repo', description: 'URL from which to download yum-repository information for SonarQube RPMs')
        string(name: 'HaSubnets', description: 'Select three subnets - each from different Availability Zones')
        string(name: 'Hostname', defaultValue: 'sonarqube', description: 'Node-name for Sonarqubes hostname and DNS record')
        string(name: 'SonarqubeServicePort', defaultValue: '9000', description: 'TCP Port number that the Sonarqube host listens to')
        string(name: 'TargetVPC', description: 'ID of the VPC to deploy cluster nodes into')
        string(name: 'SonarqubeRpmName', defaultValue: 'sonar', description: 'Name of Sonarqube RPM to install. Include release version if other-than-latest is desired. Example values would be: sonar, sonar-X.Y or sonar-X.Y.Z')
        string(name: 'RolePrefix', description: 'Prefix to apply to IAM role to make things a bit prettier (optional)')
        string(name: 'Domainname', description: 'Suffix for Sonarqubes hostname and DNS record')
        string(name: 'ProxyPrettyName', description: 'A short, human-friendly label to assign to the ELB (no capital letters)')
        string(name: 'InstanceType', defaultValue: 't2.large', description: 'Amazon EC2 instance type')
        string(name: 'LdapRealm', description: 'FQDN-syle realm-name for directory objects')
        string(name: 'LdapUrl', defaultValue: '', description: 'URL to connect to in order access a directory-service source')
        string(name: 'LdapUseStartTLS', defaultValue: 'false', description: 'Directory-node to descend from when searching for usernames')
        string(name: 'LdapAuthType', defaultValue: 'simple', description: 'Authentication-type to use with directory-service')
        string(name: 'LdapBindDn', defaultValue: '', description: 'Distinguished-name of the authentication proxy-user account')
        string(name: 'LdapBindPassword', defaultValue: '', description: 'Password of the authentication proxy-user account')
        string(name: 'LdapBaseDnUsers', defaultValue: '', description: 'Directory-node to descend from when searching for usernames')
        string(name: 'LdapBaseDnGroups', defaultValue: '', description: 'Directory-node to descend from when searching for group memberships')
        string(name: 'LdapUserQuery', defaultValue: '(&(objectClass=user)(sAMAccountName={login}))', description: 'Search-expression used to find users in directory')
        string(name: 'AmiId', description: 'ID of the AMI to launch')
        string(name: 'KeyPairName', description: 'Public/private key pairs allowing the provisioning-user to securely connect to the instance after it launches')
        string(name: 'PgsqlVersion', defaultValue: '9.6.6', description: 'The X.Y.Z version of the PostGreSQL database to deploy')
        string(name: 'PyStache', defaultValue: 'pystache', description: 'Name of preferred pystache RPM')
        string(name: 'SonarqubeBackupBucket', description: 'S3 Bucket to host Sonarqube content (Optional)')
        string(name: 'ProvisionUser', defaultValue: 'sonarbuild', description: 'Default login user account name')
        string(name: 'ElbSubnets', description: 'Public-facing subnets for ELB to proxy requests from')
        string(name: 'PipIndexFips', defaultValue: 'https://pypi.org/simple/', description: 'URL of pip index that is compatible with FIPS 140-2 requirements')
        string(name: 'WatchmakerConfig', defaultValue: '', description: '(Optional) URL to a Watchmaker config file')
        string(name: 'WatchmakerEnvironment', defaultValue: '', description: 'Environment in which the instance is being deployed')
        string(name: 'BucketInventoryTracking', defaultValue: 'false', description: '(Optional) Whether to enable generic bucket inventory-tracking. Requires setting of the ReportingBucket parameter')
        string(name: 'DesiredCapacity', defaultValue: '1', description: 'Desired number of instances in the Autoscaling Group')
        string(name: 'FinalExpirationDays', defaultValue: '30', description: 'Number of days to retain objects before aging them out of the bucket')
        string(name: 'ReportingBucket', defaultValue: '', description: '(Optional) Destination for storing analytics data. Must be provided in ARN format')
        string(name: 'ToggleCfnInitUpdate', defaultValue: 'A', description: 'A/B toggle that forces a change to instance metadata, triggering the cfn-init update sequence')
        string(name: 'ToggleNewInstances', defaultValue: 'A', description: 'A/B toggle that forces a change to instance userdata, triggering new instances via the Autoscale update policy')
    }

    stages {
        stage ('Prepare Agent Environment') {
            steps {
                deleteDir()
                git branch: "${GitProjBranch}",
                    credentialsId: "${GitCred}",
                    url: "${GitProjUrl}"
                writeFile file: 'InfraStack.parms.json',
                    text: /
                    [
                      {
                        "ParameterKey": "BucketTemplate",
                        "ParameterValue": "${env.BucketTemplate}"
                      },
                      {
                        "ParameterKey": "Ec2Template",
                        "ParameterValue": "${env.Ec2Template}"
                      },
                      {
                        "ParameterKey": "EfsTemplate",
                        "ParameterValue": "${env.EfsTemplate}"
                      },
                      {
                        "ParameterKey": "ElbTemplate",
                        "ParameterValue": "${env.ElbTemplate}"
                      },
                      {
                        "ParameterKey": "IamTemplate",
                        "ParameterValue": "${env.IamTemplate}"
                      },
                      {
                        "ParameterKey": "RdsTemplateUri",
                        "ParameterValue": "${env.RdsTemplateUri}"
                      },
                      {
                        "ParameterKey": "SgTemplateUri",
                        "ParameterValue": "${env.SgTemplateUri}"
                      },
                      {
                        "ParameterKey": "SonarOsPrepUrl",
                        "ParameterValue": "${env.SonarOsPrepUrl}"
                      },
                      {
                        "ParameterKey": "SonarAppSetupUrl",
                        "ParameterValue": "${env.SonarAppSetupUrl}"
                      },
                      {
                        "ParameterKey": "PluginsBucket",
                        "ParameterValue": "${env.PluginsBucket}"
                      },
                      {
                        "ParameterKey": "PluginFolder",
                        "ParameterValue": "${env.PluginFolder}"
                      },
                      {
                        "ParameterKey": "AdminPubkeyURL",
                        "ParameterValue": "${env.AdminPubkeyURL}"
                      },
                      {
                        "ParameterKey": "PipRpm",
                        "ParameterValue": "${env.PipRpm}"
                      },
                      {
                        "ParameterKey": "ServiceTld",
                        "ParameterValue": "${env.ServiceTld}"
                      },
                      {
                        "ParameterKey": "SonarqubeListenerCert",
                        "ParameterValue": "${env.SonarqubeListenerCert}"
                      },
                      {
                        "ParameterKey": "BackendTimeout",
                        "ParameterValue": "${env.BackendTimeout}"
                      },
                      {
                        "ParameterKey": "DbInstanceName",
                        "ParameterValue": "${env.DbInstanceName}"
                      },
                      {
                        "ParameterKey": "DbNodeName",
                        "ParameterValue": "${env.DbNodeName}"
                      },
                      {
                        "ParameterKey": "DbAdminName",
                        "ParameterValue": "${env.DbAdminName}"
                      },
                      {
                        "ParameterKey": "DbAdminPass",
                        "ParameterValue": "${env.DbAdminPass}"
                      },
                      {
                        "ParameterKey": "DbInstanceType",
                        "ParameterValue": "${env.DbInstanceType}"
                      },
                      {
                        "ParameterKey": "DbDataSize",
                        "ParameterValue": "${env.DbDataSize}"
                      },
                      {
                        "ParameterKey": "DbSnapshotId",
                        "ParameterValue": "${env.DbSnapshotId}"
                      },
                      {
                        "ParameterKey": "EpelRepo",
                        "ParameterValue": "${env.EpelRepo}"
                      },
                      {
                        "ParameterKey": "SonarqubeYumRepo",
                        "ParameterValue": "${env.SonarqubeYumRepo}"
                      },
                      {
                        "ParameterKey": "HaSubnets",
                        "ParameterValue": "${env.HaSubnets}"
                      },
                      {
                        "ParameterKey": "Hostname",
                        "ParameterValue": "${env.Hostname}"
                      },
                      {
                        "ParameterKey": "SonarqubeServicePort",
                        "ParameterValue": "${env.SonarqubeServicePort}"
                      },
                      {
                        "ParameterKey": "TargetVPC",
                        "ParameterValue": "${env.TargetVPC}"
                      },
                      {
                        "ParameterKey": "SonarqubeRpmName",
                        "ParameterValue": "${env.SonarqubeRpmName}"
                      },
                      {
                        "ParameterKey": "RolePrefix",
                        "ParameterValue": "${env.RolePrefix}"
                      },
                      {
                        "ParameterKey": "Domainname",
                        "ParameterValue": "${env.Domainname}"
                      },
                      {
                        "ParameterKey": "ProxyPrettyName",
                        "ParameterValue": "${env.ProxyPrettyName}"
                      },
                      {
                        "ParameterKey": "InstanceType",
                        "ParameterValue": "${env.InstanceType}"
                      },
                      {
                        "ParameterKey": "LdapRealm",
                        "ParameterValue": "${env.LdapRealm}"
                      },
                      {
                        "ParameterKey": "LdapUrl",
                        "ParameterValue": "${env.LdapUrl}"
                      },
                      {
                        "ParameterKey": "LdapUseStartTLS",
                        "ParameterValue": "${env.LdapUseStartTLS}"
                      },
                      {
                        "ParameterKey": "LdapAuthType",
                        "ParameterValue": "${env.LdapAuthType}"
                      },
                      {
                        "ParameterKey": "LdapBindDn",
                        "ParameterValue": "${env.LdapBindDn}"
                      },
                      {
                        "ParameterKey": "LdapBindPassword",
                        "ParameterValue": "${env.LdapBindPassword}"
                      },
                      {
                      "ParameterKey": "LdapBaseDnUsers",
                      "ParameterValue": "${env.LdapBaseDnUsers}"
                      },
                      {
                      "ParameterKey": "LdapBaseDnGroups",
                      "ParameterValue": "${env.LdapBaseDnGroups}"
                      },
                      {
                        "ParameterKey": "LdapUserQuery",
                        "ParameterValue": "${env.LdapUserQuery}"
                      },
                      {
                        "ParameterKey": "AmiId",
                        "ParameterValue": "${env.AmiId}"
                      },
                      {
                        "ParameterKey": "KeyPairName",
                        "ParameterValue": "${env.KeyPairName}"
                      },
                      {
                      "ParameterKey": "PgsqlVersion",
                      "ParameterValue": "${env.PgsqlVersion}"
                      },
                      {
                      "ParameterKey": "PyStache",
                      "ParameterValue": "${env.PyStache}"
                      },
                      {
                        "ParameterKey": "SonarqubeBackupBucket",
                        "ParameterValue": "${env.SonarqubeBackupBucket}"
                      },
                      {
                        "ParameterKey": "ProvisionUser",
                        "ParameterValue": "${env.ProvisionUser}"
                      },
                      {
                        "ParameterKey": "ElbSubnets",
                        "ParameterValue": "${env.ElbSubnets}"
                      },
                      {
                      "ParameterKey": "PipIndexFips",
                      "ParameterValue": "${env.PipIndexFips}"
                      },
                      {
                      "ParameterKey": "WatchmakerConfig",
                      "ParameterValue": "${env.WatchmakerConfig}"
                      },
                      {
                      "ParameterKey": "WatchmakerEnvironment",
                      "ParameterValue": "${env.WatchmakerEnvironment}"
                      },
                      {
                        "ParameterKey": "BucketInventoryTracking",
                        "ParameterValue": "${env.BucketInventoryTracking}"
                      },
                      {
                        "ParameterKey": "DesiredCapacity",
                        "ParameterValue": "${env.DesiredCapacity}"
                      },
                      {
                        "ParameterKey": "FinalExpirationDays",
                        "ParameterValue": "${env.FinalExpirationDays}"
                      },
                      {
                      "ParameterKey": "ReportingBucket",
                      "ParameterValue": "${env.ReportingBucket}"
                      },
                      {
                      "ParameterKey": "ToggleCfnInitUpdate",
                      "ParameterValue": "${env.ToggleCfnInitUpdate}"
                      },
                      {
                      "ParameterKey": "ToggleNewInstances",
                      "ParameterValue": "${env.ToggleNewInstances}"
                      }
                    ]
                   /
                }
            }
        stage ('Prepare AWS Environment') {
            options {
                timeout(time: 1, unit: 'HOURS')
            }
            steps {
                withCredentials(
                    [
                        [$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: "${AwsCred}", secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'],
                        sshUserPrivateKey(credentialsId: "${GitCred}", keyFileVariable: 'SSH_KEY_FILE', passphraseVariable: 'SSH_KEY_PASS', usernameVariable: 'SSH_KEY_USER')
                    ]
                ) {
                    sh '''#!/bin/bash
                        echo "Attempting to delete any active ${CfnStackRoot}-ParAuto-${BUILD_NUMBER} stacks... "
                        aws --region "${AwsRegion}" cloudformation delete-stack --stack-name "${CfnStackRoot}-ParAuto-${BUILD_NUMBER}"

                        aws cloudformation wait stack-delete-complete --stack-name ${CfnStackRoot}-ParAuto-${BUILD_NUMBER} --region ${AwsRegion}
                    '''
                }
            }
        }
        stage ('Launch SonarQube Parent Autoscale Stack') {
            options {
                timeout(time: 1, unit: 'HOURS')
            }
            steps {
                withCredentials(
                    [
                        [$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: "${AwsCred}", secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'],
                        sshUserPrivateKey(credentialsId: "${GitCred}", keyFileVariable: 'SSH_KEY_FILE', passphraseVariable: 'SSH_KEY_PASS', usernameVariable: 'SSH_KEY_USER')
                    ]
                ) {
                    sh '''#!/bin/bash
                        echo "Attempting to create stack ${CfnStackRoot}-ParAuto-${BUILD_NUMBER}..."
                        aws --region "${AwsRegion}" cloudformation create-stack --stack-name "${CfnStackRoot}-ParAuto-${BUILD_NUMBER}" \
                          --disable-rollback --capabilities CAPABILITY_NAMED_IAM \
                          --template-url "${TemplateUrl}" \
                          --parameters file://InfraStack.parms.json

                        aws cloudformation wait stack-create-complete --stack-name ${CfnStackRoot}-ParAuto-${BUILD_NUMBER} --region ${AwsRegion}
                    '''
                }
            }
        }
    }
}
