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
        string(name: 'BucketTemplate', description: 'link to bucket template')
        string(name: 'IamRoleTemplate', description: 'Link to IAM template')
        string(name: 'SecurityGroupTemplate', description: 'Link to SG template')
        string(name: 'BucketInventoryTracking', defaultValue: 'false', description: 'Optional Whether to enable generic bucket inventory-tracking. Requires setting of the ReportingBucket parameter')
        string(name: 'FinalExpirationDays', defaultValue: '30', description: 'Number of days to retain objects before aging them out of the bucket')
        string(name: 'ReportingBucket', defaultValue: '', description: 'Optional Destination for storing analytics data. Must be provided in ARN format')
        string(name: 'RetainIncompleteDays', defaultValue: '3', description: 'Number of days to retain objects that were not completely uploaded')
        string(name: 'SonarqubeBackupBucket', description: '(Optional: S3 Bucket to host Sonarqube content')
        string(name: 'TierToS3Days', defaultValue: '5', description: 'Number of days to retain objects in standard storage tier')
        string(name: 'BackupBucketArn', description: 'S3 Bucket to host Sonarqube backup-data')
        string(name: 'PluginsBucket', description: 'S3 Bucket hosting Sonarqube plugins')
        string(name: 'RolePrefix', description: 'Optional Prefix to apply to IAM role')
        string(name: 'ServiceTld', defaultValue: 'amazonaws.com', description: 'TLD of the IAMable service-name')
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
                                "ParameterKey": "IamRoleTemplate",
                                "ParameterValue": "${env.IamRoleTemplate}"
                            },
                            {
                                "ParameterKey": "SecurityGroupTemplate",
                                "ParameterValue": "${env.SecurityGroupTemplate}"
                            },
                            {
                                "ParameterKey": "BucketInventoryTracking",
                                "ParameterValue": "${env.BucketInventoryTracking}"
                            },
                            {
                                "ParameterKey": "JenkinsBackupBucket",
                                "ParameterValue": "${env.JenkinsBackupBucket}"
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
                                "ParameterKey": "RetainIncompleteDays",
                                "ParameterValue": "${env.RetainIncompleteDays}"
                            },
                            {
                                "ParameterKey": "SonarqubeBackupBucket",
                                "ParameterValue": "${env.SonarqubeBackupBucket}"
                            },
                            {
                                "ParameterKey": "TierToS3Days",
                                "ParameterValue": "${env.TierToS3Days}"
                            },
                            {
                                "ParameterKey": "BackupBucketArn",
                                "ParameterValue": "${env.BackupBucketArn}"
                            },
                            {
                                "ParameterKey": "PluginsBucket",
                                "ParameterValue": "${env.PluginsBucket}"
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
                        echo "Attempting to delete any active ${CfnStackRoot}-Infra-${BUILD_NUMBER} stacks... "
                        aws --region "${AwsRegion}" cloudformation delete-stack --stack-name "${CfnStackRoot}-Infra-${BUILD_NUMBER}"

                        aws cloudformation wait stack-delete-complete --stack-name ${CfnStackRoot}-Infra-${BUILD_NUMBER} --region ${AwsRegion}
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
                        echo "Attempting to create stack ${CfnStackRoot}-Infra-${BUILD_NUMBER}..."
                        aws --region "${AwsRegion}" cloudformation create-stack --stack-name "${CfnStackRoot}-Infra-${BUILD_NUMBER}" \
                          --disable-rollback --capabilities CAPABILITY_NAMED_IAM \
                          --template-body file://Templates/make_jenkins_infra.tmplt.json \
                          --parameters file://sonar-service-infra.parms.json

                        aws cloudformation wait stack-create-complete --stack-name ${CfnStackRoot}-Infra-${BUILD_NUMBER} --region ${AwsRegion}
                    '''
                }
            }
        }
    }
}
