# /bin/sh

EXEHOME=$1
RESHOME=$2
LAUNCH4J_HOME=$3
L4J_WORKING=$EXEHOME/bioFabl4jWorking.xml
VERCOMP=$4
CURRYEAR=$5
SC_HOME=$6
KEY_HOME=$7

echo $EXEHOME
echo $L4J_WORKING
cat $L4J_WORKING

java -jar $LAUNCH4J_HOME/launch4j.jar $L4J_WORKING

#
# This requires that the osslsigncode tool (https://sourceforge.net/projects/osslsigncode/) has been installed:
#

echo -n "Enter the key:"
read -s PERM
echo

${SC_HOME}/osslsigncode sign -pkcs12 ${KEY_HOME}/isbcert.p12 -pass ${PERM} -n "BioFabric" -i http://www.BioFabric.org \
  -t http://timestamp.verisign.com/scripts/timstamp.dll -in BioFabric.exe -out SignedBioFabric.exe

# signcode says everything is fine, but Windows says there is no signature. Use osslsigncode above instead:
# openssl pkcs12 -in isbcert.p12 -nocerts -nodes -out key.pem
# openssl rsa -in key.pem -outform PVK -pvk-strong -out authenticode.pvk
# openssl pkcs12 -in isbcert.p12 -nokeys -nodes -out cert.pem
# openssl crl2pkcs7 -nocrl -certfile cert.pem -outform DER -out authenticode.spc
# rm cert.pem
# rm key.pem
# signcode -spc authenticode.spc -v authenticode.pvk -a sha1 -$ commercial -n BioFabric \
#          -i http://www.BioFabric.org/ -t http://timestamp.verisign.com/scripts/timstamp.dll -tr 10 \ 
#          $EXEHOME/BioFabric.exe

cd $EXEHOME
rm -rf $EXEHOME/Licenses
mkdir $EXEHOME/Licenses
cat $RESHOME/Licenses/LICENSE-README-TEMPLATE.txt | sed "s#__WJRL_CURRYEAR__#$CURRYEAR#" > Licenses/LICENSE-README.txt
cp $RESHOME/Licenses/LICENSE.txt $RESHOME/Licenses/LICENSE-SUN.txt \
$RESHOME/Licenses/launch4j-head-LICENSE.txt $EXEHOME/Licenses
cp $RESHOME/README-INSTALL.txt $EXEHOME

zip BioFabric$VERCOMP.zip Licenses Licenses/LICENSE.txt Licenses/LICENSE-README.txt \
  Licenses/LICENSE-SUN.txt Licenses/launch4j-head-LICENSE.txt BioFabric.exe README-INSTALL.txt







