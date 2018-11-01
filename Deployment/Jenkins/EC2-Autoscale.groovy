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
        string(name: 'TemplateUrl', description: 'S3-hosted URL for the EC2 autoscale template file')
        string(name: 'AdminPubkeyURL', description: 'URL the file containing the admin users SSH public keys')
        string(name: 'AmiId', description: 'ID of the AMI to launch')
        string(name: 'BackupBucket', description: 'Name of S3 bucket used for Sonarqube backup tasks')
        string(name: 'CfnEndpointUrl', defaultValue: 'https://cloudformation.us-east-1.amazonaws.com', description: 'URL to the CloudFormation Endpoint')
        string(name: 'DesiredCapacity', defaultValue: '1', description: 'Desired number of instances in the Autoscaling Group')
        string(name: 'Domainname', description: 'Suffix for Sonarqubes hostname and DNS record')
        string(name: 'ElbArn', description: 'ARN of the ELBv2 TargetGroup to attach scaling-group to')
        string(name: 'EpelRepo', defaultValue: 'epel', description: 'Name of networks EPEL repo')
        string(name: 'Hostname', defaultValue: 'sonarqube', description: 'Node-name for Sonarqubes hostname and DNS record')
        string(name: 'InstanceRoleName', description: 'IAM instance role to apply to the instance')
        string(name: 'InstanceRoleProfile', defaultValue: '', description: 'IAM instance-role profile to apply to the instance')
        string(name: 'InstanceType', defaultValue: 't2.large', description: 'Amazon EC2 instance type')
        string(name: 'KeyPairName', description: 'Public/private key pairs allowing the provisioning-user to securely connect to the instance after it launches')
        string(name: 'LdapAuthType', defaultValue: 'simple', description: 'Authentication-type to use with directory-service')
        string(name: 'LdapBaseDnGroups', defaultValue: '', description: 'Directory-node to descend from when searching for group memberships')
        string(name: 'LdapBaseDnUsers', defaultValue: '', description: 'Directory-node to descend from when searching for usernames')
        string(name: 'LdapBindDn', defaultValue: '', description: 'Distinguished-name of the authentication proxy-user account')
        string(name: 'LdapBindPassword', defaultValue: '', description: 'Password of the authentication proxy-user account')
        string(name: 'LdapRealm', description: 'FQDN-syle realm-name for directory objects')
        string(name: 'LdapUrl', defaultValue: '', description: 'URL to connect to in order access a directory-service source')
        string(name: 'LdapUseStartTLS', defaultValue: 'false', description: 'Directory-node to descend from when searching for usernames')
        string(name: 'LdapUserQuery', defaultValue: '(&(objectClass=user)(sAMAccountName={login}))', description: 'Search-expression used to find users in directory')
        string(name: 'MaxCapacity', defaultValue: '2', description: 'Maximum number of instances in the Autoscaling Group')
        string(name: 'MinCapacity', defaultValue: '1', description: 'Minimum number of instances in the Autoscaling Group')
        string(name: 'NoReboot', defaultValue: 'false', description: 'Controls whether to reboot the instance as the last step of cfn-init execution')
        string(name: 'NoUpdates', defaultValue: 'false', description: 'Controls whether to run yum update during a stack update - on the initial instance launch, Watchmaker _always_ installs updates')
        string(name: 'PipIndexFips', defaultValue: 'https://pypi.org/simple/', description: 'URL of pip index that is compatible with FIPS 140-2 requirements')
        string(name: 'PipRpm', defaultValue: 'python2-pip', description: 'Name of preferred pip RPM')
        string(name: 'PluginS3Location', description: 'S3-path containing Sonarqube plugins')
        string(name: 'ProvisionUser', defaultValue: 'builder', description: 'Default login user account name')
        string(name: 'PyStache', defaultValue: 'pystache', description: 'Name of preferred pystache RPM')
        string(name: 'SecurityGroupIds', description: 'List of security groups to apply to the instance')
        string(name: 'SonarqubeAppPrepUrl', description: 'URL of the Sonarqube setup script')
        string(name: 'SonarqubeDbHost', description: 'FQDN of the Sonarqube database')
        string(name: 'SonarqubeDbInst', description: 'Instance-name of the Sonarqube database')
        string(name: 'SonarqubeDbPass', description: 'Password of the Sonarqube master database-user')
        string(name: 'SonarqubeDbUser', description: 'Name of the Sonarqube master database-user')
        string(name: 'SonarqubeOsPrepUrl', description: 'URL of the script ready the OS for Sonarqube installation')
        string(name: 'SonarqubeRpmName', defaultValue: 'sonar', description: 'Name of Sonarqube RPM to install. Include release version if other-than-latest is desired. Example values would be: sonar, sonar-X.Y or sonar-X.Y.Z')
        string(name: 'SonarqubeUser', defaultValue: 'sonarqube', description: 'Userid the Sonarqube service runs under')
        string(name: 'SonarqubeYumRepo', defaultValue: 'http://downloads.sourceforge.net/project/sonar-pkg/rpm/sonar.repo', description: 'URL from which to download yum-repository information for SonarQube RPMs')
        string(name: 'SubnetIds', description: 'List of subnets to associate to the Autoscaling Group')
        string(name: 'ToggleCfnInitUpdate', defaultValue: 'A', description: 'A/B toggle that forces a change to instance metadata, triggering the cfn-init update sequence')
        string(name: 'ToggleNewInstances', defaultValue: 'A', description: 'A/B toggle that forces a change to instance userdata, triggering new instances via the Autoscale update policy')
        string(name: 'WatchmakerConfig', defaultValue: '', description: '(Optional) URL to a Watchmaker config file')
        string(name: 'WatchmakerEnvironment', defaultValue: '', description: 'Environment in which the instance is being deployed')
    }

    stages {
        stage ('Prepare Agent Environment') {
            steps {
                deleteDir()
                git branch: "${GitProjBranch}",
                    credentialsId: "${GitCred}",
                    url: "${GitProjUrl}"
                writeFile file: 'sonar.ec2.auto.parms.json',
                    text: /
                        [
                            {
                                "ParameterKey": "AdminPubkeyURL",
                                "ParameterValue": "${env.AdminPubkeyURL}"
                            },
                            {
                                "ParameterKey": "AmiId",
                                "ParameterValue": "${env.AmiId}"
                            },
                            {
                                "ParameterKey": "BackupBucket",
                                "ParameterValue": "${env.BackupBucket}"
                            },
                            {
                                "ParameterKey": "CfnEndpointUrl",
                                "ParameterValue": "${env.CfnEndpointUrl}"
                            },
                            {
                                "ParameterKey": "DesiredCapacity",
                                "ParameterValue": "${env.DesiredCapacity}"
                            },
                            {
                                "ParameterKey": "Domainname",
                                "ParameterValue": "${env.Domainname}"
                            },
                            {
                                "ParameterKey": "ElbArn",
                                "ParameterValue": "${env.ElbArn}"
                            },
                            {
                                "ParameterKey": "EpelRepo",
                                "ParameterValue": "${env.EpelRepo}"
                            },
                            {
                                "ParameterKey": "Hostname",
                                "ParameterValue": "${env.Hostname}"
                            },
                            {
                                "ParameterKey": "InstanceRoleName",
                                "ParameterValue": "${env.InstanceRoleName}"
                            },
                            {
                                "ParameterKey": "InstanceRoleProfile",
                                "ParameterValue": "${env.InstanceRoleProfile}"
                            },
                            {
                                "ParameterKey": "InstanceType",
                                "ParameterValue": "${env.InstanceType}"
                            },
                            {
                                "ParameterKey": "KeyPairName",
                                "ParameterValue": "${env.KeyPairName}"
                            },
                            {
                                "ParameterKey": "LdapAuthType",
                                "ParameterValue": "${env.LdapAuthType}"
                            },
                            {
                                "ParameterKey": "LdapBaseDnGroups",
                                "ParameterValue": "${env.LdapBaseDnGroups}"
                            },
                            {
                                "ParameterKey": "LdapBaseDnUsers",
                                "ParameterValue": "${env.LdapBaseDnUsers}"
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
                                "ParameterKey": "LdapUserQuery",
                                "ParameterValue": "${env.LdapUserQuery}"
                            },
                            {
                                "ParameterKey": "MaxCapacity",
                                "ParameterValue": "${env.MaxCapacity}"
                            },
                            {
                                "ParameterKey": "MinCapacity",
                                "ParameterValue": "${env.MinCapacity}"
                            },
                            {
                                "ParameterKey": "NoReboot",
                                "ParameterValue": "${env.NoReboot}"
                            },
                            {
                                "ParameterKey": "NoUpdates",
                                "ParameterValue": "${env.NoUpdates}"
                            },
                            {
                                "ParameterKey": "PipIndexFips",
                                "ParameterValue": "${env.PipIndexFips}"
                            },
                            {
                                "ParameterKey": "PipRpm",
                                "ParameterValue": "${env.PipRpm}"
                            },
                            {
                                "ParameterKey": "PluginS3Location",
                                "ParameterValue": "${env.PluginS3Location}"
                            },
                            {
                                "ParameterKey": "ProvisionUser",
                                "ParameterValue": "${env.ProvisionUser}"
                            },
                            {
                                "ParameterKey": "PyStache",
                                "ParameterValue": "${env.PyStache}"
                            },
                            {
                                "ParameterKey": "SecurityGroupIds",
                                "ParameterValue": "${env.SecurityGroupIds}"
                            },
                            {
                                "ParameterKey": "SonarqubeAppPrepUrl",
                                "ParameterValue": "${env.SonarqubeAppPrepUrl}"
                            },
                            {
                                "ParameterKey": "SonarqubeDbHost",
                                "ParameterValue": "${env.SonarqubeDbHost}"
                            },
                            {
                                "ParameterKey": "SonarqubeDbInst",
                                "ParameterValue": "${env.SonarqubeDbInst}"
                            },
                            {
                                "ParameterKey": "SonarqubeDbPass",
                                "ParameterValue": "${env.SonarqubeDbPass}"
                            },
                            {
                                "ParameterKey": "SonarqubeDbUser",
                                "ParameterValue": "${env.SonarqubeDbUser}"
                            },
                            {
                                "ParameterKey": "SonarqubeOsPrepUrl",
                                "ParameterValue": "${env.SonarqubeOsPrepUrl}"
                            },
                            {
                                "ParameterKey": "SonarqubeRpmName",
                                "ParameterValue": "${env.SonarqubeRpmName}"
                            },
                            {
                                "ParameterKey": "SonarqubeUser",
                                "ParameterValue": "${env.SonarqubeUser}"
                            },
                            {
                                "ParameterKey": "SonarqubeYumRepo",
                                "ParameterValue": "${env.SonarqubeYumRepo}"
                            },
                            {
                                "ParameterKey": "SubnetIds",
                                "ParameterValue": "${env.SubnetIds}"
                            },
                            {
                                "ParameterKey": "ToggleCfnInitUpdate",
                                "ParameterValue": "${env.ToggleCfnInitUpdate}"
                            },
                            {
                                "ParameterKey": "ToggleNewInstances",
                                "ParameterValue": "${env.ToggleNewInstances}"
                            },
                            {
                                "ParameterKey": "WatchmakerConfig",
                                "ParameterValue": "${env.WatchmakerConfig}"
                            },
                            {
                                "ParameterKey": "WatchmakerEnvironment",
                                "ParameterValue": "${env.WatchmakerEnvironment}"
                            }
                        ]
                    /
            }
        }
        stage ('Prepare AWS Environment') {
            steps {
                withCredentials(
                    [
                        [$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: "${AwsCred}", secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']
                    ]
                ) {
                    sh '''#!/bin/bash
                        echo "Attempting to delete any active ${CfnStackRoot}-Ec2Auto stacks... "
                        aws --region "${AwsRegion}" cloudformation delete-stack --stack-name "${CfnStackRoot}-Ec2Auto"

                        aws cloudformation wait stack-delete-complete --stack-name ${CfnStackRoot}-Ec2Auto --region ${AwsRegion}
                    '''
                }
            }
        }
        stage ('Launch SonarQube EC2 Autoscale Stack') {
            steps {
                withCredentials(
                    [
                        [$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: "${AwsCred}", secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']
                    ]
                ) {
                    sh '''#!/bin/bash
                        echo "Attempting to create stack ${CfnStackRoot}-Ec2Auto..."
                        aws --region "${AwsRegion}" cloudformation create-stack --stack-name "${CfnStackRoot}-Ec2Auto" \
                          --disable-rollback --capabilities CAPABILITY_NAMED_IAM \
                          --template-url "${TemplateUrl}" \
                          --parameters file://sonar.ec2.auto.parms.json

                        aws cloudformation wait stack-create-complete --stack-name ${CfnStackRoot}-Ec2Auto --region ${AwsRegion}
                    '''
                }
            }
         }
      }
   }
