Essence
====

[![GitHub license](https://img.shields.io/badge/License-MIT-brightgreen.svg?style=flat-square)](/LICENSE.txt) |
[![GitHub license](https://img.shields.io/badge/Dependency-JitPack-brightgreen.svg)](https://jitpack.io/#EssencePowered/Essence)

* [Source]
* [Issues] | [![GitHub issues](https://img.shields.io/github/issues/EssencePowered/Essence.svg?style=flat-square)](http://www.github.com/EssencePowered/Essence/issues/)
* [Wiki]
* [Downloads] | [![Github Releases](https://img.shields.io/github/downloads/EssencePowered/Essence/total.svg?style=flat-square)](http://www.github.com/EssencePowered/Essence/releases)

### Status
* Master: [![Travis branch](https://img.shields.io/travis/EssencePowered/Essence/master.svg?style=flat-square)](https://travis-ci.org/EssencePowered/Essence)
* Dev: [![Travis branch](https://img.shields.io/travis/EssencePowered/Essence/dev.svg?style=flat-square)](https://travis-ci.org/EssencePowered/Essence)

Essence is meant to be exactly that, a Sponge plugin that allows you to quickly start your server with essential
commands, events, and other tidbits that you might need. Extremely configurable, only loading up the modules you want
(and providing a way for plugins to disable modules that they replace the functionality of), and providing a simple and
rich API, Essence is an elite plugin for providing simple server tasks.
 
...or so I hope, soon! With a website full of documentation and everything (at http://quickstart.dualspiral.co.uk/)! I'm only just getting this off the ground as Sponge has started to stabilise!

Essence currently provides:

* Warps
* Homes
* Teleports (needs to be tested!)
* Kicks
* Bans
* Jails
* Mutes
* Messages
* Mails
* Seen
* Time
* Weather
* Broadcasts
* Fly
* Healing and Feeding
* AFK

...some of which have the ability for warmups and cooldowns to take place, as well as charging users.

## Prerequisites
* [Java] 8
* [Git]

## Cloning
1. `git clone git@github.com:EssencePowered/Essence.git`
2. `cd Essence`
3. `cp scripts/pre-commit .git/hooks`

## Building
#### Notes
1. If you do not have Gradle installed then use ./gradlew for Unix systems or Git Bash and gradlew.bat for Windows
systems in place of any 'gradle' command.

In order to build Essence, simply run the `gradle` command. You will find the compiled JAR named similar to
`Essence-x.x.x.jar` in `builds/libs/`

## Contributing
Are you a talented programmer wanting to contribute some code? EssencePowered would love the help!
* Read our [guidelines].
* Open a pull request with your changes.

[Source]: https://github.com/EssencePowered/Essence
[Issues]: https://github.com/EssencePowered/Essence/issues
[Wiki]: https://github.com/EssencePowered/Essence/wiki
[Downloads]: https://github.com/EssencePowered/Essence/releases
[Java]: http://www.oracle.com/technetwork/java/javase/downloads/index.html
[Git]: https://git-scm.com/book/en/v2/Getting-Started-Installing-Git
[guidelines]: Contributing.md