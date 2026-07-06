# Release Notes

## 1.0.11

### Changed

- Updated Maven coordinates in documentation and module POMs from `1.0.9` to `1.0.11`.
- Added Maven Central metadata to published POMs, including project URL, license, developer, and SCM information.
- Configured `flatten-maven-plugin` with `ossrh` mode so generated release POMs retain Maven Central-required metadata.
- Included `THIRD-PARTY_LICENSE.txt` in release jars alongside `THIRD_PARTY_LICENSES.txt` for downstream legal compliance checks.

### Compatibility

- No runtime API changes.
- Java 17+ and Maven 3.8+ remain required.
