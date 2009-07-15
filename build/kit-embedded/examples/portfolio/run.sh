#!/bin/sh

# First one sets the path for client jars and VDB
CLIENT_PATH=java/*:PortfolioModel/

#Second one for the JARs in Teiid embedded
TEIID_PATH=../../client/teiid-${pom.version}-client.jar:../../deploy:../../lib/patches/*:../../lib/*:../../extensions/*

java -cp ${CLIENT_PATH}:${TEIID_PATH} JDBCClient "select * from CustomerAccount"
