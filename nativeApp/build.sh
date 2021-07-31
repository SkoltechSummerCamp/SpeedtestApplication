set -e

sudo docker build -t android-ndk:latest .
sudo docker run -d --name android-ndk-container android-ndk
sudo docker cp -a android-ndk-container:/tmp/libs ../app
sudo docker stop android-ndk-container
sudo docker rm android-ndk-container
