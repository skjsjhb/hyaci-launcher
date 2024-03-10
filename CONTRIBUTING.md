# Contributing Guidelines

## Code of Conduct

Please read our [Code of Conduct](CODE_OF_CONDUCT.md) first. It describes how we collaborate with each other with kind
and harmony.

## TL;DR

- Stick to the template when creating issues.
- Create topics with clear points.
- Open pull requests with tests.
- Resolve discussions actively.
- Search before you create.

## Issue Workflow

1. Search for existing issues **or pull requests**, your topic might have already been discussed. Also take a look at
   the discussions for any relevant content.

   If your issue is duplicated to an existing and active issue, you can comment with "+1" below and provide the details
   of your situation.

   If there is a duplicated and closed issue, do not reopen it. Comment below directly, or open a new issue instead.

   > [!NOTE]
   > We're open for extensions, but closed to modifications. An issue tagged with `invalid` or `wontfix` means that
   > we've made the final decision. Future issues of the same topic won't be accepted.

2. Confirm what kind of issue you are opening.

    - A **bug report** for reproducible bugs (with optional possible fixes).
    - A **feature request** for requesting new features or performance optimization.

   Issues should be stated with **clear points**. Questions and unclear suggestions should be moved to discussions. e.g.

   > Downloader does not respect proxy settings

   Is a valid issue (pointed out the problem). While

   > Can the download speed be optimized?

   Should be moved to discussions, since the author is unsure about the topic, and neither did they provide any
   solutions.

3. Gather relevant information for the issue.

   Provide clues to support your issue.

    - For bug reports: logs, screenshots, and system environments will be useful.
    - For feature request: code snippets and design sketches are preferred.

   Each piece of information, including the minor ones, might eventually help you and us to get the stuff done.

   > [!WARNING]
   > Please be careful with your privacy when uploading materials (especially images and logs).
   > Remove tokens, usernames and other sensitive data from the content.

4. Open an issue via the **Issue** tab, and fill in the blanks in the template:

    - The title should be short and declarative. Capitalize only on the first letter, abbreviations and pronouns.
    - The title should be written in English (for the convenience of managing).
    - The content should be written in your preferred language (for clearer expression).
    - If the content is not written in English, add a translated version below.
    - Additional lines are welcomed, but do not remove any field from the template.

5. Resolve any conversations.

   People including but not limited to maintainers will comment and share their ideas. Try to response actively.

   An issue will not be closed by bot automatically, nor would we ignore any, but it may take time to investigate.
   Please be patient.

An issue might not always end up resolved, out of the following reasons:

- The author is closing the issue.
- The issue is invalid (duplicated, out of scope). it will be closed and tagged.
- The issue contains controversial topics and none of us can convince each other. It will be closed. If it's still too
  heated, then it will also be **locked**.
- The issue is deliberately damaging the community. It will be **deleted**.

## Pull Request Workflow

1. Read and follow the guidelines of issues to create your pull request. **Directives of issues also apply to pull
   requests.**

2. Fork the repository and make necessary changes.

   Our project is based on Kotlin, so generally your code should all be in Kotlin. Avoid using Java files as supporting
   them requires modifications to the build script.

   Be careful when introducing new libraries, as each new library requires extra effort when distributing and testing.
   If a library is necessary, try to find the most suitable one.

3. Test your code.

   Write new tests for newly added features if necessary. All tests should pass before the pull request can be merged.

4. Open your pull request. Make sure the title and content of your pull requests follows the guidelines in the issues
   part.

5. Resolve any conversations.

6. Maintainers will evaluate your code and merge them (if applicable). Merges can happen by rebasing or creating a new
   merge commit. We'll take care of that.

Like issues, a pull request might not always end up merged, out of the following reasons:

- Any situation that applies to an issue.
- The code quality is terrible and cannot be merged.
- A lot of tests did not pass.
- The fork is staled and hard to merge.

## Maybe Sometimes...

We value each contribution and try to resolve every issue and pull request. However, it's impossible to resolve all of
them in practice. Therefore, please understand that your issue or pull request might not get to be part of the project
in the end, for various reasons. No matter what has happened, please don't let it discourage you from contributing to
this project or the game community. Each word you've written will help us, even might not be direct, to push the project
further and benefit the lovers of the game.