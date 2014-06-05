# Vert.x CAS Auth

This is a simple example of CAS integration with VertX HTTP Server. When accessing the URLS this service offers, it will check the session for a ticket, validate that ticket and redirect to the service. IF no ticket is present, it will redirect you to the CAS login page.

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

After a successful login, the sessionId is given a auth boolean to save on CAS calls in the future. This
is all just POC code, and will leak memory, The SessionStorage class should probably put the data in CouchBase
or mongo with expiry times set.