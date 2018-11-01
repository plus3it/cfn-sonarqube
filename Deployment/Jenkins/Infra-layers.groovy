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
        string(name: 'TemplateUrl', description: 'S3-hosted URL for the Infrastructure layers template file')
        string(name: 'BucketTemplate', description: 'link to bucket template')
        string(name: 'IamTemplate', description: 'Link to IAM template')
        string(name: 'SgTemplateUri', description: 'Link to SG template')
        string(name: 'RdsTemplateUri', description: 'Link to RDS template')
        string(name: 'EfsTemplate', description: 'Link to EFS template')
        string(name: 'ElbTemplate', description: 'Link to ELB template')
        string(name: 'BucketInventoryTracking', defaultValue: 'false', description: 'Optional Whether to enable generic bucket inventory-tracking. Requires setting of the ReportingBucket parameter')
        string(name: 'FinalExpirationDays', defaultValue: '30', description: 'Number of days to retain objects before aging them out of the bucket')
        string(name: 'PluginsBucket', description: 'S3 bucket hosting Sonarqube installations feature-extending plugin')
        string(name: 'ReportingBucket', defaultValue: '', description: 'Optional Destination for storing analytics data. Must be provided in ARN format')
        string(name: 'SonarqubeBackupBucket', description: '(Optional: S3 Bucket to host Sonarqube content')
        string(name: 'RolePrefix', description: 'Optional Prefix to apply to IAM role')
        string(name: 'ServiceTld', defaultValue: 'amazonaws.com', description: 'TLD of the IAMable service-name')
        string(name: 'DbAdminName', description: 'Name of the SonarQube master database-user')
        string(name: 'DbAdminPass', description: 'Password of the SonarQube master database-user')
        string(name: 'DbDataSize', defaultValue: '15', description: 'Size in GiB of the RDS table-space to create')
        string(name: 'DbNodeName', description: 'NodeName of the SonarQube database')
        string(name: 'DbInstanceName', description: 'Instance-name of the SonarQube database')
        string(name: 'DbInstanceType', defaultValue: 'db.m4.large', description: 'Amazon RDS instance type')
        string(name: 'DbSnapshotId', defaultValue: '', description: '(Optional) RDS snapshot-ARN to clone new database from')
        string(name: 'HaSubnets', description: 'Select at least two subnets, each in different Availability Zones')
        string(name: 'PgsqlVersion', defaultValue: '9.6.6', description: 'The X.Y.Z version of the PostGreSQL database to deploy')
        string(name: 'BackendTimeout', defaultValue: '600', description: 'How long - in seconds - back-end connection may be idle before attempting session-cleanup')
        string(name: 'HaSubnets', description: 'Select three subnets - each from different Availability Zones')
        string(name: 'ElbSubnets', description: 'Public-facing subnets for ELB to proxy requests from')
        string(name: 'ProxyPrettyName', description: 'A short, human-friendly label to assign to the ELB - no capital letters')
        string(name: 'SonarqubeListenerCert', defaultValue: '', description: 'Name/ID of the ACM-managed SSL Certificate to protect public listener')
        string(name: 'SonarqubeServicePort', defaultValue: '9000', description: 'TCP Port number that the Sonarqube host listens to')
        string(name: 'TargetVPC', description: 'ID of the VPC to deploy cluster nodes into')
    }

    stages {
        stage ('Prepare Agent Environment') {
            steps {
                deleteDir()
                git branch: "${GitProjBranch}",
                    credentialsId: "${GitCred}",
                    url: "${GitProjUrl}"
                writeFile file: 'sonar-service-infra.parms.json',
                    text: /
                        [
                            {
                                "ParameterKey": "BucketTemplate",
                                "ParameterValue": "${env.BucketTemplate}"
                            },
                            {
                                "ParameterKey": "IamTemplate",
                                "ParameterValue": "${env.IamTemplate}"
                            },
                            {
                                "ParameterKey": "SgTemplateUri",
                                "ParameterValue": "${env.SgTemplateUri}"
                            },
                            {
                                "ParameterKey": "RdsTemplateUri",
                                "ParameterValue": "${env.RdsTemplateUri}"
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
                                "ParameterKey": "BucketInventoryTracking",
                                "ParameterValue": "${env.BucketInventoryTracking}"
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
                                "ParameterKey": "PluginsBucket",
                                "ParameterValue": "${env.PluginsBucket}"
                            },
                            {
                                "ParameterKey": "SonarqubeBackupBucket",
                                "ParameterValue": "${env.SonarqubeBackupBucket}"
                            },
                            {
                                "ParameterKey": "RolePrefix",
                                "ParameterValue": "${env.RolePrefix}"
                            },
                            {
                                "ParameterKey": "ServiceTld",
                                "ParameterValue": "${env.ServiceTld}"
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
                                "ParameterKey": "DbSnapshotId",
                                "ParameterValue": "${env.DbSnapshotId}"
                            },
                            {
                                "ParameterKey": "HaSubnets",
                                "ParameterValue": "${env.HaSubnets}"
                            },
                            {
                                "ParameterKey": "PgsqlVersion",
                                "ParameterValue": "${env.PgsqlVersion}"
                            },
                            {
                                "ParameterKey": "BackendTimeout",
                                "ParameterValue": "${env.BackendTimeout}"
                            },
                            {
                                "ParameterKey": "HaSubnets",
                                "ParameterValue": "${env.HaSubnets}"
                            },
                            {
                                "ParameterKey": "ElbSubnets",
                                "ParameterValue": "${env.ElbSubnets}"
                            },
                            {
                                "ParameterKey": "ProxyPrettyName",
                                "ParameterValue": "${env.ProxyPrettyName}"
                            },
                            {
                                "ParameterKey": "SonarqubeListenerCert",
                                "ParameterValue": "${env.SonarqubeListenerCert}"
                            },
                            {
                                "ParameterKey": "SonarqubeServicePort",
                                "ParameterValue": "${env.SonarqubeServicePort}"
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
                        echo "Attempting to delete any active ${CfnStackRoot}-Infra stacks... "
                        aws --region "${AwsRegion}" cloudformation delete-stack --stack-name "${CfnStackRoot}-Infra"

                        aws cloudformation wait stack-delete-complete --stack-name ${CfnStackRoot}-Infra --region ${AwsRegion}
                    '''
                }
            }
        }
        stage ('Launch Sonarqube Infrastructure Stack') {
            steps {
                withCredentials(
                    [
                        [$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: "${AwsCred}", secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']
                    ]
                ) {
                    sh '''#!/bin/bash
                        echo "Attempting to create stack ${CfnStackRoot}-Infra..."
                        aws --region "${AwsRegion}" cloudformation create-stack --stack-name "${CfnStackRoot}-Infra" \
                          --disable-rollback --capabilities CAPABILITY_NAMED_IAM \
                          --template-url "${TemplateUrl}" \
                          --parameters file://sonar-service-infra.parms.json

                        aws cloudformation wait stack-create-complete --stack-name ${CfnStackRoot}-Infra --region ${AwsRegion}
                    '''
                }
            }
        }
    }
}
