# Contributing to HttpBuilder-NG

> If you have not already, please read our [Code of Conduct](https://github.com/http-builder-ng/http-builder-ng/blob/master/CODE_OF_CONDUCT.md).

## Did you find a bug?

You have a few options:

* You can walk away slowly and try to forget anything ever happened (we wouldn't blame you, but we'd rather you didn't).
* You could [submit an issue](https://github.com/http-builder-ng/http-builder-ng/issues/new) and we will do our best to get it fixed in an upcoming release.
* You could fork the project, fix it yourself and submit a pull request (see below)

## Do you have a feature suggestion?

See above.

## Do you want to help?

Check to see if we have any issues labeled "Help Wanted" or if we have any that have been sitting for a while inactive. Take a branch off of the `master` branch (though we may ask you to base it off of `development`) and create a pull request once your code is ready.

## Having problems using HttpBuilder?

The best place to get help with HTTP Builder NG is via [StackOverflow](http://stackoverflow.com/) with the tag `httpbuilder-ng` - be careful there is a lot of information out there about the old version of the code and the solutions will most likely _not_ work. You can also try to contact us directly, but that is not recommended.

## Submitting an Issue

If you have decided to [submit an issue](https://github.com/http-builder-ng/http-builder-ng/issues/new), please be as descriptive as possible. Provide details about the code related to the issue and provide as much of an example as possible - if we can't reproduce the issue, we probably won't be able to fix it any time soon. A good guideline is "[How to create a Minimal, Complete, and Verifiable example](https://stackoverflow.com/help/mcve)" from StackOverflow.

Once your issue has been created, we will triage it and figure out how to proceed.

Also, please be sure to check the [User Guide Troubleshooting](https://http-builder-ng.github.io/http-builder-ng/asciidoc/html5/#_troubleshooting) section for any known resolution to your problem. 

## Submitting a Pull Request

If you fix a bug or implement a new feature and want to get it into the main repo, create a Pull Request. We are pretty low on process and code standards, so basically if you follow the general Java and Groovy coding standards you should be fine. We will review the request and figure out where to go with it from there.

A couple general rules. If you add or modify code:

* Provide Spock unit tests for it (don't decrease the code coverage).
* Add or update relevant documentation (User Guide, JavaDocs, Readme, etc).
* Use our standard copyright header (or run `./gradlew licenseFormat` to have it applied)
* Do not add `@author` tags to your contributions - we prefer to use `git blame` for authorship tracking

As far as branches go, we will generally have a `development` branch off of `master` which will be the branching off point for small development tasks which will be released together. With that in mind:

* If you have a bug fix, it's a bit of a toss up, you could merge into `development` or `master` based on the scope of the change and severity of the issue. When in doubt, merge into `development` and we will redirect as needed.
* If you have a feature, you should work off of the `development` branch and then merge into it when you are ready to submit.

We are pretty flexible and there are always exceptions, just think about what you are doing and everything will be ok.

When/if we accept your contributions, we will add your name and GitHub profile link to the "Contributions" section of the User Guide.
