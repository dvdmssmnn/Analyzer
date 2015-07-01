#Analyzer

##Things that get checked
* ECB Mode for encryption
* Random IV, Salt
* Constant keys or passwords
* PBKDF iteration count (< 1000)
* Password leaks (HTTP, Filesystem)
* Hardcoded HTTP Authentication (but not all constant strings are found yet)
* Sensitive Data (needs to be manually defined in 'data.json')


###data.json
Is an array of JSON objects.

    {
      "type" : "plain"/"base64"/"hex",
      "content" : "CONTENT"
    }

##Run targets

###download

    ant download -Dhost=http://{ip}:8080 -Ddir={destination dir}

Waits until all data is written to the database and downloads the information from the device.

###checkAll

    ant checkAll -Ddir={directory}

Checks for all properties listed above. This can take a while because we have to glue together all input and output to the crypto functions etc. (probably there are a lot of calls to hash functions and those get searched too).

###GUI

    ant gui

There is already a rudimentary GUI available that shows more information than the *CheckAll*-Tool, but only for some parts:
* Crypto
  * Show the plaintext data of the cipher
  * Show traces
* Sensitive information
  * Show where it was found

##Missing
###Data storage
* CoreData (needs to be implemented first in callLog)
* NSUserDefaults
* Keychain

###Asymmetric ciphers

Asymmetric ciphers are not checked yet.
