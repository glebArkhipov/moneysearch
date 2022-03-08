docker-compose down --volumes
if [[ $1 != "-no-build" ]] ; then
  ./gradlew clean build -x test && docker build -t moneysearch-backend:latest .
fi
if [[ $? == 0 ]] ; then
  docker-compose -p "" up -d --force-recreate
fi

function rebuild_restart_backend {
  ./gradlew clean build -x test && docker build -t moneysearch-backend:latest .
  docker-compose rm -sf backend
  docker-compose up -d
}
