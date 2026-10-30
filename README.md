# cantaloupe-auth-delegate

A Java delegate for handling UCLA's auth needs in Cantaloupe.

### Prerequisites

* [JDK 17](https://adoptopenjdk.net/)
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

There are a series of unit and integration tests that run automatically when the project is built. For more detail about
them, look in the `src/test/java` and `src/test/resources` directories.

### Deploying the Delegate

To deploy a SNAPSHOT version of the delegate, run the following (with the proper credentials in your Maven settings.xml file):

    mvn deploy -Drevision=0.3.0 -Ddocker.username=username -Ddocker.password=password

To use the deployed Jar file with a [docker-cantaloupe](https://github.com/uclalibrary/docker-cantaloupe) container, supply the Maven repository location of the delegate via the DELEGATE_URL.

For instance, on a Linux machine:

    docker run -p 8182:8182 -e "CANTALOUPE_ENDPOINT_ADMIN_SECRET=secret" -e "CANTALOUPE_ENDPOINT_ADMIN_ENABLED=true" \
      -e "DELEGATE_URL=https://repo1.maven.org/maven2/edu/ucla/library/cantaloupe-auth-delegate/0.3.0/cantaloupe-auth-delegate-0.3.0.jar" \
      --name melon -v "$PWD/src/test/resources/images:/imageroot" uclalibrary/cantaloupe:5.0.7-2

Note: Sonatype has changed its upload policy for Jar uploads. We may need to adjust the way we work with them (e.g., create a new account), so new jars may be temporarily unavailable from Maven Central.

### Caveats

The current implementation of the authentication delegate is hard-coded to work with UCLA's IP ranges. To customize this project, you'd have to fork it and change the way that the IPs are configured.

### Contact

If you have comments, questions, or improvements for this project, please feel free to open a ticket in the project's [issues](https://github.com/UCLALibrary/cantaloupe-auth-delegate/issues) queue.