# Gitlet

This project is a simplified version of Git, implemented in Java, called Gitlet. It mimics some of the core functionalities of Git, allowing users to track file changes, create commits, and manage branches in a version control system.

## Project Structure

- **Gitlet Directory**: Contains the main codebase.
  - **`Commit.java`**: Manages commit objects, including metadata and file changes.
  - **`Diff.java`**: Handles differences between file versions.
  - **`DumpObj.java` & `Dumpable.java`**: Utilities for object serialization and deserialization.
  - **`GitletException.java`**: Custom exception handling for Gitlet-specific errors.
  - **`Main.java`**: The entry point for Gitlet, managing user inputs and commands.
  - **`Repository.java`**: Core functionality for version control operations.
  - **`StagingArea.java`**: Manages the staging area for changes.
  - **`UnitTest.java`**: Unit tests for various components.
  - **`Utils.java`**: General utility functions.
  - **`Makefile`**: Script to compile and run the project.
  - **`gitlet-design.md`**: Detailed design documentation.
  - **`gitlet_design.png`**: Visual representation of the Gitlet design.

- **Testing Directory**: Contains unit and integration tests to ensure the robustness of the Gitlet implementation.

## Features

- Version control operations like commit, checkout, and branch management.
- Object serialization for storing commit histories.
- Simple diff and staging functionalities.

## Getting Started

To compile and run the project, use the provided `Makefile`. Run the unit tests in the `testing` directory to verify the implementation.

## License

This project is open-source and available under the MIT License.
