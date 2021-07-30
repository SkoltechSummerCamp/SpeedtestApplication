#include <iostream>
#include <stdio.h>
#include <string.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

using namespace std;
int main()
{
    int fd;
    char * myfifo = "myfifo";
    mkfifo(myfifo, 0777);
    fd = open("myfifo", O_WRONLY);
    dup2(fd, STDOUT_FILENO);
    printf("hello world");
    fflush(stdout);
    close(fd);
    return 0;
}
