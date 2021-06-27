Getting and Building Nucleus
Nucleus currently uses Gradle 7.0.0 and is compiled using JDK 11 (but to a Java 8 target).

To get a copy of the Nucleus source, ensure you have Git installed, and run the following commands from a command prompt or terminal:

git clone git@github.com:NucleusPowered/Nucleus.git
cd Nucleus
cp scripts/pre-commit .git/hooks
To build Nucleus, navigate to the source directory and run either:

./gradlew build on UNIX and UNIX like systems (including macOS and Linux)
gradlew build on Windows systems
You will find the compiled JAR which will be named like Nucleus-[version]-plugin.jar in output/. A corresponding API and javadocs jar will also exist.

Building against the Nucleus API
Nucleus is available via a Maven repository.

Repo: 'https://repo.drnaylor.co.uk/artifactory/list/minecraft`
Group ID: io.github.nucleuspowered
Artifact Name: nucleus-api
The versioning follows version[-SNAPSHOT|-ALPHAn|-BETAn|-RCn], where n is an integer. Add the -SNAPSHOT section for the latest snapshot.

You can also get Nucleus as a whole this way, but internals may break at any time. The API is guaranteed to be more stable.

You can also use JitPack as a repository, if you prefer.

Third Party Libraries
The compiled Nucleus plugin includes the following libraries (with their licences in parentheses):

Vavr 0.10.3 (Apache 2.0)
See THIRDPARTY.md for more details.
