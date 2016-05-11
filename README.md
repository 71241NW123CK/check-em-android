# check-em-android
An Android library for scanning routing and account information from a check using MICR.

## Setup
To use, include the host repository in your repositories
```groovy
repositories {
    maven {
        url 'https://raw.githubusercontent.com/71241NW123CK/maven-repo/master'
    }
}
```
and add the project to your dependencies:
```groovy
dependencies {
    compile 'party.treesquaredcode.android:check-em:0.0.0'
}
```

## Usage
Make sure your device has a camera.
Call `CheckEm.getSharedInstance().checkEm(activity)`, where `activity` is the current `Activity`.  This will open the check scanning Activity.
Get the routing number using `CheckEm.getSharedInstance().getRoutingNumberResult()`.
Get the account number (including any dashes) using `CheckEm.getSharedInstance().getAccountNumberResult()`.
To clear these values, use `CheckEm.getSharedInstance().clearResults()`.  Subsequent calls to get the routing number or account number will return `null` until the user has scanned a check again.
