#!/bin/bash

set -o nounset \
    -o errexit \
    -o verbose \
    -o xtrace

export MSYS_NO_PATHCONV=1

echo -n "cleaning-up files"
   echo -n "cleaning-up files"
for i in *.crt *.jks *.csr *_creds *~ *.sh\# *.srl *.key *.req *.pem .\#*; do
    echo -n "Checking for files $i .."
    if test -f "$i" ; then
        echo "exists"
	rm $i
    else
        echo "does not exist"
    fi
done
echo "Done."

#exit

echo -n "Generate the CA Key.."
   # Generate CA key
   openssl req -new -x509 -keyout snakeoil-ca-1.key -out snakeoil-ca-1.crt -days 365 -subj "/CN=ca1.test.confluent.io/OU=TEST/O=CONFLUENT/L=PaloAlto/C=US" -passin pass:confluentxx -passout pass:confluentxx
   #openssl req -new -x509 -keyout snakeoil-ca-1.key -out snakeoil-ca-1.crt -days 365 -subj \'/CN=ca1.test.confluent.io/OU=TEST/O=CONFLUENT/L=PaloAlto/S=Ca/C=US' -passin pass:confluentxx -passout pass:confluentxx
   # openssl req -new -x509 -keyout snakeoil-ca-2.key -out snakeoil-ca-2.crt -days 365 -subj '/CN=ca2.test.confluent.io/OU=TEST/O=CONFLUENT/L=PaloAlto/S=Ca/C=US' -passin pass:confluentxx -passout pass:confluentxx
echo "Done."

echo -n "Generate the kafka Cert pem.."
   # Kafkacat
   openssl genrsa -des3 -passout "pass:confluentxx" -out kafkacat.client.key 1024
   openssl req -passin "pass:confluentxx" -passout "pass:confluentxx" -key kafkacat.client.key -new -out kafkacat.client.req -subj '/CN=kafkacat.test.confluent.io/OU=TEST/O=CONFLUENT/L=PaloAlto/C=US'
   openssl x509 -req -CA snakeoil-ca-1.crt -CAkey snakeoil-ca-1.key -in kafkacat.client.req -out kafkacat-ca1-signed.pem -days 9999 -CAcreateserial -passin "pass:confluentxx"
echo "Done."



for i in kaf1 kaf2 kaf3 producer consumer; do
#do
   echo -n "Create keystores $i .."
   # Create keystores
   keytool -genkey -noprompt \
		 -alias $i \
		 -dname "CN=$i.test.confluent.io, OU=TEST, O=CONFLUENT, L=PaloAlto, C=US" \
		 -keystore kafka.$i.keystore.jks \
		 -keyalg RSA \
		 -storepass confluentxx \
		 -keypass confluentxx
   echo "Done."

   echo -n "Create CSR, sign, and import into keystore $i"
	# Create CSR, sign the key and import back into keystore
	keytool -keystore kafka.$i.keystore.jks -alias $i -certreq -file $i.csr -storepass confluentxx -keypass confluentxx
        echo -n "."
	openssl x509 -req -CA snakeoil-ca-1.crt -CAkey snakeoil-ca-1.key -in $i.csr -out $i-ca1-signed.crt -days 9999 -CAcreateserial -passin pass:confluentxx
        echo -n "."
	keytool -keystore kafka.$i.keystore.jks -alias CARoot -import -file snakeoil-ca-1.crt -storepass confluentxx -keypass confluentxx
        echo -n "."
	keytool -keystore kafka.$i.keystore.jks -alias $i -import -file $i-ca1-signed.crt -storepass confluentxx -keypass confluentxx
   echo "Done."

   echo -n "Create truststore and import CA Cert $i .."
	# Create truststore and import the CA cert.
	keytool -keystore kafka.$i.truststore.jks -alias CARoot -import -file snakeoil-ca-1.crt -storepass confluentxx -keypass confluentxx
   echo "Done."

   echo -n "Create Credential files $i .."
        echo "confluentxx" > ${i}_sslkey_creds
        echo "confluentxx" > ${i}_keystore_creds
        echo "confluentxx" > ${i}_truststore_creds
  echo "Done."
done
