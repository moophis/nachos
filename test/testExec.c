#include "stdio.h"
#include "syscall.h"

int main(int argc, char *argv[]) {
	int pid = -1;
	int status;
	if ((pid = exec("hello.coff", argc, argv)) == -1) {
		printf("Cannot execute!\n");		
	} else {
		printf("Waiting for PID %d\n", pid);
		if (join(pid, &status) == -1) {
			printf("Join failure!\n");
		} else {
			printf("PID %d has joined with status = %d\n", pid, status);
		}
	}
	printf("Execute another program\n");
	if ((pid = exec("deadloop.coff", argc, argv)) == -1) {
		printf("Cannot execute!\n");		
	}
	if ((pid = exec("deadloop.coff", argc, argv)) == -1) {
		printf("Cannot execute!\n");		
	}

	return 0;
}
