# Setting up for a local test of the cache migrator

To setup a local test, you'll need a Postgres Docker container and a locally running Indy instance. You can get this by building the Indy version this doc sits in using:

```
    <indy-workspace-dir>$ mvn -DskipTests=true clean install
```

Then, from this directory you can run:

```
    $ ./local-env-setup.sh
```

This will start a postgres db in your localhost docker system, and then configure and start a local indy instance. That instance will use the postgres db to hold the content index cache.

After you have this, you should run a few builds of other projects using the settings.xml included here, which points at the local indy instance.

After running a few builds, shut down the Indy instance using CTL-C...

BUT DO NOT SHUTDOWN THE POSTGRESQL SERVER YET.

After that, you should be able to build and run the cache-migrator using something like this:

```
    $ java -jar target/indy-cache-migrator-1.5.0.4-SNAPSHOT.jar \
        -i /home/jdcasey/code/stacks/indy/indy/scratch/postgres/infinispan.xml \
        dump \
        indy_cache_content_index \
        /tmp/ispn-dump.gz
```

After you're all done testing, you can shutdown the postgres db using:

```
    $ docker stop postgres
```


