set -x
mvn -Dcom.sun.management.jmxremote.port=3333 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false tomcat7:run -pl :archiva-webapp-js -am -Pdev $@
