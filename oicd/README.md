
This module extends the `org.pac4j.oidc.client.KeycloakOidcClient` class with a http servlet that exposes the oidc access and refresh tokens. 

## Usage

Use the `MouthyKeycloakOidcClient` class as you would in the shiro configuration.

The instances have two properties, `port` - the port on which the servlet will run- and `allowedClient` - a java compatible regexp the incoming address will be matched against.

```
[...]

keycloakOidcClient = no.ssb.dapla.notes.oidc.MouthyKeycloakOidcClient

keycloakOidcClient.port = 9877
keycloakOidcClient.allowedClient = "*."

keycloakOidcClient.name = keycloakOidcClient
keycloakOidcClient.configuration = $oidcConfig
keycloakOidcClient.authorizationGenerator = $roleAdminAuthGenerator

[...]
```

The http endpoint exposed by the `MouthyKeycloakOidcClient` servlet are: 

| Endpoint              | Method        | Description  |
| --------------------- |:-------------:| ------------:|
| /oidc/{name}/access   | GET           | The access token for the user with {name} |
| /oidc/{id}/refresh    | GET           | The refresh token for the user with {name} |



Using the patched interpreter (not yet released) one can then access the tokens in the spark session as follow:

```
import org.apache.spark.sql.SparkSession

val session = SparkSession.builder().getOrCreate()
session.conf.get('spark.ssb.username')
session.conf.get('spark.ssb.access')
session.conf.get('spark.ssb.refresh')
```