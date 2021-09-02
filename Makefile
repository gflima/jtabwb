.PHONY: all
all:
	@echo 'usage: ${MAKE} [compile|check|clean|package|install]'

.PHONY: check
check:
	nosetests -v ./tests

.PHONY: compile package
compile package:
	make -C java $@
	cp java/target/jtabwb-1.0-jar-with-dependencies.jar ./jtabwb


.PHONY: clean
clean:
	make -C java clean
	-rm -rf ./build ./dist

.PHONY: install
install: package
	pip install .
