MVN= mvn -q

.PHONY: all
all:
	@echo 'usage: ${MAKE} [compile|clean|install|package]'

.PHONY: compile clean install package
compile clean install package:
	${MVN} $@

.PHONY: update-local-maven-repo
update-local-maven-repo:
	${MVN} org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file\
	  -Dfile=lib/ferram-util-1.0.jar\
	  -DgroupId=ferram\
	  -DartifactId=ferram-util\
	  -Dversion=1.0\
	  -Dpackaging=jar\
	  -DlocalRepositoryPath=local-maven-repo
