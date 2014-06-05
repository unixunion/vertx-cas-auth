## CAS-server

This is a simple CAS server which you should deploy on TOMCAT. This project is built with Maven

## Building

```
mvn clean package
```

## Configure Tomcat
### Certificate

Generte a certificate with the HOSTNAME of the CAS server in the NAME field.

NOTE! use the password ***changeit*** for the *keystore* AND *certificate*

```
keytool -genkey -alias tomcat -keyalg RSA -validity 365
What is your first and last name?
  [Unknown]:  cas.mydomain.com
```

### Tomcat SSL

Open server.xml and enable SSL by uncommenting:

```
    <Connector port="8080" protocol="HTTP/1.1"
               connectionTimeout="20000"
               redirectPort="8443" />
```

### Deploy CAS-server
Drop the cas.war file into $TOMCAT_HOME/webapps

### Verify
Browse to https://TOMCAT_HOST:8443/cas and login as

```
username casuser
password Mellon
```