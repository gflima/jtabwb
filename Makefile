.PHONY: all
all:
	@echo 'usage: ${MAKE} [compile|clean|package|install]'

.PHONY: compile
compile:
	mvn compile

.PHONY: package
package:
	mvn package

.PHONY: clean
clean:
	mvn clean

.PHONY: install
install:
	mvn install

.PHONY: update-local-maven-repo
update-local-maven-repo:
	mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file\
	  -Dfile=lib/ferram-util-1.0.jar\
	  -DgroupId=ferram\
	  -DartifactId=ferram-util\
	  -Dversion=1.0\
	  -Dpackaging=jar\
	  -DlocalRepositoryPath=local-maven-repo
