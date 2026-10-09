# cantaloupe-auth-delegate

A Java delegate for handling UCLA's auth needs in Cantaloupe.

### Prerequisites

* [A JDK](https://adoptopenjdk.net/) (>= 11)
* [Maven](https://maven.apache.org/)
* [git-lfs](https://git-lfs.github.com/)

The git-lfs application must be installed and have been initialized locally before the Cantaloupe dependency will be able to be downloaded.

### Building the Delegate

There is a two step build process. First, to install the cantaloupe jar in your local Maven repository, run:

    mvn validate

After that is done, you can run the following to build the delegate (Docker credentials are required to pull the `cantaloupe-ucla` image for testing):

    mvn verify -Ddocker.username=username -Ddocker.password=password

This will run tests of the delegate and provide a Jar file to use with your Cantaloupe installation.

### Testing the Delegate

TODO

### Deploying the Delegate

To deploy a SNAPSHOT version of the delegate, run the following (with the proper credentials in your Maven settings.xml file):

    mvn deploy -Drevision=0.0.1 -Ddocker.username=username -Ddocker.password=password

To use the deployed Jar file with a [docker-cantaloupe](https://github.com/uclalibrary/docker-cantaloupe) container, supply the Maven repository location of the delegate via the DELEGATE_URL.

For instance, on a Linux machine:

    docker run -p 8182:8182 -e "CANTALOUPE_ENDPOINT_ADMIN_SECRET=secret" -e "CANTALOUPE_ENDPOINT_ADMIN_ENABLED=true" \
      -e "DELEGATE_URL=https://repo1.maven.org/maven2/edu/ucla/library/cantaloupe-auth-delegate/0.0.1/cantaloupe-auth-delegate-0.0.1.jar" \
      --name melon -v "$PWD/src/test/resources/images:/imageroot" uclalibrary/cantaloupe:5.0.4-0

Note that the SNAPSHOT version will change each time the deploy is run. That part of the example above will need to be updated after a new snapshot deployment. The path to the mounted image directory will also need to be changed to work with your local file system. Improved instructions will be provided once we publish a versioned release of the delegate.
