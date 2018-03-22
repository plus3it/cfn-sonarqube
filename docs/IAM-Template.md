### IAM Role

The [make_sonarqube_IAM-role.tmplt.json](/Templates/make_sonarqube_IAM-role.tmplt.json) file sets up an IAM role. This role is attached to the Sonarqube-hosting EC2 instances. This role:
* Grants access from the EC2 instances to an associated S3 "backups" bucket.
* Allows deployment of EC2 instances via the AutoScaling service within a least-privileges deployment-environment.
* Grants access to a named-bucket/folder containing the Sonarqube plugins (and any other software that may be needed in future iterations.
* the IAM role includes permissions sufficient to make use of AWS's [Systems Manager](https://aws.amazon.com/systems-manager/) service (as a logical future capability).

An example of the resultant IAM policy can be viewed [here](/docs/IAMpolicyExample.md)
