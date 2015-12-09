---
title: "dotMaven WebDAV/Settings Add-On"
---

### Getting Started: Mount Up!

You can mount the dotMaven view of your Indy server using the URL:

    http://localhost:8080/mavdav

(`mavdav` is the old name for dotMaven, which has just sort of stuck around.)

This gives you two subdirectories:

* [settings](#settings)
* [storage](#storage)

Using the Linux `davfs2` package, you can mount dotMaven with a command like this:

    $ mount -t davfs http://localhost:8080/mavdav /mnt/dotmaven

<a name="settings"></a>

### Let dotMaven Generate Your Settings

If you've ever used a repository manager, you're familiar with the Maven `settings.xml` file. The first thing you always have to do after setting up your shiny new repository manager is go configure a `<mirror/>` in your settings, so Maven knows to use the proxy location instead of hitting the central repository directly. Ugh, editing XML!

To make life easier, dotMaven delivers Maven `settings.xml` files that are generated specifically for use with each and every repository available on your Indy server. You can access these generated settings files directly, using a HTTP **GET** request, or you can access them via a mounted WebDAV directory. 

For example, you might use the default `public` repository group like this:

    mvn -s /mnt/dotmaven/settings/group/settings-public.xml clean install

#### Keeping Your Local Repositories Straight

Each generated `settings.xml` specifies a `<localRepository/>` directory of:

    ${user.home}/.m2/repo-${type}-${name}

For example, on my Linux machine the default `public` group would use:

    /home/jdcasey/.m2/repo-group-public

This has the advantage that it prevents any possibility of cross-pollution of content between repositories, groups, etc. that might refer to different remote locations but use the same storage on your local disk.

#### Deploy to Your Group

If you use a settings file generated for a hosted repository, or a group that contains a hosted repository as a member, dotMaven will add an active profile to the `settings.xml` to support deployment:

For example:

    [...]
        <profile>
          <id>deploy-settings</id>
          <properties>
            <altDeploymentRepository>local-deployments::default::http://localhost:8080/api/hosted/local-deployments</altDeploymentRepository>
          </properties>
        </profile>

      </profiles>
      <activeProfiles>
        [...]
        <activeProfile>deploy-settings</activeProfile>
      </activeProfiles>
    </settings>

<a name="storage"></a>

### WebDAV for Your Stored Artifacts

The dotMaven Indy add-on aims to give you a filesystem mount that you can browse to see what content Indy has cached (or is hosting). Sometimes this can be a convenient way to quickly look at what a certain POM or metadata file contains, in your native text editor. This is a read-only view of the content Indy manages, and as such there really isn't too much to add.

### Client APIs

Since its functionality is implemented via on-demand file and directory-listing generators, the dotMaven add-on currently doesn't have a client API.


