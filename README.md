# cantaloupe-auth-delegate

A Java delegate for supporting IIIF Auth in Cantaloupe.

This is not ready for use. If you've found this already somehow, bookmark it and return again later. Thanks.

### Building the Delegate

There is a two step build process. First, to install the cantaloupe jar in your local Maven repository, run:

    mvn validate

After that is done, you can run the following to build the delegate:

    mvn verify

This will run tests of the delegate and provide a Jar file to use with your v5 Cantaloupe installation.

### Deploying the Delegate

To deploy a SNAPSHOT version of the delegate, run the following (with the proper credentials in your Maven settings.xml file):

    mvn deploy -Drevision=0.0.1-SNAPSHOT

To use the deployed Jar file with a [docker-cantaloupe](https://github.com/uclalibrary/docker-cantaloupe) container, supply its Maven repository location via the DELEGATE_URL.

For instance:

    docker run -p 8182:8182 -e "CANTALOUPE_ENDPOINT_ADMIN_SECRET=secret" -e "CANTALOUPE_ENDPOINT_ADMIN_ENABLED=true" \
      -e "DELEGATE_URL=https://s01.oss.sonatype.org/content/repositories/snapshots/edu/ucla/library/cantaloupe-auth-delegate/0.0.1-SNAPSHOT/cantaloupe-auth-delegate-0.0.1-20210526.031106-2.jar" \
      --name melon -v "/home/kevin/Workspace/docker-cantaloupe/src/test/resources/images:/imageroot" cantaloupe:latest

Note that the SNAPSHOT version will change each time the deploy is run. That part of the example above will need to be updated after a new deployment. The path to the mounted image directory will also need to be changed. More generic instructions will be provided once we release a versioned release of the delegate.
