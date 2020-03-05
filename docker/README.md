
The zeppelin docker image contains a modified interpreter script that fetches the user tokens before submitting the 
application using spark-submit. 

In order for the script to function correctly, the environment variable `ZEPPELIN_SSB_USE_TOKENS` must be set to `true`
and the User Impersonate checkbox checked in the spark interpreter setting.

Only the spark, sql and pyspark interpreters are available in the dapla-zeppelin docker image.