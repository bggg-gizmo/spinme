# SpinMe Android release

## Current verified release

The current Android release on `main` is:

| Field | Value |
| --- | --- |
| APK | [`SpinMe-v1.0.0.apk`](SpinMe-v1.0.0.apk) |
| Package | `com.spinme.app` |
| Version | `1.0.0` |
| versionCode | `3` |
| minSdk | `29` |
| targetSdk | `36` |
| File size | `45,800 bytes` |
| SHA-256 | `ec7b0d95d4a0c1ba0e7d9e5d70ce673efc28ca93c7a30624925556d0d4301389` |
| Git blob | `deb9f22b9b2c2a209593ee776eee68957408bf8d` |
| Signing certificate SHA-256 | `bcfd9417869e6671e2f33efa7345cd49c0f4a9452d014f1d1fab67de2e166093` |

## Verification performed

The v1.0.0 release artifact was verified for:

- ZIP central-directory/archive integrity;
- Android zip alignment;
- APK Signature Scheme v3;
- signing certificate fingerprint;
- package name `com.spinme.app`;
- `versionName 1.0.0` / `versionCode 3`;
- minSdk 29 / targetSdk 36;
- final SHA-256.

Canonical machine-readable verification data:

- [`SHA256SUMS.txt`](SHA256SUMS.txt)
- [`RELEASE-METADATA.txt`](RELEASE-METADATA.txt)

## Historical artifacts

The malformed v0.2.0 public artifact was withdrawn and quarantined under [`../old_bulids/`](../old_bulids/). It is not a supported release.

The v0.1.0 artifact and its original verification metadata are also retained there for history.

v0.1.0 used a different Android signing certificate. Users coming directly from v0.1.0 must uninstall it before installing v1.0.0.
