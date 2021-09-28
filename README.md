# cantaloupe-auth-delegate

A Java delegate for supporting IIIF Auth in Cantaloupe.

This is not ready for use. If you've found this already somehow, bookmark it and return again later. Thanks.

### Prerequisites

* [A JDK](https://adoptopenjdk.net/) (>= 11)
* [Maven](https://maven.apache.org/)
* [git-lfs](https://git-lfs.github.com/)

The git-lfs application must be installed and have been initialized locally before the Cantaloupe dependency will be able to be downloaded.

### Building the Delegate

There is a two step build process. First, to install the cantaloupe jar in your local Maven repository, run:

    mvn validate

After that is done, you can run the following to build the delegate:

    mvn verify

This will run tests of the delegate and provide a Jar file to use with your v5 Cantaloupe installation.

### Testing the Delegate

There are unit and integration tests. The integration tests spin up Docker containers for [Hauth](https://github.com/UCLALibrary/hauth) and [Cantaloupe](https://github.com/uclalibrary/docker-cantaloupe). In this process, the latest cantaloupe-auth-delegate build artifact is injected into the Cantaloupe container. If any tests fail, the build will fail.

An alternative to running the test containers as a part of the integration test suite is to run them directly. To do this, one would type:

    mvn initialize docker:build docker:run

To find out which host ports the containers are running on, look for output in the Maven console logs that looks like:

    [INFO] --- build-helper-maven-plugin:3.0.0:reserve-network-port (reserve-port) @ cantaloupe-auth-delegate ---
    [INFO] Reserved port 35221 for test.http.port
    [INFO] Reserved port 43871 for test.db.port
    [INFO] Reserved port 34665 for test.iiif.images.port
    [INFO] Reserved port 34733 for test.db.cache.port

Those port numbers can be used, in conjunction with the localhost host name, to access each service's public interface. The port numbers will change with each run, according to which ports are free on the machine.

Once all manual testing is completed, the containers can be stopped by typing: [ctrl]-C.

### Deploying the Delegate

To deploy a SNAPSHOT version of the delegate, run the following (with the proper credentials in your Maven settings.xml file):

    mvn deploy -Drevision=0.0.1-SNAPSHOT

To use the deployed Jar file with a [docker-cantaloupe](https://github.com/uclalibrary/docker-cantaloupe) container, supply the Maven repository location of the delegate via the DELEGATE_URL.

For instance:

    docker run -p 8182:8182 -e "CANTALOUPE_ENDPOINT_ADMIN_SECRET=secret" -e "CANTALOUPE_ENDPOINT_ADMIN_ENABLED=true" \
      -e "DELEGATE_URL=https://s01.oss.sonatype.org/content/repositories/snapshots/edu/ucla/library/cantaloupe-auth-delegate/0.0.1-SNAPSHOT/cantaloupe-auth-delegate-0.0.1-20210526.031106-2.jar" \
      --name melon -v "/home/kevin/Workspace/docker-cantaloupe/src/test/resources/images:/imageroot" cantaloupe:latest

Note that the SNAPSHOT version will change each time the deploy is run. That part of the example above will need to be updated after a new snapshot deployment. The path to the mounted image directory will also need to be changed to work with your local file system. Improved instructions will be provided once we publish a versioned release of the delegate.
