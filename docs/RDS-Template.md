### RDS Externalized Database

Sonarqube uses a database to host configuration, tracking and other, non-BLOB data. Sonarqube supports a couple of different database back-ends. This stack-set makes use of PGSQL. The [make_sonarqube_RDS.tmplt.json](/Templates/make_sonarqube_RDS.tmplt.json) template deploys a small, multi-AZ database to provide for Sonarqube's database needs. Being externalized, loss of an EC2 instance does not result in the loss of database contents. Leveraging AWS's [RDS](https://aws.amazon.com/rds/) also means that backups and version upgrades are handled by the CSP. Using a multi-AZ design means that, even if an AZ becomes unavailable, the Sonarqube-hosting EC2 can still contact its database (or can be re-deployed to an AZ hat still has RDS connectivity).

This template can also be used to stand up a clone of an existing RDS-deployment. To do so, add the ARN of the source-stack's most recent RDS snapshot. This can be one of the daily snapshots - or, probably better, a manual snapshot completed _just_ before launching the parent stack.

Note: Whether the resultan RDS configuration is multi-AZ or not depends on the options selected by the template-user. Selecting the single-AZ option lowers accumulated AWS service charges but does so at the expense of design-resiliency.
