# Contributing guide

**Want to contribute? Great!**

We try to make it easy, and all contributions, even the smaller ones, are more than welcome.
This includes bug reports, fixes, documentation, examples...
But first, read this page (including the small print at the end).

* [Coding Philosophy](#coding-philosophy)
* [Before you contribute](#before-you-contribute)
  + [Code reviews](#code-reviews)
  + [Coding Guidelines](#coding-guidelines)
  + [Continuous Integration](#continuous-integration)
  + [Tests and documentation are not optional](#tests-and-documentation-are-not-optional)
  + [Current status](#current-status)
* [Reporting an issue](#reporting-an-issue)
* [Legal](#legal)
* [The small print](#the-small-print)

## Coding Philosophy

Writing a runtime is a big challenge. We want Chicory to always be a solid foundation
for running Wasm in Java. In order to accomplish this, it's going to take a large team
of diverse contributors. That's why our goal up front is to aim for writing "dumb" and
simple code that's easy to understand and is as backwards compatible as possible.

The reason is we want to optimize for:

 * attracting more contributors
 * supporting more users
 * supporting more platforms

It's important we focus on this in the beginning phase so that we can grow a large team
of contributors. We also want to make it possible for people with deep Wasm and runtime experience,
but maybe not the deepest Java experience, to contribute.

This philosophy tends to lead us down what might seem like some non-optimal paths. We may ask you
to simplify things, use older versions of Java, or reject improvements that we feel
makes things more confusing without enough measurable benefits.

We don't expect to be able to maintain this forever, and some parts of the codebase will 
inevitably suffer from necessary complexity in the name of correctness, safety, or speed.
But we are holding the line as long as we can.

## Before you contribute

To contribute, use GitHub Pull Requests, from your **own** fork.

Also, make sure you have set up your Git authorship correctly:

```
git config --global user.name "Your Full Name"
git config --global user.email your.email@example.com
```

If you use different computers to contribute, please make sure the name is the same on all your computers.

We may use this information to acknowledge your contributions!

### Code reviews

All submissions, including submissions by project members, need to be reviewed and approved by at least one project owner before being merged.

[GitHub Pull Request Review Process](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/reviewing-changes-in-pull-requests/about-pull-request-reviews) is followed for every pull request.

### Coding Guidelines

 * We primarily use the Git history to track authorship. GitHub also has [this nice page with your contributions](https://github.com/quarkusio/quarkus/graphs/contributors).
 * Please take care to write code that fits with existing code styles. The syntactic formatting is automated and can be applied project wise using the command `mvn spotless:apply`.
 * Commits should be atomic and semantic. Please properly squash your pull requests before submitting them. Fixup commits can be used temporarily during the review process but things should be squashed at the end to have meaningful commits.
 * We typically squash and merge pull requests when they are approved. This tends to keep the commit history a little bit more tidy without placing undue burden on the developers.

### Continuous Integration

Because we are all humans, and to ensure Chicory evolves in the right direction, all changes must pass continuous integration before being merged. The CI is based on GitHub Actions, which means that pull requests will receive automatic feedback.  Please watch out for the results of these workflows to see if your PR passes all tests.

### Tests and documentation are not optional

Don't forget to include tests in your pull requests.
Also don't forget the documentation (reference documentation, javadoc...).

## Reporting an issue

This project uses GitHub issues to manage the issues. Open an issue directly in GitHub.

If you believe you found a bug, and it's likely possible, please indicate a way to reproduce it, what you are seeing and what you would expect to see.

## Legal

All original contributions to Chicory projects are licensed under the
[ASL - Apache License](https://www.apache.org/licenses/LICENSE-2.0),
version 2.0 or later, or, if another license is specified as governing the file or directory being
modified, such other license.

## The small print

This project is an open source project. Please act responsibly, be nice, polite and enjoy!

