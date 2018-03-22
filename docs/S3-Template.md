### S3 Bucket

The Sonarqube service EC2s typically need access to two buckets: a bucket that hosts the installation-automation scripts and software and a bucket for backups. The [make_sonarqube_S3-backup_bucket.tmplt.json](/Templates/make_sonarqube_S3-backup_bucket.tmplt.json) template _only_ takes care of setting up the bucket for backup-activities. The outputs from this template are used by the IAM Role template to create the requisite S3 bucket access-rules in the resultant IAM policy document.

It is assumed that the bucket hosting toolchain-related software, scripts and other "miscellaneous" data will exist outside of the Sonarqube deployment-silo.
