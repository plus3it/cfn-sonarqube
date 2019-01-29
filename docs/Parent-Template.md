### Parent Template

The function of this template is to provide an "Easy" button for deploying a Sonarqube service-stack. The provided template invokes the Security Group, S3, IAM, RDS, ELB and standalone-EC2 templates. Upon completion of this template's running, an ELB-fronted Sonarqube service will be up and ready for initial configuration.

This template can also be used to stand up a clone of an existing deployment. To do so, add the ARN of the source-stack's most recent RDS snapshot. This can be one of the daily snapshots - or, probably better, a manual snapshot completed _just_ before launching the parent stack.

*Note:* A wide variety of "parent" templates may be needed to deal with the particulars of a given deployment (e.g., environments where permissions for creating IAM roles are separate from those for creating EC2s). This parent is meant as an example suitable for environments where the template-user has full provisioning-rights within an AWS account.
