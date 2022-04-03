Nucleus v3
====

For version 1, the legacy version for Sponge API 7.1 (Minecraft 1.12), see the tag "S7.1-v1-FINAL".\
For version 2, the current version for Sponge API 7.3 (Minecraft 1.12), see the branch "v2/S7".

* [Source]
* [Issues]
* [Website]
* [Downloads]
* [Documentation]
* [Discord]

Licence: [MIT](LICENSE.md) (except for the Nucleus logo, which is all rights reserved, and any third party shaded code)

Nucleus is a Sponge plugin that forms a solid base for your server, providing essential commands, events, and other
tidbits that you might need. Extremely configurable, only loading up the commands and modules you want (and providing a way for
plugins to disable modules that they replace the functionality of), and providing a simple and rich API, Nucleus is an
elite plugin for providing simple server tasks, and an essential addition to your server!

Nucleus is being actively developed for Sponge API 8. 

## Contributions

Are you a talented programmer wanting to contribute some code? Perhaps someone who likes to write documentation? Do you 
have a bug that you want to report? Or perhaps you have an idea for a cool new idea that would fit in with Nucleus? We'd
be grateful for your contributions - we're an open community that appreciates any help you are willing to give!

* Read our [guidelines].
* Open an issue if you have a bug to report, or a pull request with your changes.

## Getting and Building Nucleus

Nucleus currently uses Gradle 7.3.3 and is compiled using JDK 11 (but to a Java 8 target). 

To get a copy of the Nucleus source, ensure you have Git installed, and run the following commands from a command prompt
or terminal:

1. `git clone git@github.com:NucleusPowered/Nucleus.git`
2. `cd Nucleus`
3. `cp scripts/pre-commit .git/hooks`

To build Nucleus, navigate to the source directory and run either:

* `./gradlew build` on UNIX and UNIX like systems (including macOS and Linux)
* `gradlew build` on Windows systems

You will find the compiled JAR which will be named like `Nucleus-[version]-plugin.jar` in `output/`. A corresponding API and
javadocs jar will also exist.

## Building against the Nucleus API

Nucleus is available via a Maven repository.

* Repo: `https://repo.drnaylor.co.uk/artifactory/list/minecraft`
* Group ID: `io.github.nucleuspowered`
* Artifact Name: `nucleus-api`

The versioning follows `version[-SNAPSHOT|-ALPHAn|-BETAn|-RCn]`, where `n` is an integer. Add the `-SNAPSHOT` section for the latest snapshot.

You can also get Nucleus as a whole this way, but internals may break at any time. The API is guaranteed to be more stable.

You can also use [JitPack](https://jitpack.io/#NucleusPowered/Nucleus) as a repository, if you prefer.

## Third Party Libraries

The compiled Nucleus plugin includes the following libraries (with their licences in parentheses):

* Vavr 0.10.3 (Apache 2.0)

See [THIRDPARTY.md](THIRDPARTY.md) for more details.

[Source]: https://github.com/NucleusPowered/Nucleus
[Issues]: https://github.com/NucleusPowered/Nucleus/issues
[Downloads]: https://github.com/NucleusPowered/Nucleus/releases
[Website]: http://v2-beta.nucleuspowered.org/
[Documentation]: http://v2-beta.nucleuspowered.org/docs
[guidelines]: Contributing.md
[Discord]: https://discord.gg/A9QHG5H
