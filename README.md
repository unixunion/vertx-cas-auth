# Vert.x CAS Auth

This is a simple example of CAS integration with VertX HTTP Server. When accessing the URLS this service offers, 
it will check the session for:

 * a sessionId
 * a auth boolean on the sessionId in the SessionStorage
 * a CAS ticket
 
 If the sessionId is not set as authenticated in the SessionStorage system, it checks for the CAS ticket. If that also
 does not exist, the request is redirected to CAS for authentication.
 

## Requirements
This example requires a running CAS server, see cas-server subdirectory for a example of a very simple CAS setup which only contains a single static user:

```
username casuser
password Mellon
```


## Config

```json
{
  "main":"com.deblox.cas.CasHttpService", // main class to boot
  "casHost": "localhost", // hostname the casClient should use to reach CAS
  "casPort": 8443, // port the casClient should use
  "cas_redirectUrl": "https://localhost:8443/cas", // used in redirects, NO trails!
  "keepAlive": true, // keep connection to Cas Alive or not.
  "trustAll": true, // allows self-signed certs for DEV
  "ssl": true
}
```

## URLS

```
http://localhost:3001/login
http://localhost:3001/logout
http://localhost:3001/someservice
```

## Running

```
./gradlew run -i
```

## SessionStorage

A simple SessionStorage is implemented as a ConcurrentMap with some basic Expiry support.

After a successful login, the sessionId is given a auth boolean to save on CAS calls in the future. This
is all just POC code, and will leak memory, The SessionStorage class should probably put the data in CouchBase
or mongo with expiry times set.