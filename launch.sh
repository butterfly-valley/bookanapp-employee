mvn clean install -DskipTests
cd ../bookanapp-admin
mvn clean install -DskipTests
cd ../bookanapp-auth
mvn clean install -DskipTests
cd ../bookanapp-appointment
mvn clean install -DskipTests
cd ../bookanapp-appointment
mvn clean install -DskipTests
cd ../bookanapp-employee
mvn clean install -DskipTests
cd ../bookanapp-provider
mvn clean install -DskipTests
cd ../bookanapp-user
mvn clean install -DskipTests
cd ../bookanapp-discovery
mvn clean install -DskipTests
docker-compose build
docker-compose up -d
