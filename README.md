# Dapla note repository
[![Build Status](https://drone.prod-bip-ci.ssb.no/api/badges/statisticsnorway/dapla-noterepo/status.svg)](https://drone.prod-bip-ci.ssb.no/statisticsnorway/dapla-noterepo)

Dapla is a micro-service part of the [dapla]() architecture. The repository is used by notebook systems as a backend.

It extends the notebook system with statistics norway-specific functions such as data input/output normalization and history management and note validation.

## Test 

The service is a micronaut application and can be run with the following gradle command:

```
~# git clone git@github.com:statisticsnorway/dapla-noterepo.git
~# cd dapla-noterepo 
~/dapla-noterepo# gradle :server:run
```

The zeppelin repository implementation comes with a Dockerfile. It relies on the 
platform SSB registry so you need to authenticate with it first.
```
~/dapla-noterepo# gcloud auth configure-docker
~/dapla-noterepo# docker run -it -p 8080:8080 --rm  --name dapla-zeppelin dapla-zeppelin
```

You can also build your own image locally: 
```
~/dapla-noterepo# gradle build
~/dapla-noterepo# docker build -t dapla-zeppelin -f zeppelin-plugin/docker/Dockerfile .
```

## Zeppelin repository

The zeppelin repository implementation communicates with the service with gRPC calls. The plugin can be configured in zeppelin or via environment variables.

| Environment variable        | Zeppelin variable           | Description                             | Default value |
| --------------------------- | --------------------------- | --------------------------------------- | ------------- |
| ZEPPELIN_NOTEBOOK_SSB_HOST  | zeppelin.notebook.ssb.host  | The host running the gRPC service       | localhost     |
| ZEPPELIN_NOTEBOOK_SSB_PORT  | zeppelin.notebook.ssb.port  | The port to connect to the gRPC service | 50051         |
| ZEPPELIN_NOTEBOOK_SSB_PLAIN | zeppelin.notebook.ssb.plain | Set plain (non-encrypted) protocol.     | false         |

