Nucleus
====

[![GitHub license](https://img.shields.io/badge/License-MIT-brightgreen.svg?style=flat-square)](/LICENSE.txt) |
[![](https://jitpack.io/v/NucleusPowered/Nucleus.svg?style=flat-square)](https://jitpack.io/#NucleusPowered/Nucleus) |
[![Master branch](https://img.shields.io/travis/NucleusPowered/Nucleus/master.svg?style=flat-square)](https://travis-ci.org/NucleusPowered/Nucleus)

* [Source]
* [Issues] | [![GitHub issues](https://img.shields.io/github/issues/NucleusPowered/Nucleus.svg?style=flat-square)](http://www.github.com/NucleusPowered/Nucleus/issues/)
* [Wiki]
* [Downloads] | [![Github Releases](https://img.shields.io/github/downloads/NucleusPowered/Nucleus/total.svg?style=flat-square)](http://www.github.com/NucleusPowered/Nucleus/releases)

Nucleus is a Sponge plugin that forms a solid base for your server, providing essential commands, events, and other
tidbits that you might need. Extremely configurable, only loading up the commands and modules you want (and providing a way for
plugins to disable modules that they replace the functionality of), and providing a simple and rich API, Nucleus is an
elite plugin for providing simple server tasks.
 
Nucleus is the unbeatable combination of HassanS6000's EssentialCmds and experience of writing an essentials plugin, with the
system loading technology of dualspiral's QuickStart, making Nucleus an essential addition to your server.
 
...or so we hope, soon! With a website full of documentation and everything!

Some of commands will have the ability for warmups and cooldowns to take place, as well as charging users (if an Economy plugin is installed).

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

You will find the compiled JAR which will be named like `Nucleus-x.x.x.jar` in `builds/libs/`

[Source]: https://github.com/NucleusPowered/Nucleus
[Issues]: https://github.com/NucleusPowered/Nucleus/issues
[Wiki]: https://github.com/NucleusPowered/Nucleus/wiki
[Downloads]: https://github.com/NucleusPowered/Nucleus/releases
[guidelines]: Contributing.md
