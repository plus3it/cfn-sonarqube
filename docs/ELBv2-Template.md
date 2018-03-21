### Application Elastic LoadBalancer

All of the Sonarqube EC2 instances launched by this project should be deployed into a VPC's private subnets. The Elastic LoadBalancer &mdash; created by the [make_sonarqube_ELBv2.tmplt.json](/Templates/make_sonarqube_ELBv2.tmplt.json) template &mdash; provides the public-facing ingress-/egress-point to the Sonarqube service-deployment. This ELB provides the bare-minimum transit services required for the Sonarqube web service to be usable from client requests arriving via the public Internet.
