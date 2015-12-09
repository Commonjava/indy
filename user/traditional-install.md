---
title: "Traditional Installation"
---

### Requirements

* Java 1.7+
* Bash or some other *nix shell (or, you can port the startup script)

### Getting Started: The Directory Layout

For the quickest possible installation of Indy, check out the [Quickstart Article](quickstart.html).

Indy goes to great lengths to ensure that nothing beyond the tarball is actually required in order to run the application. This means the tarball includes configuration files, template contents in the data directory, and other default files that users are likely to need in order to have a functional system without extra work. If you tried to simply unpack a new version of Indy into an existing installation directory, it is likely that these default files would overwrite customizations you had made previously with the older version. We'll discuss it further below, but all of this means you will have to plan your deployment if you need to preserve application configuration, state, and storage.  To do this, you'll need to know how Indy uses the different directories and files within its distribution tarball.

The Indy tarball tries to prepare the way for installation in LSB-compliant filesystem locations, without actually forcing you to unpack files all over your filesystem if you don't want to. This means that it contains subdirectories roughly equivalent to the LSB locations where files would typically be installed. 

All of the following directories are designed to be moved into a more LSB-compliant layout, their locations configurable in one way or another:

| Tarball Directory       | LSB Equivalent           | Description                                                                                              |
|-------------------------|--------------------------|----------------------------------------------------------------------------------------------------------|
| `etc/indy`             | `/etc/indy`             | Contains application configuration, such as defaults, port/interface to bind to, etc.                    |
| `var/lib/indy/data`    | `/var/lib/indy/data`    | Contains application state for Indy. Repository and group definitions, content templates, etc.          |
| `var/lib/indy/storage` | `/var/lib/indy/storage` | Contains hosted and cached artifact files, with subdirectories corresponding to different repositories.  |
| `var/log/indy`         | `/var/log/indy`         | Log files.                                                                                               |

Finally, Indy includes two more directories that are a bit of a departure from the LSB layout. These directories' locations cannot be configured without modifying the launch script. With the exception of the `boot.properties` file in `bin/` (which we'll cover later), they constitute the part of Indy you would normally want to upgrade (as opposed to directories that contain configuration, state, or storage).

These "special" directories are:

* `bin` - This directory is a mashup of the following LSB locations:
    * `/usr/bin` - the `indy.sh` startup script
    * `/usr/share/indy/` - the sysV and systemd init scripts
    * `/etc/sysconfig` - Not actually contained in the distribution tarball, but referenced from `indy.sh` when it exists, the `boot.properties` file can setup the default command-line options for launching Indy. More on this later.
* `lib` - This directory contains the jars that make up the actual Indy application. Its contents would typically be in `/usr/lib/indy`.

### Planning for Upgrade

As mentioned above, the main concern for upgrading a traditional Indy installation is preserving the files unique to your deployment while upgrading to the new version. What this translates to in practice is moving your configuration, data, and storage directories *outside* the directory structure so you have locations that aren't associated to any given installation directory. To do this, you need to modify two configuration locations (not exactly two files)...and one of them presents something of a challenge to maintain.

#### Indy Configuration

The `etc/indy` directory structure in the Indy tarball comes with several files that are documented in their comments. The main ones to be aware of are:

* `etc/indy/main.conf` - This is the main entry point for Indy application configuration, and it normally contains a line like the following:  

        Include conf.d/*.conf  

  Each of the configuration files in `conf.d/` roughly corresponds to an add-on or subsystem within Indy
* `etc/indy/logging/logback.xml` - While not commented all that well, this file is a standard configuration file for the [logback logging API](http://logback.qos.ch/), and what you would use if you needed to fine-tune the log levels to expose something error you don't understand in the logs.

#### Relocating Data and Storage

To move your data and storage directories out of the installation directory, you'd modify your `etc/indy/main.conf` file to look something like this:

    # passthrough.timeout=300
    # nfc.timeout=300
    
    [ui]
    
    # UI files are stored here, for easy access to allow customization.
    ui.dir=${indy.home}/var/lib/indy/ui
    
    
    [flatfiles]
    
    # This is where configurations and persistent state related to both 
    # the core functions of Indy and its addons are stored.
    data.dir=/var/lib/indy/data
    
    # This is where temporary files used in various calculations for 
    # addon functions are stored.
    work.dir=${indy.home}/var/lib/indy/work
    
    
    [storage-default]
    
    # This is the location where proxied / uploaded / generated repository
    # content is stored. It is distinct from configuration state and other
    # persistent data related to addons.
    storage.dir=/var/lib/indy/storage
    
    
    # [threadpools]
    #
    # This configures the Weft threadpool-injector. It is used to 
    # initialize threadpools with custom names, sizes, and thread 
    # priorities, and inject them via the CDI annotation: @ExecutorConfig
    # (class name is: org.commonjava.cdi.util.weft.ExecutorConfig)
    #
    # defaultThreads=NN # Default for this is calculated by: 
    #                     Runtime.getRuntime().availableProcessors() * 2
    # defaultPriority=8
    # For a custom threadpool called 'mypool' you might configure it 
    # using:
    # mypool.threads=NN
    # mypool.priority=N
    
    
    # Include addon-specific configurations (or really any configuration)
    # from:
    Include conf.d/*.conf

Pay particular attention to the `data.dir` and `storage.dir` configuration elements. Their values normally start with `${indy.home}` just like the `ui.dir` element. Along with this, of course, you'd want to copy the `var/lib/indy/data` and `var/lib/indy/storage` directories to their new LSB locations (referenced by your new configuration).

But wait a second. You've just changed the `etc/indy/main.conf` file. When you unpack a new Indy distribution, it'll overwrite your changes and Indy will lose track of those other data and storage locations!

To prevent this, you should also move your `etc/indy` directory to `/etc/indy`. This just leaves the question of how to reference the new configuration location when you start Indy. 

You could use a command-line option to direct Indy to the new location. Indy offers the following command-line options for just this purpose:

    $ bin/indy.sh --help
    Usage: $0 [OPTIONS] [<target-path>]
    
    
    -C (--context-path) VAL                : Specify a root context path 
                                             for all of indy to use
    -c (--config) VAL                      : Use an alternative 
                                             configuration file (default: 
                                             etc/indy/main.conf in 
                                             <indy-home>)
    -h (--help)                            : Print this and exit
    -i (--interface, --bind, --listen) VAL : Bind to a particular IP 
                                             address (default: 0.0.0.0, 
                                             or all available interfaces)
    -p (--port) N                          : Use different port 
                                             (default: 8080)

If you're using a custom script to launch Indy (eg. a custom script in your `/etc/init.d` directory, or a homebrew systemd service definition), this might be a good option for you. As we'll see below, another option is use the `boot.properties` file.

#### Boot Properties

While not provided by default in the tarball, the `bin/boot.properties` file is something the `bin/indy.sh` script looks for. If it finds this properties file, the script will use it to set the `-Dindy.boot.defaults` system property, which provides a durable alternative to the available Indy command-line options. Each command-line option has a corresponding `boot.properties` entry. If `boot.properties` did exist by default, it would look like this:

    #bind=0.0.0.0
    #port=8080
    #config=${indy.home}/etc/indy/main.conf
    #context-path=/

As you can see, redirecting Indy durably to use a different location for its `main.conf` file is relatively simple. Just add the following file, with the following contents, and you're set:

    $ cat bin/boot.properties
    config=/etc/indy/main.conf

With this file in place, you can start Indy with a simple invocation of its launch script:

    $ bin/indy.sh

While technically a configuration file, and technically not outside of the Indy installation directory, the `boot.properties` file has no default. This means it should never be overwritten by a new install.

#### An Imperfect Approach

On the other hand, simply unpacking a new tarball into an existing Indy installation directory can cause other problems in the `lib/` directory (since the jars are versioned, and an upgraded Indy version will change these jars). It's normally a much better idea to unpack to a versioned directory and move a symlink (eg. `current/`) to reference the new installation directory. It also means you can roll back to an older version if you need to (*and if you've got your configuration and data in version control*). This approach produces a directory structure containing your Indy installations that looks something like this:

    $ ls -l /home/jdcasey/apps/indy
    ... indy-0.18.5
    ... indy-0.19.2
    ... current -> indy-0.19.2

What this means is each time you unpack a new version of Indy, you'll need to copy your `boot.properties` file to the new installation. Alternatively, you could keep `boot.properties` in `/etc/indy`, and symlink it into each Indy installation. Either way, you have a small post-processing step to setup Indy after you've unpacked it. 

If you use a moving `current` symlink for the Indy installation itself, that makes two post-processing steps.

### Versioning Your Data

Since we're talking about preserving your Indy data, you should know that Indy also has a [Revisions add-on](revisions-addon.html) (available in the default `savant` distribution flavor), which knows about using Git to manage the contents of the `data` directory (the place where repository definitions, etc. are stored). You can tell Indy (via its configuration) to clone another Git repository for its data directory if you have a remote Git repository you want to use, or you can simply let Indy initialize a local Git repository. If you allow Indy to initialize its own local Git repository, you will have the option later to push it to a remote location and get an instant, versioned backup of your data directory.

Once enabled, the Revisions add-on will store each modification to a store definition, each change to a content template, changes to almost anything that gets stored in the `var/lib/indy/data` directory, to Git. You can even tell it to push any changes to a remote Git repository using the same configuration!

