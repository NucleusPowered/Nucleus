Contributor Guidelines
===

## Code Style

We follow the [Google's Java Code Conventions](https://google.github.io/styleguide/javaguide.html) but some of the more
important things to note, including changes are:

* Line endings
    * Use Unix line endings when committing (\n).
    * Windows users of git can do `git config --global core.autocrlf true` to
    let git convert them automatically.

* Column width
    * 150 for Javadocs
    * 150 for code
    * Feel free to wrap when it will help with readability

* Indentation
    * Use 4 space tabs for indentations, do not use spaces.

* Vertical whitespace
    * Place a blank line before the first member of a class, interface, enum,
    etc. (i.e. after class Example {) as well as after the last member.

* File headers
    * File headers must contain the license headers for the project as defined in HEADER.txt.
    * You can use the `gradle licenseFormat` to automatically to do this for
    you.

* Imports
    * Imports must be grouped in the following order:
        * normal imports
        * java imports
        * javax imports
        * static imports

* Javadocs
    * Do not use @author
    * Wrap additional paragraphs in `<p>` and `</p>`
    * Capitalize the first letter in the descriptions within each “at clause”,
    i.e. @param name Player to affect, no periods
    * Be descriptive when explaining the purpose of the class, interface,
    enum, method etc.

* Deprecation
    * Do not deprecate content unless you have a replacement, or if the
    provided feature is removed.
    * When deprecating, provide the month and year when it was deprecated.

## Code Conventions
* Use Optionals instead of returning null in the API.
* Only one declaration per line.
* All uppercase letters for a constant variable. Multiple words should be
separated with a '_'.

## Pull Request
* Provide at least one use case if providing a new feature.
* Before sending a pull request ensure that your branch is up to date with the
branch you are targeting.
* You may add as many commits as you want. However rebase them before you mark
them for final examination. This just helps us going through the code easier.
* Please follow the above guidelines for your pull request(s) to be
**accepted**.