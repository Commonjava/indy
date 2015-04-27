---
title: "Docker Deployment"
---

### Requirements

* Linux host with a modern version of Docker installed
* Some knowledge of Docker container management (or [a willingness to learn](http://docs.docker.com/userguide/))

### Contents

* [Getting Started](#getting-started)
* [Persistent Deployment](#persistent-deployment)
* [Advanced Deployment Options](#advanced-options)
* [Upgrading Your AProx Deployment](#upgrading-your-aprox-deployment)
* [Auto-Deployment](#autodeploy)

### Getting Started
<a name="getting-started></a>

If you have a Linux machine with Docker installed, the quickest way to try AProx is to instantiate a container from the [buildchimp/aprox](https://registry.hub.docker.com/u/buildchimp/aprox/) image. This image has a **lot** of available options, as we'll explore below. However, you can try a simple deployment by issuing a single command:

    $ sudo docker run -p 8081:8081 -ti --rm \
         --name=aprox buildchimp/aprox

If you're on a RHEL 7 server, you might have more luck prefixing the image name with `docker.io`:

    $ sudo docker run -p 8081:8081 -ti --rm \
         --name=aprox docker.io/buildchimp/aprox

You should be able to see the log output on screen. When the server says it's listening on port 8081, you should be able to browse to [http://localhost:8081/](http://localhost:8081/).

When you're finished, simply use **CTL-C** to stop the container.

#### I'm New to Docker...What Did I Just Do??

The above Docker command has several parts, some of which are optional or even detrimental to running the server more permanently. Let's explore each part of the above command in order.

    sudo docker

On most systems, the default Docker installation will only allow the `root` user to control containers. So, we're using `sudo` to run the Docker command as `root`. In enterprise deployments, this might not be the preferred way of managing Docker containers.

    run ... buildchimp/aprox

or, on RHEL 7:

    run ... [docker.io/]buildchimp/aprox

This Docker sub-command simply resolves the given image from the Docker registry, downloading the associated filesystem archives, and sets up a new running container based on it. This part is not optional.

    -p 8081:8081

This option exposes the port `8081` on the host system (given by the **first** segment of the option value) and maps it to the port `8081` that AProx listens on inside the Docker container (the **second** part of the option value). This makes AProx browseable on [http://localhost:8081/](http://localhost:8081). 

For another example, you could use the following option instead:

    -p 80:8081

This will expose port `80` on your host system and map that to the Docker AProx port `8081`.

    -ti

This is actually two options crammed together: one to allocate a pseudo-TTY so you can see the console log, and one to attach STDIN to the running container. Attaching STDIN allows you to stop the container by typing **CTL-C**. If you specified only `-t`, typing **CTL-C** would detach from the container's log without actually stopping it. In that scenario, to stop the container you'd have to issue the command:

    $ sudo docker stop aprox

There are two more options in the command line above:

    --rm

This tells Docker to remove the container when it stops. It makes our little experiment with AProx clean up after itself when you stop it.

    --name=aprox

This tells Docker to name the container `aprox` locally (this is distinct from the *image* name `buildchimp/aprox`). The container name is useful for starting or stopping it once the `run` command has been issued.

#### How Do I Make the AProx Container Permanent?

That's even simpler:

    $ sudo docker run -p 8081:8081 -t \
         --name=aprox buildchimp/aprox

Again, if you're on a RHEL 7 server, you might have more luck prefixing the image name with `docker.io`:

    $ sudo docker run -p 8081:8081 -t \
         --name=aprox docker.io/buildchimp/aprox

### Persistent Deployment
<a name="persistent-deployment"></a>

It's easy to setup a Docker container using the `run` command directly, but how do you setup a persistent service that will survive reboot?

To boot your AProx instance when the host boots, you can select from a few options. For sysV hosts that use the traditional `/etc/inittab` file and `/etc/init.d` directory, you can either write a script to put in `/etc/init.d` (an exercise left to the reader), or you can add something like the following command to `/etc/inittab`:

    docker start aprox

If you have a host with systemd on it, you can use the scripts and service definitions in an associated project, [aprox-docker](https://github.com/Commonjava/aprox-docker/) to setup your AProx container. This project has the added benefit of containing scripts you can use to autodeploy new AProx versions using one of a few different methods.

#### AProx Docker Utilities

The [aprox-docker](https://github.com/Commonjava/aprox-docker/) project contains both the Dockerfile definitions used to build the two available AProx images (`aprox` and `aprox-volumes`), and the scripts and service definitions that make it easier to manage containers based on these Docker images. We'll worry about the `aprox-volumes` image [later](#aprox-volumes). For now, we only need the `aprox` image.

It's usually easiest to just `git clone` the latest release of aprox-docker. These scripts are available for [download as a tarball](http://repo.maven.apache.org/maven2/org/commonjava/aprox/docker/aprox-docker-utils/), of course, but by cloning the `latest` Git branch you gain the ability to update your copy whenever a new version is released. Even more attractive, if you need to tweak any of the scripts, you have the option to commit your changes and even maintain your own fork if you want to. 

To clone a local copy of the aprox-docker scripts project, run the following:

    $ git clone -b latest https://github.com/Commonjava/aprox-docker.git

You should end up with the following project directory:

    $ tree aprox-docker/
    aprox-docker/
    ├── aprox-server
    │   ├── Dockerfile
    │   ├── README.md
    │   └── start-aprox.py
    ├── aprox-volumes
    │   ├── Dockerfile
    │   ├── README.md
    │   └── start-volumes.py
    ├── aprox.py
    ├── autodeploy-file.py
    ├── autodeploy-url.py
    ├── init-aprox-server-no-vols.py
    ├── init-aprox-server.py
    ├── init-aprox-volumes.py
    ├── pom.xml
    ├── README.md
    ├── src
    │   └── main
    │       └── assembly
    │           └── utils.xml
    ├── systemd
    │   ├── aprox-server-novols.service
    │   ├── aprox-server.service
    │   └── aprox-volumes.service
    └── utils
        └── json-get.py

#### Initializing AProx with Scripts

To use the aprox-docker scripts to initialize your AProx container, you can run:

    $ ./init-aprox-server-no-vols.py

Used with no additional arguments, this script will use some sensible defaults and start an AProx container that works in standalone mode (no mounted Docker [volumes](https://docs.docker.com/userguide/dockervolumes/), but the script itself offers many options that we'll discuss below in [Advanced Deployment Options](#advanced-options).

#### Surviving Reboot with Systemd

If your host uses systemd, you can setup your AProx server container to start when the host boots, and to restart if the AProx container ever dies. The aprox-docker project contains a directory called `systemd` which is full of `.service` scripts for this purpose. Each service definition contains instructions in the form of embedded comments for installing. However, let's look at how you would define a systemd service for the AProx container you setup above:

    $ sudo docker stop aprox
    $ sudo cp ./systemd/aprox-no-vols.service /etc/systemd/system
    $ sudo systemctl enable aprox-no-vols
    $ sudo systemctl start aprox-no-vols && journalctl -f

The first command simply stops the AProx container (mainly so we can see the service start later). The second copies the service definition to the systemd directory structure. The third command links it into the appropriate place for systemd to manage the service, and ensures it will start when the host boots. Finally, the last command manually starts the `aprox-no-vols` service, and immediately follows the output of the `journalctl` command, which shows the system logs. Your AProx bootup log entries should scroll past while you watch the service start. 

Hitting **CTL-C** will allow you to stop following the system log.

### Advanced Deployment Options
<a name="advanced-options"></a>

Remember that `init-aprox-server-no-vols.py` script we used above? If we run that with `--help`, this is the output we'll see:

    $ ./init-aprox-server-no-vols.py --help
    Usage: init-aprox-server-no-vols.py [options]
    init-aprox-server-no-vols.py [options] - [aprox options]

    Options:
      -h, --help            show this help message and exit
      -d DEVDIR, --devdir=DEVDIR
                            Directory to mount for devmode deployment
                            (default: disabled, to use released version 
                            from URL)
      -D DEBUG_PORT, --debug-port=DEBUG_PORT
                            Port on which AProx JPDA connector should 
                            listen (default: disabled)
      -E ETC_URL, --etc-url=ETC_URL
                            URL from which to git-clone the etc/aprox 
                            directory (default: disabled)
      -F FLAVOR, --flavor=FLAVOR
                            The flavor of AProx binary to deploy 
                            (default: savant)
      -i IMAGE, --image=IMAGE
                            The image to use when deploying (default:
                            buildchimp/aprox)
      -n NAME, --name=NAME  The container name under which to deploy 
                            AProx (default: aprox)
      -p PORT, --port=PORT  Port on which AProx should listen 
                            (default: 8081)
      -q, --quiet           Don't start with TTY
      -S SSHDIR, --sshdir=SSHDIR
                            Directory to mount for use as .ssh directory 
                            by AProx (default: disabled)
      -U URL, --url=URL     URL from which to download AProx (default is
                            calculated, using 'savant' flavor)
      -V VERSION, --version=VERSION
                            The version of AProx to deploy 
                            (default: 0.19.2)
      --config=CONFIG       Volume mount for 'etc/aprox' configuration 
                            directory
      --data=DATA           Volume mount for state data files
      --logs=LOGS           Volume mount for logs
      --storage=STORAGE     Volume mount for artifact storage

As you see, it offers a lot of potential for customizing our AProx deployment. We can customize several simple things, like selecting which port the AProx server is mapped to, or exposing a debug port for JPDA debugging. We can specify a particular AProx version, distribution flavor, or even a URL to the distribution binary we want to deploy.

We can do things having deeper effects, yet which are still essentially simple, like customizing which Docker image and local container names to use (but please use caution if you customize these, since the defaults are used in other scripts for things like autodeployment).

Or, to explore the rabbit hole a little deeper still, read on.

#### Host-Side Storage

One maintenance task you're probably not thinking about yet is upgrading AProx. As with many such tasks, it pays to plan ahead to make sure your AProx deployment can be upgraded without data loss.

Ordinarily, upgrading an application is a matter of upgrading an operating system package, or maybe unpacking a tarball into a particular directory and restarting. The Docker approach makes this a little more complicated, since the Docker deployment unit is a whole container. Upgrading via Docker usually involves pulling a new Docker image and creating a new container based on it. This can be attractive because you don't have to worry about establishing and following your own filesystem practices for persistent configuration and data. If you're used to using packages like RPMs it can still be attractive because you don't need to figure out how to adapt if you have to deploy an instance on a system that doesn't support RPM. 

But for all the convenience, upgrading via Docker usually inspires one big question: How do I preserve my data? In terms of AProx itself, this includes both hosted artifacts and instance state (repository definitions, autoprox rules, content templates, and the like). The standard approach documented in the Docker community is to use a volume container, since it provides a portable way to define storage that's separated from the container running the code itself. To take this approach with AProx, read [below](#aprox-volumes).

In simpler environments, you can store persistent data directly on the host system. This can be attractive in its own right, since you can back those storage locations with networked filesystems, RAID arrays, or whatever you want. If you want to use this strategy, you'll want to look again at the initialization script above. Specifically, look at these options:

      --config=CONFIG       Volume mount for 'etc/aprox' configuration 
                            directory
      --data=DATA           Volume mount for state data files
      --logs=LOGS           Volume mount for logs
      --storage=STORAGE     Volume mount for artifact storage

If we want to adhere to standards, we might try something like this:

    $ mkdir -p /var/lib/aprox/{data,storage}
    $ mkdir -p /var/log/aprox
    $ mkdir -p /etc/aprox
    $ ./init-aprox-server-no-vols.py \
         --config=/etc/aprox \
         --data=/var/lib/aprox/data \
         --logs=/var/log/aprox \
         --storage=/var/lib/aprox/storage

Or, if we're using a networked filesystem mount, we might prefer this:

    $ mkdir -p /mnt/aprox/{data,storage}
    $ mkdir -p /mnt/aprox/logs
    $ mkdir -p /mnt/aprox/etc
    $ ./init-aprox-server-no-vols.py \
         --config=/mnt/aprox/etc \
         --data=/mnt/aprox/data \
         --logs=/mnt/aprox/logs \
         --storage=/mnt/aprox/storage

The initialization command will add Docker volume mounts for each of these, so AProx has access to thim. When the AProx server container boots, it will expand the appropriate files from the distribution binary into these locations. Later, if we need to upgrade AProx, we can have confidence that these files won't be erased when the old container is replaced.

#### SSH, Git, and the `etc` URL

One core practice of the DevOps movement is keeping revision history on configurations as well as code and management scripts. Without a version history for your application's configuration, how will you recover to a known-good state if someone changes your application's configuration and something goes wrong? This is a very common problem in complex server deployments. Not having a revision history can wreck your rollback process and leave services dead in the water while you scramble to guess at what was in that file before, when everything was working.

Okay, so you've decided to put your AProx etc directory in version control. Great. Now, how do you get those versioned files into your running Docker container? If you're using host-side storage with Docker volumes, you may have many options for initializing your AProx configuration. On the other hand, to ensure you're using a configuration that hasn't drifted through manual editing, it might be better **not** to mount that etc/aprox volume, and instead clone the configuration directly from your Git repository. So, let's take another look at that `init-aprox-server-no-vols.py` script's help output:

      -E ETC_URL, --etc-url=ETC_URL
                            URL from which to git-clone the etc/aprox 
                            directory (default: disabled)
      ...
      -S SSHDIR, --sshdir=SSHDIR
                            Directory to mount for use as .ssh directory 
                            by AProx (default: disabled)

The `-E` option allows you to pass in the URL for your AProx etc directory's Git repository:

    $ ./init-aprox-server-no-vols.py \
         -E https://github.com/jdcasey/aprox-etc.git

Or, perhaps you want to keep that configuration someplace more private? Say, someplace you need SSH to clone from? No problem, just setup a normal `.ssh` directory structure containing:

    $ls -1 $HOME/aprox-ssh/
      known_hosts
      id_rsa

Then, use the option to add your new SSH configuration directory *and* the etc URL to the initialization script:

    $ ./init-aprox-server-no-vols.py \
         -E ssh://secretsauce.myco.com/git/aprox-etc.git \
         -S $HOME/aprox-ssh

Before the AProx container boots, the initialization script will call the SELinux command `chcon -Rt svirt_sandbox_file_t $HOME/aprox-ssh` to allow Docker access to the aprox-ssh directory, then create the container with a mounted volume for the SSH configuration. When the container boots, it will copy the mounted SSH config volume to `/root/.ssh` to make it available for Git commands to use.

AProx itself doesn't know about using Git to manage its `etc` directory; managing that directory is considered out of scope for AProx. However, AProx does have an add-on (available in the default `savant` distribution flavor), which knows about using Git to manage the contents of the `data` directory (the place where repository definitions, etc. are stored). If you use the above method to clone your AProx configuration from Git, you can also tell AProx (via its configuration) to clone another Git repository for its data directory. You can even tell it to push any changes to a remote Git repository using the same configuration!

The magic file is `etc/aprox/conf.d/revisions.conf`, and you can enable a remote Git data repository by making it to look something like this:

    [revisions]
    # 'push.enabled' determines whether changes get pushed back to the 
    # origin git repository.
    # Values: true|false
    push.enabled=true
    
    # 'conflict.action' determines what to do with changes that conflict 
    # with local configuration files.
    # Values:
    #   * merge - attempt a git merge
    #   * overwrite - keep the copy from the origin repository
    #   * keep - keep the local copy
    #
    conflict.action=keep
    
    # 'data.upstream.url' determines the origin-repository URL for 
    # cloning/pulling and pushing changes.
    #
    data.upstream.url=ssh://secretsauce.myco.com/git/aprox-data.git

One thing to note about managing AProx configuration and data directories via Git:

    Once you take over for the defaults provided in the AProx distribution, you're in charge of merging in new configurations, content templates, and so forth for any version upgrades.

These configuration changes should be minimal since we go to great lengths to give AProx sensible default configuration values, and to make things like configuration backward compatible.

Still, it's worth remembering.

### Using a Volume Container
<a name="aprox-volumes"></a>

If you need portable storage with the ability to upgrade AProx without losing your data and artifacts, you need a volume container. Using a volume container for AProx isn't hard; we've created a purpose-built Docker image for it, along with scripts in aprox-docker to support it. Basically, there are just a few differences from the instructions given above:

* You have to initialize and start the `aprox-volumes` container before starting the `aprox` server container. You can initialize this container with the following command:

    $ ./init-aprox-volumes

  For the most part, you shouldn't really need to customize this command.

* You have to use the `init-aprox-server` script in place of the `init-aprox-server-no-vols` script for all of the commands given above. For obvious reasons, host-side storage won't work with this script, so those options aren't available.

* You have to use two systemd service definitions instead of one. Instead of `aprox-server-novols.service`, you'll copy/enable/start `aprox-volumes.service` **then** `aprox-server.service`. (The process for installing/starting these services is the same as given above.)

### Upgrading Your AProx Deployment
<a name="upgrading-your-aprox-deployment"></a>

Once you've accounted for the data and artifact storage for things you want to keep, upgrading is a simple matter of stopping the old container and starting a new one:

    $ sudo docker stop aprox
    $ sudo docker rm aprox.last
    $ sudo docker rename aprox aprox.last
    $ ./init-aprox-server-no-vols.py <your-custom-options> \
         -V <new-version>

...or, with a volume container in place:

    $ ./init-aprox-server.py <your-custom-options> \
         -V <new-version>

Note that we're keeping the old AProx container around (renamed to `aprox.last`) just in case we need to rollback.

Also note that if you're using a Docker volume container, you don't need to do anything with that container. Your data and artifacts are stored there; you don't want to touch it during an upgrade!

### Auto-Deployment
<a name="autodeploy"></a>

#### A file appears

#### Or, keep an eye on that Maven repository
