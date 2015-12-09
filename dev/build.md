---
title: "Building Indy"
---

### Requirements

* JDK 1.7+
* [Apache Maven](http://maven.apache.org/download.html) 3.0+

### Instructions

Building Indy is a pretty simple task if you've ever used Maven before. Ensure you have both `java` and `mvn` on your `PATH`, then call:

	$ mvn clean install

If you want to run the full complement of functional tests, use the `run-its` profile:

	$ mvn -Prun-its clean install

To run a quick build so you can start poking around with minimal fuss, sometimes it's useful to skip all tests:

	$ mvn -DskipTests=true clean install

**NOTE: If you only build this way and then submit a pull request for your work, it will not endear you to the Indy development community. You've been warned!** 

### Manually Testing a Build

**NOTE:** The following instructions currently require Ruby. In future, these scripts may be ported to Python.

If you want to quickly start an instance of Indy that you just built, you can use the `bin/debug-launcher.rb` test script. To find out how to use this script, try:

	$ ./bin/debug-launcher.rb -h

Normally, unless you need something specific, you can get by with just:

	$ ./bin/debug-launcher.rb

On the other hand, if you want a "live" server against which to test UI changes, you could use the `-u` or `--uidev` option. This option symlinks the `var/lib/indy/ui` directory in the expanded Indy launcher directory to the `layover` UI project sources, in `uis/layover/src/main/js/app`. Any changes you make to those files will be reflected in the running Indy instance.
