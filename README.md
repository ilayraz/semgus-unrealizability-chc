# semgus-unrealizability-chc

This project is an implementation of a CHC unrealizability solver for SemGuS problems using Z3 Spacer module.
It relies on the [SemGuS parser](https://github.com/SemGuS-git/Semgus-Parser) in order to create files that it can consume.
This project requires Java17+ preview version features (pattern matching switch) to run.

To build the project, run `./gradlew build` command, and to `./gradlew run` to run.
The program takes a single argument input of the target JSON file produced by the SemGuS parser.
For example, to run with `plus-2-times-3.json` example file, use the command `./gradlew run --args="$(pwd)/examples/plus-2-times-3.json"`.