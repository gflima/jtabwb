.PHONY: all
all:
	@echo 'usage: ${MAKE} [check|clean|install|package]'

.PHONY: check
check:
	nosetests -v ./tests

.PHONY: clean
clean:
	make -C java clean
	-rm -rf ./build ./dist

.PHONY: install
install: package
	pip install .

.PHONY: package
package:
	make -C java $@
	cp java/target/jtabwb-1.0-jar-with-dependencies.jar ./jtabwb
