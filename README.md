Nucleus
====

* [Source]
* [Issues]
* [Website]
* [Downloads]
* [Documentation]
* [TeamCity]
* [IRC] | EsperNet - #nucleus

Master branch build status:  [![API 5 branch](https://img.shields.io/travis/NucleusPowered/Nucleus/sponge-api/5.svg?style=flat-square)](https://travis-ci.org/NucleusPowered/Nucleus)

Licence: [MIT](LICENSE.md)

Nucleus is a Sponge plugin that forms a solid base for your server, providing essential commands, events, and other
tidbits that you might need. Extremely configurable, only loading up the commands and modules you want (and providing a way for
plugins to disable modules that they replace the functionality of), and providing a simple and rich API, Nucleus is an
elite plugin for providing simple server tasks, and an essential addition to your server!
 
We're in beta right now, but we'd love the feedback!

Nucleus is being actively developed for Sponge API 5 and Sponge API 6. Sponge API 4.1 builds are receiving bug fixes only.  

## Contributions

Are you a talented programmer wanting to contribute some code? Perhaps someone who likes to write documentation? Do you 
have a bug that you want to report? Or perhaps you have an idea for a cool new idea that would fit in with Nucleus? We'd
be grateful for your contributions - we're an open community that appreciates any help you are willing to give!

* Read our [guidelines].
* Open an issue if you have a bug to report, or a pull request with your changes.

## Getting and Building Nucleus

To get a copy of the Nucleus source, ensure you have Git installed, and run the following commands from a command prompt
or terminal:

1. `git clone git@github.com:NucleusPowered/Nucleus.git`
2. `cd Nucleus`
3. `cp scripts/pre-commit .git/hooks`

To build Nucleus, navigate to the source directory and run either:

* `./gradlew build` on UNIX and UNIX like systems (including OS X and Linux)
* `gradlew build` on Windows systems

You will find the compiled JAR which will be named like `Nucleus-x.x.x-all.jar` in `builds/libs/`. Make sure you use the
one ending in `-all`, as this contains shadowed dependencies.

## Building against Nucleus

Nucleus is available via a Maven repository.

* Repo: `http://repo.drnaylor.co.uk/artifactory/list/minecraft`
* Group ID: `io.github.nucleuspowered`
* Artifact Name: `Nucleus`

You can also use [JitPack](https://jitpack.io/#NucleusPowered/Nucleus) as a repository, if you prefer.

## Third Party Libraries

The compiled Nucleus plugin includes the following libraries (with their licences in parentheses):

* QuickStart Module Loader (MIT)
* MaxMind GeoIP2 API (Apache 2)
* MaxMind DB (Apache 2)
* Jackson (Apache 2)
* Cron-Utils (Apache 2)

See [THIRDPARTY.md](THIRDPARTY.md) for more details.

[Source]: https://github.com/NucleusPowered/Nucleus
[Issues]: https://github.com/NucleusPowered/Nucleus/issues
[Downloads]: https://github.com/NucleusPowered/Nucleus/releases
[Website]: http://nucleuspowered.org/
[Documentation]: http://nucleuspowered.org/docs
[guidelines]: Contributing.md
[TeamCity]: https://teamcity.drnaylor.co.uk/project.html?projectId=QuickStart&tab=projectOverview
[IRC]: http://esper.net/publicirc.php
