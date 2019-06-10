#! /bin/bash

# Building the Java Keystore for Code Signing
#
# 1) You need a code signing certiificate. ISB uses GoDaddy. Run the following to generate the CSR to send to GoDaddy.
# The encrypted private key was written to ISB_codesign.key, which is a PEM file. The CSR, ISB_codesign.req, was sent
# off to GoDaddy. Note that it includes the public key in it. You need to remember the password used to encrypt 
# the private key.
#
# % openssl req -new -keyout ISB_codesign.key -out ISB_codesign.req -config codesign.openssl.conf
#
# 2) Having generated the request, confirm that it is OK:
#
# % openssl req -in ISB_codesign.req -text -verify -noout
#
#
# 3) Having sent CSR to GoDaddy, you get back a zip file, <alphanumeric-blah>.zip. This archive contains two files, 
#    <other-alphanumeric-blah>-SHA2.pem and <other-alphanumeric-blah>-SHA2.spc. Copy the <alphanumeric-blah>.zip into
#    codeSigning.zip.
#
# 4) We need to get the private key, and the code signing PEM into a Java Key Store (JKS). Looking on the web, there apparently 
# is no Java keystore command that can import a pre-existing private key. Intstead, go to http://www.agentbob.info/agentbob/79-AB.html. 
# At that URL, there is a Java program, ImportKey, that can do it, and also pull in the signing certificate. I have modified that code 
# (keytools folder) to take all the parameters on the command line, and to prompt for the keystone password.
#
# 5) This script buildKeystore.sh builds the keystore. Note that there was a problem converting the signing cert PEM, which has 
# two certs creating *a certificate chain*. The command "openssl crl2pkcs7" had to be used instead of the "openssl x509" that 
# was suggested.

KEY_HOME=$1
TOOL_HOME=$2
PHASE=$3
cd ${KEY_HOME}

#
# Ant passwords come from an inputstring. This needs two passwords, so do it in two phases
#

if [ ${PHASE} = "phase_1" ]; then
	echo "running ${PHASE}"

	echo "You must have copied the current GoDaddy cert to codeSigning.zip"
	exit
	unzip -o codeSigning.zip
	PEMFILE=`ls *-SHA2.pem`
	
	#
	# The private key (actually a PEM file) needs to be converted to DER format:
	#
	
	openssl pkcs8 -topk8 -nocrypt -in ISB_codesign.key -out ISB_codesign.der -outform der
	
	#
	# The cert from GoDaddy contains a certificate chain of two certs. If one uses the
	# command suggested in the ImportKey comments, openssl x509, only one of the certs gets
	# written out. The following command gets both certs in the chain out to a der file:
	#
	
	openssl crl2pkcs7 -nocrl -certfile ${PEMFILE} -outform DER -out codeChain.der
fi

if [ ${PHASE} = "phase_2" ]; then
	echo "running ${PHASE}"
	#
	# This builds the keystore. It prompts for the password for the keystore:
	#
	
	java -cp ${TOOL_HOME}/ImportKey.jar keytools.ImportKey ISB_codesign.der codeChain.der isbcert ISBSignCert.jks
fi