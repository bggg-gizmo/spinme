# Security Policy

## Supported versions

| Version | Support status |
| --- | --- |
| **1.0.0** | Current supported public release |
| 0.2.0 | Withdrawn; malformed public APK artifact; unsupported |
| 0.1.0 | Historical; unsupported |

Security fixes are provided for the current public release line. Older development snapshots and archived/withdrawn binaries are not supported.

## Reporting a vulnerability

Please do not open a public issue for suspected security vulnerabilities.

Use GitHub's **Private vulnerability reporting** feature for this repository. If that feature is temporarily unavailable, contact the repository owner privately through an established trusted channel.

Include the affected version/commit, reproduction steps, security impact, and only the proof-of-concept material needed to reproduce the issue. Do not include unnecessary secrets, personal data, or third-party information.

## Release integrity

The current Android release is `releases/SpinMe-v1.0.0.apk`.

Published integrity data is maintained in:

- `releases/SHA256SUMS.txt`
- `releases/RELEASE-METADATA.txt`

The release build process verifies APK signing, zip alignment, and ZIP archive integrity before publication.
