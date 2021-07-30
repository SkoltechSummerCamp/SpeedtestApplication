sudo docker run -d --name android-ndk-container android-ndk   \
    && mkdir -p bin                                           \
    && sudo docker cp -a android-ndk-container:/tmp/libs bin  \
    && sudo docker stop android-ndk-container                 \
    && sudo docker rm android-ndk-container
