# Sonarqube

The Sonarqube project is a sub-project of the overarching DevOps Tool-Chain (DOTC) project. This project — and its peer projects — is designed to handle the automated deployment of common DevOps tool-chain services onto STIG-harderend, EL7-compatible Amazon EC2 instances and related AWS resources. Specifically, this project provides CloudFormation (CFn) templates for:

* [Security Groups](docs/SG-Template.md)
* Simple Storage Service [(S3) Bucket](docs/S3-Template.md)
* [Relational Database Service](docs/RDS-Template.md) (RDS)
* Identity and Access Management [(IAM) role](docs/IAM-Template.md)
* Application LoadBalancer (a.k.a., ["ELBv2"](docs/ELBv2-Template.md))
* [Standalone](docs/EC2-standalone-Template.md)  EC2 instance
* "One-button" [master deployment](docs/Parent-Template.md)

Additionally, automation-scripts are provided to automate the installation and configuration of the Sonarqube application within the Template-deployed EC2 instances.

## Design Assumption

These templates are intended for use within AWS [VPCs](https://aws.amazon.com/vpc/). It is further expected that the deployed-to VPCs will be configured with public and private subnets. All Sonarqube elements other than the Elastic LoadBalancer(s) are expected to be deployed into private subnets. The Elastic LoadBalancers provide transit of Internet-originating web UI requests to the the Sonarqube node's web-based management-interface.

These templates _may_ work outside of such a networking-topology but have not been so tested.

## Notes on Templates' Usage

It is generally expected that the use of the various, individual-service templates will be run via the "parent" template. The "parent" template allows for a kind of "one-button" deployment method where all the template-user needs to worry about is populating the template's fields and ensuring that CFn can find the child templates.

In order to use the "parent" template, it is recommended that the child templates be hosted in an S3 bucket separate from the one created for backups by this stack-set. The template-hosting bucket may be public or not. The files may be set to public or not. CFn typically has sufficient privileges to read the templates from a bucket without requiring the containing bucket or files being set public. Use of S3 for hosting eliminates the need to find other hosting-locations or sort out access-particulars of those hosting locations.

The EC2-related templates currently require that the scripts be anonymously `curl`-able. The scripts can still be hosted in a non-public S3 bucket, but the scripts' file-ACLs will need to allow `public-read`. This may change in future releases &mdash; likely via an enhancement to the IAM template.

The EC2 scripts will also pull down Sonarqube plugins from an S3 bucket. This bucket may be set to public-read or not. Similarly, the plugin files may be set to public-read or not. The templates create an IAM instance-role that is attached to the EC2 instance. This instance-role will provide adequate access from the created EC2-instance to a named-bucket in order for an `s3 sync` operation to be performed.

Note that the immediately-preceding means that the design-expectation is that any new plugins will be pushed into the named-bucket and a redeployment of the EC2 instance will result in the plugins being installed. Any plugin configuration will have to be performed by the operator once the new instance has deployed. Plugin configuration-actions are typically persisted via the stack-sets'RDS-hosted PGSQL database.

These templates do _not_ include Route53 functionality. It is assumed that the requisite Route53 or other DNS alias will be configured separate from the instantiation of the public-facing ELB.
