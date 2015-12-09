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
  * [Host-Side Storage](#host-side-storage)
  * [SSH, Git, and the `etc` URL](#ssh-git-etc)
  * [Dev Mode](#devmode)
* [Using a Volume Container](#indy-volumes)
* [Upgrading Your Indy Deployment](#upgrading-your-indy-deployment)
* [Auto-Deployment](#autodeploy)

### Getting Started
<a name="getting-started"></a>

If you have a Linux machine with Docker installed, the quickest way to try Indy is to instantiate a container from the [commonjava/indy](https://registry.hub.docker.com/u/commonjava/indy/) image. This image has a **lot** of available options, as we'll explore below. However, you can try a simple deployment by issuing a single command:

    $ sudo docker run -p 8081:8081 -ti --rm \
         --name=indy commonjava/indy

If you're on a RHEL 7 server, you might have more luck prefixing the image name with `docker.io`:

    $ sudo docker run -p 8081:8081 -ti --rm \
         --name=indy docker.io/commonjava/indy

You should be able to see the log output on screen. When the server says it's listening on port 8081, you should be able to browse to [http://localhost:8081/](http://localhost:8081/).

When you're finished, simply use **CTL-C** to stop the container.

#### I'm New to Docker...What Did I Just Do??

The above Docker command has several parts, some of which are optional or even detrimental to running the server more permanently. Let's explore each part of the above command in order.

    sudo docker

On most systems, the default Docker installation will only allow the `root` user to control containers. So, we're using `sudo` to run the Docker command as `root`. In enterprise deployments, this might not be the preferred way of managing Docker containers.

    run ... commonjava/indy

or, on RHEL 7:

    run ... [docker.io/]commonjava/indy

This Docker sub-command simply resolves the given image from the Docker registry, downloading the associated filesystem archives, and sets up a new running container based on it. This part is not optional.

    -p 8081:8081

This option exposes the port `8081` on the host system (given by the **first** segment of the option value) and maps it to the port `8081` that Indy listens on inside the Docker container (the **second** part of the option value). This makes Indy browseable on [http://localhost:8081/](http://localhost:8081). 

For another example, you could use the following option instead:

    -p 80:8081

This will expose port `80` on your host system and map that to the Docker Indy port `8081`.

    -ti

This is actually two options crammed together: one to allocate a pseudo-TTY so you can see the console log, and one to attach STDIN to the running container. Attaching STDIN allows you to stop the container by typing **CTL-C**. If you specified only `-t`, typing **CTL-C** would detach from the container's log without actually stopping it. In that scenario, to stop the container you'd have to issue the command:

    $ sudo docker stop indy

There are two more options in the command line above:

    --rm

This tells Docker to remove the container when it stops. It makes our little experiment with Indy clean up after itself when you stop it.

    --name=indy

This tells Docker to name the container `indy` locally (this is distinct from the *image* name `commonjava/indy`). The container name is useful for starting or stopping it once the `run` command has been issued.

#### How Do I Make the Indy Container Permanent?

That's even simpler:

    $ sudo docker run -p 8081:8081 -t \
         --name=indy commonjava/indy

Again, if you're on a RHEL 7 server, you might have more luck prefixing the image name with `docker.io`:

    $ sudo docker run -p 8081:8081 -t \
         --name=indy docker.io/commonjava/indy

### Persistent Deployment
<a name="persistent-deployment"></a>

It's easy to setup a Docker container using the `run` command directly, but how do you setup a persistent service that will survive reboot?

To boot your Indy instance when the host boots, you can select from a few options. For sysV hosts that use the traditional `/etc/inittab` file and `/etc/init.d` directory, you can either write a script to put in `/etc/init.d` (an exercise left to the reader), or you can add something like the following command to `/etc/inittab`:

    docker start indy

If you have a host with systemd on it, you can use the scripts and service definitions in an associated project, [indy-docker](https://github.com/Commonjava/indy-docker/) to setup your Indy container. This project has the added benefit of containing scripts you can use to autodeploy new Indy versions using one of a few different methods.

#### Indy Docker Utilities

The [indy-docker](https://github.com/Commonjava/indy-docker/) project contains both the Dockerfile definitions used to build the two available Indy images (`indy` and `indy-volumes`), and the scripts and service definitions that make it easier to manage containers based on these Docker images. We'll worry about the `indy-volumes` image [later](#indy-volumes). For now, we only need the `indy` image.

It's usually easiest to just `git clone` the latest release of indy-docker. These scripts are available for [download as a tarball](http://repo.maven.apache.org/maven2/org/commonjava/indy/docker/indy-docker-utils/), of course, but by cloning the `latest` Git branch you gain the ability to update your copy whenever a new version is released. Even more attractive, if you need to tweak any of the scripts, you have the option to commit your changes and even maintain your own fork if you want to. 

To clone a local copy of the indy-docker scripts project, run the following:

    $ git clone -b latest https://github.com/Commonjava/indy-docker.git

You should end up with the following project directory:

    $ tree indy-docker/
    indy-docker/
    ├── indy-server
    │   ├── Dockerfile
    │   ├── README.md
    │   └── start-indy.py
    ├── indy-volumes
    │   ├── Dockerfile
    │   ├── README.md
    │   └── start-volumes.py
    ├── indy.py
    ├── autodeploy-file.py
    ├── autodeploy-url.py
    ├── init-indy-server-no-vols.py
    ├── init-indy-server.py
    ├── init-indy-volumes.py
    ├── pom.xml
    ├── README.md
    ├── src
    │   └── main
    │       └── assembly
    │           └── utils.xml
    ├── systemd
    │   ├── indy-server-novols.service
    │   ├── indy-server.service
    │   └── indy-volumes.service
    └── utils
        └── json-get.py

#### Initializing Indy with Scripts

To use the indy-docker scripts to initialize your Indy container, you can run:

    $ ./init-indy-server-no-vols.py

Used with no additional arguments, this script will use some sensible defaults and start an Indy container that works in standalone mode (no mounted Docker [volumes](https://docs.docker.com/userguide/dockervolumes/), but the script itself offers many options that we'll discuss below in [Advanced Deployment Options](#advanced-options).

#### Surviving Reboot with Systemd

If your host uses systemd, you can setup your Indy server container to start when the host boots, and to restart if the Indy container ever dies. The indy-docker project contains a directory called `systemd` which is full of `.service` scripts for this purpose. Each service definition contains instructions in the form of embedded comments for installing. However, let's look at how you would define a systemd service for the Indy container you setup above:

    $ sudo docker stop indy
    $ sudo cp ./systemd/indy-no-vols.service /etc/systemd/system
    $ sudo systemctl enable indy-no-vols
    $ sudo systemctl start indy-no-vols && journalctl -f

The first command simply stops the Indy container (mainly so we can see the service start later). The second copies the service definition to the systemd directory structure. The third command links it into the appropriate place for systemd to manage the service, and ensures it will start when the host boots. Finally, the last command manually starts the `indy-no-vols` service, and immediately follows the output of the `journalctl` command, which shows the system logs. Your Indy bootup log entries should scroll past while you watch the service start. 

Hitting **CTL-C** will allow you to stop following the system log.

### Advanced Deployment Options
<a name="advanced-options"></a>

Remember that `init-indy-server-no-vols.py` script we used above? If we run that with `--help`, this is the output we'll see:

    $ ./init-indy-server-no-vols.py --help
    Usage: init-indy-server-no-vols.py [options]
    init-indy-server-no-vols.py [options] - [indy options]

    Options:
      -h, --help            show this help message and exit
      -d DEVDIR, --devdir=DEVDIR
                            Directory to mount for devmode deployment
                            (default: disabled, to use released version 
                            from URL)
      -D DEBUG_PORT, --debug-port=DEBUG_PORT
                            Port on which Indy JPDA connector should 
                            listen (default: disabled)
      -E ETC_URL, --etc-url=ETC_URL
                            URL from which to git-clone the etc/indy 
                            directory (default: disabled)
      -F FLAVOR, --flavor=FLAVOR
                            The flavor of Indy binary to deploy 
                            (default: savant)
      -i IMAGE, --image=IMAGE
                            The image to use when deploying (default:
                            commonjava/indy)
      -n NAME, --name=NAME  The container name under which to deploy 
                            Indy (default: indy)
      -p PORT, --port=PORT  Port on which Indy should listen 
                            (default: 8081)
      -q, --quiet           Don't start with TTY
      -S SSHDIR, --sshdir=SSHDIR
                            Directory to mount for use as .ssh directory 
                            by Indy (default: disabled)
      -U URL, --url=URL     URL from which to download Indy (default is
                            calculated, using 'savant' flavor)
      -V VERSION, --version=VERSION
                            The version of Indy to deploy 
                            (default: 0.19.2)
      --config=CONFIG       Volume mount for 'etc/indy' configuration 
                            directory
      --data=DATA           Volume mount for state data files
      --logs=LOGS           Volume mount for logs
      --storage=STORAGE     Volume mount for artifact storage

As you see, it offers a lot of potential for customizing our Indy deployment. We can customize several simple things, like selecting which port the Indy server is mapped to, or exposing a debug port for JPDA debugging. We can specify a particular Indy version, distribution flavor, or even a URL to the distribution binary we want to deploy.

We can do things having deeper effects, yet which are still essentially simple, like customizing which Docker image and local container names to use (but please use caution if you customize these, since the defaults are used in other scripts for things like autodeployment).

Or, to explore the rabbit hole a little deeper still, read on.

#### Host-Side Storage
<a name="host-side-storage"></a>

One maintenance task you're probably not thinking about yet is upgrading Indy. As with many such tasks, it pays to plan ahead to make sure your Indy deployment can be upgraded without data loss.

Ordinarily, upgrading an application is a matter of upgrading an operating system package, or maybe unpacking a tarball into a particular directory and restarting. The Docker approach makes this a little more complicated, since the Docker deployment unit is a whole container. Upgrading via Docker usually involves pulling a new Docker image and creating a new container based on it. This can be attractive because you don't have to worry about establishing and following your own filesystem practices for persistent configuration and data. If you're used to using packages like RPMs it can still be attractive because you don't need to figure out how to adapt if you have to deploy an instance on a system that doesn't support RPM. 

But for all the convenience, upgrading via Docker usually inspires one big question: How do I preserve my data? In terms of Indy itself, this includes both hosted artifacts and instance state (repository definitions, autoprox rules, content templates, and the like). The standard approach documented in the Docker community is to use a volume container, since it provides a portable way to define storage that's separated from the container running the code itself. To take this approach with Indy, read [below](#indy-volumes).

In simpler environments, you can store persistent data directly on the host system. This can be attractive in its own right, since you can back those storage locations with networked filesystems, RAID arrays, or whatever you want. If you want to use this strategy, you'll want to look again at the initialization script above. Specifically, look at these options:

      --config=CONFIG       Volume mount for 'etc/indy' configuration 
                            directory
      --data=DATA           Volume mount for state data files
      --logs=LOGS           Volume mount for logs
      --storage=STORAGE     Volume mount for artifact storage

If we want to adhere to standards, we might try something like this:

    $ mkdir -p /var/lib/indy/{data,storage}
    $ mkdir -p /var/log/indy
    $ mkdir -p /etc/indy
    $ ./init-indy-server-no-vols.py \
         --config=/etc/indy \
         --data=/var/lib/indy/data \
         --logs=/var/log/indy \
         --storage=/var/lib/indy/storage

Or, if we're using a networked filesystem mount, we might prefer this:

    $ mkdir -p /mnt/indy/{data,storage}
    $ mkdir -p /mnt/indy/logs
    $ mkdir -p /mnt/indy/etc
    $ ./init-indy-server-no-vols.py \
         --config=/mnt/indy/etc \
         --data=/mnt/indy/data \
         --logs=/mnt/indy/logs \
         --storage=/mnt/indy/storage

The initialization command will add Docker volume mounts for each of these, so Indy has access to thim. When the Indy server container boots, it will expand the appropriate files from the distribution binary into these locations. Later, if we need to upgrade Indy, we can have confidence that these files won't be erased when the old container is replaced.

#### SSH, Git, and the `etc` URL
<a name="ssh-git-etc"></a>


One core practice of the DevOps movement is keeping revision history on configurations as well as code and management scripts. Without a version history for your application's configuration, how will you recover to a known-good state if someone changes your application's configuration and something goes wrong? This is a very common problem in complex server deployments. Not having a revision history can wreck your rollback process and leave services dead in the water while you scramble to guess at what was in that file before, when everything was working.

Okay, so you've decided to put your Indy etc directory in version control. Great. Now, how do you get those versioned files into your running Docker container? If you're using host-side storage with Docker volumes, you may have many options for initializing your Indy configuration. On the other hand, to ensure you're using a configuration that hasn't drifted through manual editing, it might be better **not** to mount that etc/indy volume, and instead clone the configuration directly from your Git repository. So, let's take another look at that `init-indy-server-no-vols.py` script's help output:

      -E ETC_URL, --etc-url=ETC_URL
                            URL from which to git-clone the etc/indy 
                            directory (default: disabled)
      ...
      -S SSHDIR, --sshdir=SSHDIR
                            Directory to mount for use as .ssh directory 
                            by Indy (default: disabled)

The `-E` option allows you to pass in the URL for your Indy etc directory's Git repository:

    $ ./init-indy-server-no-vols.py \
         -E https://github.com/jdcasey/indy-etc.git

Or, perhaps you want to keep that configuration someplace more private? Say, someplace you need SSH to clone from? No problem, just setup a normal `.ssh` directory structure containing:

    $ls -1 $HOME/indy-ssh/
      known_hosts
      id_rsa

Then, use the option to add your new SSH configuration directory *and* the etc URL to the initialization script:

    $ ./init-indy-server-no-vols.py \
         -E ssh://secretsauce.myco.com/git/indy-etc.git \
         -S $HOME/indy-ssh

Before the Indy container boots, the initialization script will call the SELinux command `chcon -Rt svirt_sandbox_file_t $HOME/indy-ssh` to allow Docker access to the indy-ssh directory, then create the container with a mounted volume for the SSH configuration. When the container boots, it will copy the mounted SSH config volume to `/root/.ssh` to make it available for Git commands to use.

Indy itself doesn't know about using Git to manage its `etc` directory; managing that directory is considered out of scope for Indy. However, Indy does have a [Revisions add-on](revisions-addon.html) (available in the default `savant` distribution flavor), which knows about using Git to manage the contents of the `data` directory (the place where repository definitions, etc. are stored). If you use the above method to clone your Indy configuration from Git, you can also tell Indy (via its configuration) to clone another Git repository for its data directory. You can even tell it to push any changes to a remote Git repository using the same configuration!

#### Dev Mode
<a name="devmode"></a>

If you're interested in hacking on Indy, or if you're using file-based autodeployment (see [Auto-Deployment](#autodeploy), below), you'll probably be interested in the devmode option:

      -d DEVDIR, --devdir=DEVDIR
                            Directory to mount for devmode deployment
                            (default: disabled, to use released version 
                            from URL)

If you build your own Indy binary and want to deploy it to a Docker container for testing, you can upload the launcher tarball to some directory (eg. `/tmp/indy-dev/`), then restart your Indy server container with something like this:

    $ ./init-indy-server.py -d /tmp/indy-dev/ <your-custom-options>

Alternatively, if you wanted to, you could expand your binary into a directory structure (eg. `/tmp/indy-dev/indy/`) and start in devmode using the expanded directory:

    $ ./init-indy-server.py -d /tmp/indy-dev/indy/ <your-custom-options>

This way, you don't have to reference a remote URL for downloading the Indy binary you want to deploy.

### Using a Volume Container
<a name="indy-volumes"></a>

If you need portable storage with the ability to upgrade Indy without losing your data and artifacts, you need a volume container. Using a volume container for Indy isn't hard; we've created a purpose-built Docker image for it, along with scripts in indy-docker to support it. Basically, there are just a few differences from the instructions given above:

* You have to initialize and start the `indy-volumes` container before starting the `indy` server container. You can initialize this container with the following command:

    $ ./init-indy-volumes

  For the most part, you shouldn't really need to customize this command.

* You have to use the `init-indy-server` script in place of the `init-indy-server-no-vols` script for all of the commands given above. For obvious reasons, host-side storage won't work with this script, so those options aren't available.

* You have to use two systemd service definitions instead of one. Instead of `indy-server-novols.service`, you'll copy/enable/start `indy-volumes.service` **then** `indy-server.service`. (The process for installing/starting these services is the same as given above.)

### Upgrading Your Indy Deployment
<a name="upgrading-your-indy-deployment"></a>

Once you've accounted for the data and artifact storage for things you want to keep, upgrading is a simple matter of stopping the old container and starting a new one:

    $ sudo docker stop indy
    $ sudo docker rm indy.last
    $ sudo docker rename indy indy.last
    $ ./init-indy-server-no-vols.py <your-custom-options> \
         -V <new-version>

...or, with a volume container in place:

    $ ./init-indy-server.py <your-custom-options> \
         -V <new-version>

Note that we're keeping the old Indy container around (renamed to `indy.last`) just in case we need to rollback.

Also note that if you're using a Docker volume container, you don't need to do anything with that container. Your data and artifacts are stored there; you don't want to touch it during an upgrade!

### Auto-Deployment
<a name="autodeploy"></a>

Using Docker containers, it's possible to boil down the upgrade process to the mechanical execution of a few commands, or even to script it completely. Having done that, why not set it up as a cron job that checks some location for an updated version and auto-deploys it? By controlling the source of that updated version information you still have complete control over when the server updates itself.

The indy-docker utilities project comes with two scripts to help with this:

#### The `autodeploy-file.py` script

This script watches one directory for a new Indy launcher tarball to appear. When it detects a new version to deploy, it copies it to a **devmode** directory and restarts the Indy Docker container, referencing that devmode directory. (See [Dev Mode](#devmode), above).

For example, you might watch a directory of `/tmp/indy-next` and use a devmode directory of `/tmp/indy-deployed` using a cron job like this:

    */5 * * * * root /root/indy-docker/utils/autodeploy-file.py \
      -s indy-server \
      -w /tmp/indy-next \
      -d /tmp/indy-deployed \
      /root/indy-docker-utils/init-indy-server.py \
      -d /tmp/indy-deployed -p 80 -q

When a new file appears in `/tmp/indy-next`, this script will detect it. When it does, it removes anything in `/tmp/indy-deployed` and moves the new file in. Then, it removes the existing Indy server container and reinitializes it with the new devmode tarball.

#### The `autodeploy-url.py` script

This script watches a `maven-metadata.xml` file on some remote URL (given as an option value to the script) for either SNAPSHOT or release version updates. When it sees one, it restarts the Indy Docker container, referencing the new URL for the updated version.

This approach can be very useful if you're developing against a snapshot version of Indy (perhaps developing a new Indy feature in concert with another application that uses the Java client API). In such a situation, you might use a crontab entry similar to this:

    */5 * * * * root /root/indy-docker-utils/autodeploy-url.py \
      -s indy-server \
      -u https://oss.sonatype.org/content/repositories/snapshots/org/commonjava/indy/launch/indy-launcher-savant/0.19.3-SNAPSHOT/maven-metadata.xml \
      /root/indy-docker-utils/init-indy-server \
      -U {url} -p 80 -q

The above uses the `{url}` placeholder to inject the calculated URL to the new snapshot tarball when one appears. This placeholder is populated before calling `/root/indy-docker-utils/init-indy-server`.

If instead you want to publish release versions of Indy somewhere, you could manage your own maven-metadata.xml file alongside it, and use an entry like this:

    0 */4 * * * root /root/indy-docker-utils/autodeploy-url.py \
      -s indy-server \
      -r \
      -v \
       -u http://my.server.org/indy-deploy/maven-metadata.xml \
       /root/indy-docker-utils/init-indy-server \
       -U {url} -p 80 -q

This approach (using `r` tells the script to look for a release version) will cause the script to look for a version directory that's a sibling of the `maven-metadata.xml` file, which contains the new launcher tarball. Again, notice the `{url}` placeholder that's populated before calling `/root/indy-docker-utils/init-indy-server`.
