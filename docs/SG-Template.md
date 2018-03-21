### Security Groups

The [make_sonarqube_SGs.tmplt.json](/Templates/make_sonarqube_SGs.tmplt.json) file sets up the security group used to gate network-access to the Sonarqube elements. The Sonarqube design assumes that the entirety of the Sonarqube-deployment exists within an isolated security-silo. This silo contains only the Sonarqube-service elements. The security-groups created by this template are designed to foster communication between service-elements while allowing network-ingress and -egress to the silo _only_ through the Internet-facing load-balancer.
