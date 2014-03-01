#include "stdio.h"
#include "stdlib.h"

#define BUFSIZE 1024

char buf[BUFSIZE];

int main(int argc, char *argv[]) {
  int fd, amount;

  fd = open("test.txt");
  if (fd==-1) {
    printf("Unable to open test.txt");
    return 1;
  }

  while ((amount = read(fd, buf, BUFSIZE))>0) {
    write(1, buf, amount);
  }

  close(fd);
  return 0;
}
