# Log Analysis Tools for Indy

This is a set of tools for analyzing Indy log files.

## Installing

To install, you may want to use a virtualenv:

```
    $ virtualenv ./venv
    $ source ./venv/bin/activate
```

Then, you can install in "developer mode" using:

```
    $ pip install -e .
```

## Commands

The following commands will be installed:

* `indy-log-timer` - Analyze a set of indy.\*.log files looking for start/end matches, according to a timer config YAML file
* `indy-log-timer-sample` - Print out a sample timer config YAML file for doing start/end time analysis

 