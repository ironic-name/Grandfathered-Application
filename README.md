# Grandfathered

Projects often change hands and providers shut down. [Parse](http://parseplatform.org) is an example of this. While this example might not be the best as Parse can still be deployed in an open source incarnation, this project aims to explore the possibility of migrating users from one backend to another as painlessly, from a user's point of view, as possible.

## The problem

For security, passwords are never (at least by anyone reputable) stored as plain text and are usually hashed. To avoid the use of the use of [rainbow tables](https://en.wikipedia.org/wiki/Rainbow_table) and other similar tools of attacking, passwords are often stored as a [salted hash](https://crackstation.net/hashing-security.htm). 

This is great for security, but makes it difficult to integrate users migrated form an old backend to a new one. The multitude of hashing algorithms that could be used to secure passwords make it difficult to match them up. Parse, the ever present example (and the hypothetical subject of this experiment) uses [BCrypt](http://bcrypt.sourceforge.net) to secure their password. BCrypt is based off of the blowfish algorithm published by Bruce Schneider in 1993.

>There are two kinds of cryptography in this world: cryptography that will stop your kid sister from reading your files, and cryptography that will stop major governments from reading your files. This book is about the latter.
- _Preface to Applied Cryptography by Bruce Schneier_

This is a fairly common algorithm and would typically make a migration simple, but it is not to say that a target backend would necessarily use the same hashing algorithm.

## Theory

This experiment aims to find a method of migrating users whose passwords are encrypted with a certain hashing algorithm to a backend that makes use of a different hashing function. 

Difficulty also is introduced when selecting a hashing algorithm. Not all are secure. In light of its [vulnerabilities being exposed](https://shattered.io) earlier this year, we will most definitely not be using SHA1.

The manner in which we will try to accomplish this is by creating a new password for each user in a computable, yet secret, manner, and encrypting this password with the new backend's hashing algorithm. The old backend's password will still be stored in the users records on the new backend's database. This will allow us to make the migration to the new backend almost invisible from the user's perspective. 

When signing in, the migrated user will enter their email address and password, which will be used to:

* Check if the user is migrated 
* Compute their migration password, and sign in. 

Once successfully logged in, the password the user entered will be compared to the hash representing their old password. If this comparison fails, the password can be seen as not matching. If the comparison is successful, the passwords match, and the user's password on the backend will be updated to match whatever they entered as their password on the initial migration.

## Background

This is not the first time that I have attempted to solve this problem, though there were some rather large flaws in my previous approach, as I am sure there are in this one.

These included:

* Using one master password for all migrated users
* Leaving this password in plain text in the application in which the solution was implemented
* Checking the file and all related secrets into version control as is.

To fix this, the outlined approach:

* Uses a secret that is defined by the developer when they run through the process
* Hides the secret key in an environment variable in the android sample application in a that isn't checked into version control
* Computes a unique password for each user, instead of using one password for all, decreasing the chances of compromising all migrated users in one fell swoop

## Expected procedure

The expected process can be (shortly and roughly) expressed as: 

    // Not real
    String email = "user@something.com";
    String password = "Password"; 
    
    // Button click or something similar
    buttonClick() {
        tryLoginWithDetails(email, password);
    }
    
    tryLoginWithDetails(String email, String password) {
        // Do login stuff
        if (succeeded) {
            // Proceed to navigate to home screen or whatever
        } else {
            if (isMigrated(email)) {
                tryLoginAsMigratedUser(email, password);
            } else {
                // Incorrect password. Display error message
            }
        }
    }

    Boolean isMigrated(String email) {
        // Get email addresses of migrated users
        String[] migratedUsers = allMgratedUsers;
        // Check if email exists in array of migrated users, and return true if it does
        return existence; 
    }
    
    tryLoginAsMigratedUser(String email, String password) {
        String computedPassword = calculatePassword(email);
       
        // Do login stuff
        if (succeeded) {
            // Get hashed version of old backend password
            Boolean matched = OldHashingAlgorithm.compare(password, oldBackendHash);
            if (matched) {
                // Password matched old backend password set as new backend password and remove user form list of migrated users
                // Proceed to navigate to home screen or whatever 
            } else { 
                // Passwords don't match; display error
            } 
        } else {
             
        }
    }
     
    String calculatePassword(String email) {
        // Secret key retrieved from environment variables
        return NewHashingAlgorithm.hash(email + secretKey);
    }

## Implementation

For this example, the users will be drawn from the export of a Parse user database and run through the [companion to this project](https://github.com/ironic-name/grandfathered-firebase) to generate the required files for the migration. The documentation on this repo also has more instructions on the migration.

### NOTE:
After the user's have been migrated, the secret key used to generate the migration files should be added as a environment variable in this repo's sample project.

After doing the migration and setting the secret key, the application can be built and run. 

### Test:
- [ ] Attempt to log in with one of the many migrated users and deliberately specify an incorrect password. Assert the system does not log the user in. 
- [ ] Attempt to log in with the another migrated user with the correct password. Assert the system logs the user in.
- [ ] Log out after logging in as migrated user and try to log in again. Assert the system logs the user in.
- [ ] Attempt to log in with a non-migrated user (sign up), and then log in. Assert the system logs the user in.

## Observations
Due to having to compute the users migration password locally on an often not incredibly powerful device the migrated login process can be slow.
