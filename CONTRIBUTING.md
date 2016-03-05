Contributor Guidelines
===

Thank you for contributing to Nucleus! Please have a read through this document and make sure you understand it all
before contributing.

## Reporting an Issue, Requesting a Feature

When submitting an issue through GitHub Issues, please remember that we are volunteers, and as volunteers, we have
limited time to be able to help. We would love to spend as much time fixing your issues and implementing features
as possible. To that end, please keep the following in mind when reporting bugs and requesting features:

* Make the title of the report clear. "It's broken" does not help us much, "Nucleus reports an error when I try to use
/lockweather" is more helpful and lets us see what is wrong just by looking at the list.

* When reporting a bug:
    * Describe how you reproduce the bug step by step
    * Explain what you expect to happen
    * Explain what actually happens
    * Include screenshots or console output if it helps explain the issue
    (but do **not** take a screenshot of console output)

* When describing a feature:
    * Tell us what you'd like to see
    * Tell us why you want it, and what the use case is
    * Give us an idea on how it should work

* If we need more information about a bug or a feature, we'll ask for clarification! We want to get the system right.

* If we reject a suggestion, remember that Nucleus is an _essentials_ plugin, NOT "everything but the kitchen sink".
We may, however, consider writing a separate plugin as part of the Nucleus family.

For more information about writing a bug report in general, have a look at [this page](http://www.chiark.greenend.org.uk/~sgtatham/bugs.html),
particularly the summary at the bottom.

## Pull Requests

If you'd like to write code for us, great, welcome aboard! We have a few things that you should be aware of.

### Developing Nucleus.

As Nucleus is a big plugin, there are some fancy tricks involved to make the system easier to manage, including the
heavy use of annotations and auto-generation of permissions. Though it can seem intimidating at first, it's actually an easy
system if you don't have to touch the internals!

If you are developing a command, please keep the following in mind:

* ALWAYS use the `CommandBase<>` class. It contains a lot of scaffolding that the plugin loader requires.

* The `@Permissions`, `@Modules` and `@RegisterCommand` annotations are mandatory on this class.

* If you require one of the Nucleus handlers/services - check to see if it can be injected. using the `@Inject`
annotation. The plugin object is always injected and does not need to be done again.

Listeners and Runnable Tasks extend the `ListenerBase` and `TaskBase` classes, respectively. These should also have a
`@Module` annotation at the top.

The key rule here is - if you are unsure as to how something works, **please** ask us! We are more than willing to help you
as much as possible!

### Code Style

We tend to follow the [Google's Java Code Conventions](https://google.github.io/styleguide/javaguide.html) but some of
the more important things to note, including changes are:

* Line endings
    * Use Unix line endings when committing (\n).
    * Windows users of git can do `git config --global core.autocrlf true` to let git convert them automatically.

* Column width
    * 150 for Javadocs
    * 150 for code
    * Feel free to wrap when it will help with readability

* Indentation
    * Use 4 spaces for indentations, do not use tabs.

* File headers
    * File headers must contain the license headers for the project as defined in HEADER.txt.
    * You can use `gradle licenseFormat` to automatically to do this for you.

* Imports
    * Imports must be grouped in the following order:
        * normal imports
        * java imports
        * javax imports

* Javadocs
    * Do not use @author
    * Wrap additional paragraphs in `<p>` and `</p>`
    * Capitalize the first letter in the descriptions within each “at clause”,
    i.e. @param name Player to affect, no periods
    * Be descriptive when explaining the purpose of the class, interface,
    enum, method etc.

* Deprecation
    * Do not deprecate content unless you have a replacement, or if the provided feature is removed.
    * Be prepared to provide justification for the deprecation.
    * When deprecating, provide the month and year when it was deprecated.

Note that this style guide is _not_ a hard and fast requirement, but please keep your code style sane and similar
to ours.

### Code Conventions
* Return `java.util.Optional<>` if you are adding an API method that could return `null`.
* Only one declaration per line.
* All uppercase letters for a constant variable. Multiple words should be
separated with an underscore - `_`.

### Submitting your Pull Requests
In your PRs, please make sure you fulfil the following:

* Provide a justification for the change - is it a new feature or a fix to a bug?
    * Please note that we will reject features that are not deemed as "essential".
* Before sending a pull request ensure that your branch is up to date with the
branch you are targeting. This should normally be master.
* Do not squash commits unless directed to do so, but please _rebase_ your changes on
top of master when you feel your changes are ready to be submitted - _do not merge_.
We will squash the commits in a way we feel logical.
