#include "stdio.h"
#include "syscall.h"

int main(int argc, char *argv[]) {
	int pid = -1, pid2 = -1;
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
		printf("Join for the second time\n");
		if (join(pid, &status) == -1) {
			printf("Join failure!\n");
		} else {
			printf("PID %d has joined with status = %d\n", pid, status);
		}
	}
//	printf("Parent: Execute another program\n");
//	if ((pid = exec("deadloop.coff", argc, argv)) == -1) {
//		printf("Cannot execute!\n");		
//	}
//	if ((pid2 = exec("deadloop.coff", argc, argv)) == -1) {
//		printf("Cannot execute!\n");		
//	}
//	printf("Parent: Begin to wait two children!\n");
//	if (join(pid, &status) == -1) {
//		printf("Join pid = %d failure!\n", pid);
//	} else {
//		printf("PID %d has joined with status = %d\n", pid, status);
//	}
//	if (join(pid2, &status) == -1) {
//		printf("Join pid = %d failure!\n", pid2);
//	} else {
//		printf("PID %d has joined with status = %d\n", pid, status);
//	}

	return 0;
}
