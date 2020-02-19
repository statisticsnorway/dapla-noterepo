
This module extends the `org.pac4j.oidc.client.KeycloakOidcClient` class with a http servlet that exposes the oidc access and refresh tokens. 

## Usage

Use the `MouthyKeycloakOidcClient` class as you would in the shiro configuration.

```
[...]

keycloakOidcClient = no.ssb.dapla.notes.oidc.MouthyKeycloakOidcClient
keycloakOidcClient.name = keycloakOidcClient
keycloakOidcClient.configuration = $oidcConfig
keycloakOidcClient.authorizationGenerator = $roleAdminAuthGenerator

[...]
```

Using the patched interpreter (not yet released) one can then access the tokens in the spark session as follow:

```
import org.apache.spark.sql.SparkSession

val session = SparkSession.builder().getOrCreate()
session.conf.get('spark.ssb.username')
session.conf.get('spark.ssb.access')
session.conf.get('spark.ssb.refresh')
```