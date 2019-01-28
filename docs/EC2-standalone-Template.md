### Standalone Sonarqube Instance(s)

The [make_sonarqube_EC2-instance.tmplt.json](/Templates/make_sonarqube_EC2-instance.tmplt.json) template &mdash; along with deployment-automation helper-scripts &mdash; creates an EC2 instance that hosts the Java-based Sonarqube web service. The resultant EC2 instance contains a fully-configured Sonarqube service. The service will have appropriate connector-definitions for:

* Working with an external, PGSQL-based configuration-database
* Working behind an SSL-terminating, Internet-facing HTTP proxy

As well as:

* Daily cron job to push backups to an associated S3 bucket
* LDAP connector configuration for authentication offload
* Installation of all function-extending plugins that have been staged to a named S3 bucket/folder.
