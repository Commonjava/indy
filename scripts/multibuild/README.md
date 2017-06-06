# Multibuild package for Maven Projects

This setup is intended to run multiple builds against a specific Indy instance.

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

## Test Setup

To run a test, the user must capture the build parameters in a YAML file. This file should be
in its own directory, since the output / logs / other data related to the build execution
will also be captured there. This allows the user to keep track of the circumstances under which
a particular type of build was run, and the result of that test.

The basic structure is:

```
    ./my-test-build
    +- test.yaml
```

## YAML Format

You can find a sample of the build specification in `sample-testfile.yaml`. The build specification allows
the user to specify the following:

* HTTProx proxy port (if used)
* section for build, containing:
  * number of build threads
  * number of total builds
  * project directory to use as a location for cloning build sources
* section for report verification, containing:
  * number of threads for verifying folo tracking reports
* a vagrant section, containing test initialization parameters for the VMs, with the following sections:
  * pre-build
  * post-build
  * pre-report
  * post-report

### Initializing Vagrant

Each of the vagrant sub-sections outlined above can contain a `copy` section and a `run` section. The copy section
looks like this:

```
    copy:
      - <src>: <dest>
      ...
```

The run section is a little more involved:

```
    run:
      -
        host: nfs
        commands:
          - 'sudo ls -alh /tmp'
          ...
      -
        host: indy
        commands:
          - 'sudo do-something-cool'
        wait-for-indy: true
      ...
```

This section allows you to specify a script of actions and optionally wait for Indy to start responding again 
(in case you restart it).


## Building a Test Project

The user must provide the following arguments to the `multibuild` command:

* TESTFILE - A path to the YAML file, which is outlined above. This file's directory will collect the build results
* INDY_URL - An Indy URL, with everything setup to work with the `public` group


## Using with the Vagrant VMs

To use this with the Vagrant configuration contained in this project, run the VMs. You can use the instructions in
the root README to fine-tune these VMs for your test.

Use `vagrant ssh-config indy` in the root directory of this repository to get the host IP address for the Indy
instance. Once you have this, you can use `http://<IP>:8080` as the Indy URL for input into the multibuild script.

## Example

Starting from the project root directory (directory above this one):

```
    $ vagrant up
    $ export INDY=$(vagrant ssh-config indy | grep HostName | awk '{print $2}')

    [CONNECT TO INDY UI AT http://${INDY}:8080 and configure it appropriately]

    $ cd multibuild
    $ git clone https://github.com/Commonjava/indy project
    $ multibuild my-test/test.yaml http://${INDY}:8080

    [BUILDS RUN]
```

