# Kafka Store Pub/Sub

This Git Repository is a Kafka Producer '-' Consumer model built as 2 distinct Spring MicroServices. This was first developed against Sprint 3.1.5.

There are also Docker-Compose files present to set-up a quick environment to show how the docker configs can quickly configure a kafka lab to test the microsevices.

Access the h2 database in the following link - http://localhost:8081/h2-console

JDBC Address: jdbc:h2:mem:kafkabd

Consumer Event recover options for this repository. All of these recoveries options are scenarios where there
is an actual recovery reason to retry the consumption again.

The behavior is controller auto configuration with application.yml -> by spring.kafka.republishRetries: true|false

   - False - Stores the recoverable uses in the DB. (options 2 & 4)
   - True - Republishes back into kafka with the newly update topics (options 1 & 3)

1) When a recoverable failure occurs because of runtime exception. It would be possible to re-tag the message's topic and update the kafka message 'store-events.RETRY'.

   The caveat is avoiding an infinite loop on the recovery while the rutime exception continues.

1) When a recoverable failure occurs because of runtime exception. It would be possible to persist the message into the DB and process the message when the runtime exception is corrected. The build a scaler that will take the retry records and reprocess them back into the consumer process. Basically polling the message queue at a regular interval for failed records and reprocess them by redirecting the storeEventsConsumer service logic.

1) Discard the message - 'Re-Publish' the failed record into a deadletter topic for tracking purposes. Same as previously metioned. Push the update back into kafka with an updated topic: 'store-events.DEADLETTER'

1) Save the failed record into the DB for tracking purposes only and move on...
