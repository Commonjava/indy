This module is used only for sonar report. It is involved only in run-metrics profile.

### What has been done
As indy has a seperate module to do integration test, we need some way to collect all unit tests & ftests report together to apply sonar's reporting requirements. This commit did it like below:

* Appends all unit tests & ftests jacoco exec together in parent/target, then merged into one
* Unpack all source code & unit test source & ftest source together in a separate module
* Collects all unit test report & ftest report in the same module
* Unpack & copy unit test & ftest compiled classes in the same module
* Uses all upper to generate jacoco report
* Uses this module to report to sonar

 
### How to use

* Generate test reports - run "mvn clean install -Prun-its,run-metrics" **in indy parent folder, but not in this savant-report folder**
* Report to sonar - after reports generation, run "mvn sonar:sonar -Psonar-test-report -Dsonar.host.url=${your sonar host}" **in this folder**, or "mvn -f ./embedder-tests/savant-report/pom.xml -Psonar-test-report -Dsonar.host.url=${your sonar host}" **in indy parent folder** 
