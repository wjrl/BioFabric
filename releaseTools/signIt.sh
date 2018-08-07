#! /bin/bash

KEYDIR=$1
CODESIGN=$2
VER=$3

echo -n "Enter the key:"
read -s PERM
echo

# Sign the jar file:
jarsigner -storetype pkcs12 -storepass ${PERM} -keystore ${KEYDIR}/isbcert.p12 -tsa http://timestamp.comodoca.com/rfc3161 -signedJar ${CODESIGN}/sBioFabric-V${VER}.jar ${CODESIGN}/bioFabric-V${VER}.jar "institute for systems biology's comodo ca limited id"
