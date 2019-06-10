#! /bin/bash

#
# Build stripped-down JREs from the OpenJDK releases. Note we are building Mac and Windows JREs *on a Mac*, so 
# we need to be using the Mac Java tools.
#

JDK_HOME=$1
BUILD_DIR=$2
VER=$3
JDK_VER=$4
JRE_TARG=$5
WHICH=$6


MAC_JAVA_CONTENTS=${JDK_HOME}/Apple/jdk-${JDK_VER}.jdk/Contents
MAC_JAVA_HOME=${MAC_JAVA_CONTENTS}/Home
WIN_JAVA_HOME=${JDK_HOME}/Windows/jdk-${JDK_VER}
LIN_JAVA_HOME=${JDK_HOME}/Linux/jdk-${JDK_VER}
 
JRE_DEPS=`${MAC_JAVA_HOME}/bin/jdeps -s ${BUILD_DIR}/bioFabric-V${VER}.jar | awk '{print $3}' | paste -sd "," -`

cd ${JRE_TARG}

# Linux version:

if [ ${WHICH} = "Linux" ]; then
  rm -rf lin-jre-${JDK_VER}

  ${MAC_JAVA_HOME}/bin/jlink --module-path ${LIN_JAVA_HOME}/jmods --add-modules ${JRE_DEPS} --output lin-jre-${JDK_VER} --strip-debug --compress 2 --no-header-files --no-man-pages
  chmod +x lin-jre-${JDK_VER}/bin/*
  find lin-jre-${JDK_VER} -name .DS_Store -delete
fi

# Windows version:

if [ ${WHICH} = "Win" ]; then
  rm -rf win-jre-${JDK_VER}

  ${MAC_JAVA_HOME}/bin/jlink --module-path ${WIN_JAVA_HOME}/jmods --add-modules ${JRE_DEPS} --output win-jre-${JDK_VER} --strip-debug --compress 2 --no-header-files --no-man-pages

  find win-jre-${JDK_VER} -name .DS_Store -delete
fi

# Mac version:

if [ ${WHICH} = "Mac" ]; then
  rm -rf mac-jre-${JDK_VER}
  mkdir -p mac-jre-${JDK_VER}/Contents
  mkdir mac-jre-${JDK_VER}/Contents/MacOS
  cp ${MAC_JAVA_CONTENTS}/Info.plist mac-jre-${JDK_VER}/Contents
  pushd mac-jre-${JDK_VER}/Contents/MacOS
  ln -s ../Home/lib/libjli.dylib libjli.dylib
  popd
  ${MAC_JAVA_HOME}/bin/jlink --module-path ${MAC_JAVA_HOME}/jmods --add-modules ${JRE_DEPS} --output mac-jre-${JDK_VER}/Contents/Home --strip-debug --compress 2 --no-header-files --no-man-pages

  find mac-jre-${JDK_VER} -name .DS_Store -delete
fi