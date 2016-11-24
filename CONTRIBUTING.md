# Contributing to HTTP Builder NG

## Did you find a bug?

You have a few options:

* You can walk away slowly and try to forget anything ever happened (we wouldn't blame you, but we'd rather you didn't).
* You could [submit an issue](https://github.com/http-builder-ng/http-builder-ng/issues/new) and we will do our best to get it fixed in an upcoming release.
* You could fork the project, fix it yourself and submit a pull request (see below)

## Do you have a feature suggestion?

See above.

## Submitting an Issue

If you have decided to [submit an issue](https://github.com/http-builder-ng/http-builder-ng/issues/new), please be as descriptive as possible. Provide details about the code related to the issue and provide as much of an example as possible - if we can't reproduce the issue, we probably won't be able to fix it any time soon.

Once your issue has been created, we will triage it and figure out how to proceed.

## Submitting a Pull Request

If you fix a bug or implement a new feature and want to get it into the main repo, create a Pull Request. We are pretty low on process and code standards, so basically if you follow the general Java and Groovy coding standards you should be fine. We will review the request and figure out where to go with it from there.

A couple general rules. If you add or modify code:

* Provide Spock unit tests for it (don't decrease the code coverage).
* Add or update relevant documentation (User Guide, JavaDocs, Readme, etc). 

Generally, our upcoming release is done from the `development` branch which we create branches from for individual tasks - the `master` branch is for the current production code. With that in mind:

* If you have a bug fix, it's a bit of a toss up, you could merge into `development` or `master` based on the scope of the change.
* If you have a feature, you should work off of the `development` branch.

We are pretty flexible, just think about what you are doing and everything will be ok.

## Having problems using HttpBuilder?

The best place to get help with HTTP Builder NG is via [StackOverflow](http://stackoverflow.com/) with the tag `httpbuilder-ng` - be careful there is a lot of information out there about the old version of the code and the solutions will most likely _not_ work. You can also try to contact us directly, but that is not recommended.
