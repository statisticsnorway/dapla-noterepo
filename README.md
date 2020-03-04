# Dapla note repository
[![Build Status](https://drone.prod-bip-ci.ssb.no/api/badges/statisticsnorway/dapla-noterepo/status.svg)](https://drone.prod-bip-ci.ssb.no/statisticsnorway/dapla-noterepo)

Dapla noterepo contains tools and extensions for notebook system that are in use in the dapla project.

The oidc module extends the pac4j with a servlet that exposes the tokens of the users it has seen.

The docker folder contains an alpine based zeppelin image with a custom interpreter script that fetches the user tokens
before starting the interpreter.
