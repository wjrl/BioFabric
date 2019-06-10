# /bin/bash

#
# This script signs the BioFabric windows installer file:
#

INSTALLER_HOME=$1
SC_HOME=$2
KEY_HOME=$3

PEMFILE=`ls ${KEY_HOME}/*-SHA2.pem`

mv ${INSTALLER_HOME}/BioFabricInstaller.exe ${INSTALLER_HOME}/UnsignedBioFabricInstaller.exe

#
# This requires that the osslsigncode tool (https://sourceforge.net/projects/osslsigncode/) has been installed:
#

${SC_HOME}/osslsigncode sign -certs ${PEMFILE} -key ${KEY_HOME}/ISB_codesign.key -n "BioFabric" \
  -i http://www.BioFabric.org -t http://timestamp.verisign.com/scripts/timstamp.dll \
  -in ${INSTALLER_HOME}/UnsignedBioFabricInstaller.exe -out ${INSTALLER_HOME}/BioFabricInstaller.exe
