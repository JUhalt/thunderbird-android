# Wear OS email experiment

**A standalone Wear OS email app experiment, built from Thunderbird for Android.**

This independent community project explores email on a smartwatch. The app's name is still being decided.
The goal is to develop a focused prototype that can be discussed with the Thunderbird team and, if there is
agreement on direction, contribute useful work upstream.

This is an unofficial experiment. Thunderbird has not endorsed or adopted this project.

> **Status: planning and development setup.**
> This fork does not yet contain a Wear OS application module or an installable watch app.
> The milestones below describe planned work.

## What we want to build

- **Install on the watch:** a Wear OS app with an interface designed for a small, round screen.
- **Work independently:** core email functions should work without requiring a companion phone app.
- **Start with reading mail:** a recent-message list and a lightweight message reader.
- **Cooperate with Thunderbird:** explore optional integration with the existing Thunderbird app where practical.
- **Reuse existing work:** investigate Thunderbird's mail components before introducing new implementations.

## First milestones

- [x] Establish a fork and document the standalone Wear OS goal.
- [ ] Create a minimal watch app that launches in a Wear OS emulator.
- [ ] Build a sample inbox and message reader using fictional messages.
- [ ] Investigate reusable mail components, account setup, and authentication on the watch.
- [ ] Connect one test account and read recent messages directly on the watch.
- [ ] Evaluate usability, accessibility, battery use, and connectivity on a physical watch.
- [ ] Present a focused prototype and discuss a contribution path with Thunderbird maintainers.

Replying, composing, attachments, and additional mail actions will be considered after the reading experience works.

## Working in this repository

| Branch | Purpose |
| --- | --- |
| `main` | Track the upstream Thunderbird source. |
| `wearos-prototype` | Develop and document the Wear OS experiment. |

The repository currently includes Thunderbird's existing phone applications and shared code.
The planned watch application will target Wear OS; installing a phone application is not part of its goal.

For development setup, use [Android Studio and the repository setup guide][setup].
Use the checked-in Gradle wrapper and the project's documented Java and Android SDK requirements.

The next development checkpoint is a minimal watch screen running in an emulator.
Instructions for building and installing the watch app will be added when a Wear OS module exists.

## Coordination with Thunderbird

The existing [Wear OS Port issue (#6969)][wear-issue] provides background for this effort.
Before proposing changes to Thunderbird, follow the [upstream contribution workflow][workflow],
confirm the scope with maintainers, and obtain any required issue assignment.

The Thunderbird team will decide whether any proposed contribution fits its plans.

## Contributing

This experiment is maintained in [JUhalt's fork][fork].
Keep changes small and reviewable, and follow the repository's [contribution guidelines][contributing]
and [AI agent guide][agents]. AI-assisted contributions must be disclosed and meet the same review
and verification standards as other contributions.

For information about the existing Thunderbird phone applications, see the
[preserved upstream README][upstream-readme].

## Credits and license

This work builds on [Thunderbird for Android][upstream], which is based on K-9 Mail.
Credit belongs to the Thunderbird, K-9 Mail, and wider contributor communities for the existing codebase.

The repository retains the [Apache License, Version 2.0][license] and its existing [notices][notices].

[setup]: docs/contributing/development-environment.md
[wear-issue]: https://github.com/thunderbird/thunderbird-android/issues/6969
[workflow]: docs/contributing/contribution-workflow.md
[fork]: https://github.com/JUhalt/thunderbird-android/tree/wearos-prototype
[contributing]: docs/CONTRIBUTING.md
[agents]: AGENTS.md
[upstream-readme]: README.upstream.md
[upstream]: https://github.com/thunderbird/thunderbird-android
[license]: LICENSE
[notices]: NOTICE
