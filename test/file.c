#include "syscall.h"
#include "stdlib.h"
#include "stdio.h"

int
main()
{
    char writeBuf[50], readBuf[50];

    printf("tesing stdin: input a character\n");
    char c = fgetc(0);
    printf("getting %c from stdin\ntesing stdin: input a line\n", c);
    readline(readBuf, 50);
    printf("read line from stdin:\n %s\n\n", readBuf);

    strcpy(writeBuf, "Testing stdout: write someting to it\n\n");
    write(1, writeBuf, strlen(writeBuf));

    char* name = "a.txt";
    int fd1 = creat(name);
    strcpy(writeBuf, "first write\n");
    printf("Testing create and write:\n %d characters written to a.txt\n", write(fd1, writeBuf, strlen(writeBuf)));
    close(fd1);

    fd1 = open(name);
    int num = read(fd1, readBuf, 2);
    printf("Testing close and open: close and reopen a.txt, read 2 characters:\n");
    write(1, readBuf, num);
    read(fd1, readBuf, 50);
    printf("\nTesting positioning: read the rest of a.txt: %s\n", readBuf);
    strcpy(writeBuf, "second write\n");
    printf("Testing positioning: write to the end of a.txt: \n %d characters written\n\n", write(fd1, writeBuf, strlen(writeBuf)));
    close(fd1);

    int fd2 = open(name);
    read(fd2, readBuf, 50);
    printf("The full content in a.txt is now: %s\n\n", readBuf);
    close(fd2);

    char* tempName = "foo";
    int i;
    for (i=0; i<16; i++) {
        printf("Attempt to open another 16 files: open returns %d\n", creat(tempName));
    }
    for (i=2; i<16; i++) {
        printf("Attempt to close %dth files: close returns %d\n", i, close(i));
    }

    printf("\nTesting that read returns for invalid address: returns %d\n\n", read(100, readBuf, 50));

    printf("Testing that write returns for read-only address: returns %d\n\n", write(0, writeBuf, 50));

    char* fakeName = "s";
    printf("Testing that open returns for invalid file name: returns %d\n\n", open(fakeName));

    unlink(name);
    printf("Testing that unlink removes file: after unlink a.txt, open returns %d\n\n", open(name));

    unlink(name);
    printf("Testing that unlink returns for invalid file name: returns %d\n\n", unlink(fakeName));

    halt();

    /* not reached */
}
