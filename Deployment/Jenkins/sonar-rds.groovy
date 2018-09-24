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
        string(name: 'AdminPubkeyURL', description: 'URL the file containing the admin users SSH public keys')
        string(name: 'DbAdminName', description: 'Name of the SonarQube master database-user')
        string(name: 'DbAdminPass', description: 'Password of the SonarQube master database-user')
        string(name: 'DbDataSize', defaultValue: '5', description: 'Size in GiB of the RDS table-space to create')
        string(name: 'DbIsMultiAz', defaultValue: 'false', description: 'Select whether to create a multi-AZ RDS deployment')
        string(name: 'DbNodeName', description: 'NodeName of the SonarQube database')
        string(name: 'DbInstanceName', description: 'Instance-name of the SonarQube database')
        string(name: 'DbInstanceType', defaultValue: 'db.t2.small', description: 'Amazon RDS instance type')
        string(name: 'DbSecurityGroup', defaultValue: '', description: 'List of security groups to apply to the RDS database')
        string(name: 'DbSnapshotId', defaultValue: '', description: '(Optional) RDS snapshot-ARN to clone new database from')
        string(name: 'DbStorageType', defaultValue: 'gp2', description: 'Type of storage used to host DB-data')
        string(name: 'DbStorageIops', defaultValue: '1000', description: 'Provisioned-IOPS of storage to used to host DB-data')
        string(name: 'DbSubnets', description: 'Select at least two subnets, each in different Availability Zones')
        string(name: 'PgsqlVersion', defaultValue: '9.6.6', description: 'The X.Y.Z version of the PostGreSQL database to deploy')
        string(name: 'TargetVPC', description: 'ID of the VPC to deploy cluster nodes into')
    }

    stages {
        stage ('Prepare Agent Environment') {
            steps {
                deleteDir()
                git branch: "${GitProjBranch}",
                    credentialsId: "${GitCred}",
                    url: "${GitProjUrl}"
                writeFile file: 'sonar.rds.parms.json',
                    text: /
                        [
                            {
                                "ParameterKey": "AdminPubkeyURL",
                                "ParameterValue": "${env.AdminPubkeyURL}"
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
                                "ParameterKey": "DbDataSize",
                                "ParameterValue": "${env.DbDataSize}"
                            },
                            {
                                "ParameterKey": "DbIsMultiAz",
                                "ParameterValue": "${env.DbIsMultiAz}"
                            },
                            {
                                "ParameterKey": "DbNodeName",
                                "ParameterValue": "${env.DbNodeName}"
                            },
                            {
                                "ParameterKey": "DbInstanceName",
                                "ParameterValue": "${env.DbInstanceName}"
                            },
                            {
                                "ParameterKey": "DbInstanceType",
                                "ParameterValue": "${env.DbInstanceType}"
                            },
                            {
                                "ParameterKey": "DbInstanceName",
                                "ParameterValue": "${env.DbInstanceName}"
                            },
                            {
                                "ParameterKey": "DbInstanceType",
                                "ParameterValue": "${env.DbInstanceType}"
                            },
                            {
                                "ParameterKey": "DbSecurityGroup",
                                "ParameterValue": "${env.DbSecurityGroup}"
                            },
                            {
                                "ParameterKey": "DbSnapshotId",
                                "ParameterValue": "${env.DbSnapshotId}"
                            },
                            {
                                "ParameterKey": "DbStorageType",
                                "ParameterValue": "${env.DbStorageType}"
                            },
                            {
                                "ParameterKey": "DbStorageIops",
                                "ParameterValue": "${env.DbStorageIops}"
                            },
                            {
                                "ParameterKey": "DbSubnets",
                                "ParameterValue": "${env.DbSubnets}"
                            },
                            {
                                "ParameterKey": "PgsqlVersion",
                                "ParameterValue": "${env.PgsqlVersion}"
                            },
                            {
                                "ParameterKey": "TargetVPC",
                                "ParameterValue": "${env.TargetVPC}"
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
                        echo "Attempting to delete any active ${CfnStackRoot}-Rds-${BUILD_NUMBER} stacks... "
                        aws --region "${AwsRegion}" cloudformation delete-stack --stack-name "${CfnStackRoot}-Rds-${BUILD_NUMBER}"

                        aws cloudformation wait stack-delete-complete --stack-name ${CfnStackRoot}-Rds-${BUILD_NUMBER} --region ${AwsRegion}
                    '''
                }
            }
        }
        stage ('Launch SonarQube RDS Stack') {
            steps {
                withCredentials(
                    [
                        [$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: "${AwsCred}", secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']
                    ]
                ) {
                    sh '''#!/bin/bash
                        echo "Attempting to create stack ${CfnStackRoot}-Rds-${BUILD_NUMBER}..."
                        aws --region "${AwsRegion}" cloudformation create-stack --stack-name "${CfnStackRoot}-Rds-${BUILD_NUMBER}" \
                          --disable-rollback --capabilities CAPABILITY_NAMED_IAM \
                          --template-url "${TemplateUrl}" \
                          --parameters file://sonar.rds.parms.json

                        aws cloudformation wait stack-create-complete --stack-name ${CfnStackRoot}-Rds-${BUILD_NUMBER} --region ${AwsRegion}
                    '''
                }
            }
        }
    }
}
