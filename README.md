### This repo includes

This repo includes a back end and an Android application client. The client will receive SMSs, extract transaction details and then send them back to the server.

First install [momo](https://github.com/SharkFourSix/momo) to your local maven repo

```
mvn clean install
```

Then configure as a dependency

```xml
<dependency>
	<groupId>lib.gintec_rdl</groupId>
	<artifactId>Momo</artifactId>
	<version>1</version>
</dependency>
```

Build ***momo-poc-web-app*** and ***android-app*** (Gradle/Android Studio) alternatively, a compiled APK has been attached (under releases)
