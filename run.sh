git reset --hard HEAD
git pull

chmod a+x ./gradlew

./gradlew clean

./gradlew bootWar

docker-compose stop

docker-compose build --no-cache

docker-compose up -d