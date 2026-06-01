# Gradle Wrapper JAR

The `gradle-wrapper.jar` binary is not stored in this repository.

To generate it, run the following command from the project root:

```bash
gradle wrapper --gradle-version 8.14 --distribution-type all
```

This requires a local Gradle installation. Alternatively, if Gradle is not installed:

```bash
# On macOS (Homebrew)
brew install gradle
gradle wrapper --gradle-version 8.14 --distribution-type all

# On Linux (SDKMAN)
sdk install gradle
gradle wrapper --gradle-version 8.14 --distribution-type all
```

After running this command, `gradle-wrapper.jar` will appear in this directory and
`gradlew` / `gradlew.bat` will function correctly.
