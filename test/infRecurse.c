#include "stdio.h"
#include "syscall.h"

int counter = 0;

void fun(char *str) {
	printf("counter = %d\n", counter++);
	fun("wojiubuxingbuyichu.........................."
		"adfadfadfadfadfadsfadfadfadfadfadfadfadfadfaadfadfadfaf"
		"adfadfadfafadfafadfadfafdafefafeafdfadfadfadfdfdfadfadf"
		"adfadfadfafadfafadfadfafdafefafeafdfadfadfadfdfdfadfadf"
		"adfadfadfafadfafadfadfafdafefafeafdfadfadfadfdfdfadfadf"
		"adfadfadfafadfafadfadfafdafefafeafdfadfadfadfdfdfadfadf"
		"adfadfadfafadfafadfadfafdafefafeafdfadfadfadfdfdfadfadf"
		"adfadfadfafadfafadfadfafdafefafeafdfadfadfadfdfdfadfadf"
		"adfadfadfafadfafadfadfafdafefafeafdfadfadfadfdfdfadfadf"
		"adfadfadfafadfafadfadfafdafefafeafdfadfadfadfdfdfadfadf"
		"adfadfadfafadfafadfadfafdafefafeafdfadfadfadfdfdfadfadf"
		"adfadfadfafadfafadfadfafdafefafeafdfadfadfadfdfdfadfadf"
		"adfadfadfafadfafadfadfafdafefafeafdfadfadfadfdfdfadfadf"
		"adfadfadfafadfafadfadfafdafefafeafdfadfadfadfdfdfadfadf"
		"adfadfadfafadfafadfadfafdafefafeafdfadfadfadfdfdfadfadf"
		"adfadfadfadfadfadfadfadfafadfadfadfadfadfadfadfadfadfad"
		"adfadfadfeafeadfadfadfadfadfadfafdfadfadfdfdfdfdafadfad"
		"adfadfadfadfadfadfadfadfafadfadfadfadfadfadfadfadfadfad"
		"adfadfadfeafeadfadfadfadfadfadfafdfadfadfdfdfdfdafadfad"
		"adfadfadfadfadfadfadfadfafadfadfadfadfadfadfadfadfadfad"
		"adfadfadfeafeadfadfadfadfadfadfafdfadfadfdfdfdfdafadfad"
		"adfadfadfadfadfadfadfadfafadfadfadfadfadfadfadfadfadfad"
		"adfadfadfeafeadfadfadfadfadfadfafdfadfadfdfdfdfdafadfad"
		"adfadfadfadfadfadfadfadfafadfadfadfadfadfadfadfadfadfad"
		"adfadfadfeafeadfadfadfadfadfadfafdfadfadfdfdfdfdafadfad"
		"adfadfadfadfadfadfadfadfafadfadfadfadfadfadfadfadfadfad"
		"adfadfadfeafeadfadfadfadfadfadfafdfadfadfdfdfdfdafadfad"
		"adfadfadfadfadfadfadfadfafadfadfadfadfadfadfadfadfadfad"
		"adfadfadfeafeadfadfadfadfadfadfafdfadfadfdfdfdfdafadfad"
		"adfadfadfadfadfadfadfadfafadfadfadfadfadfadfadfadfadfad"
		"adfadfadfeafeadfadfadfadfadfadfafdfadfadfdfdfdfdafadfad"
		"adfadfadfadfadfadfadfadfafadfadfadfadfadfadfadfadfadfad"
		"adfadfadfeafeadfadfadfadfadfadfafdfadfadfdfdfdfdafadfad"
		"adfadfadfadfadfadfadfadfafadfadfadfadfadfadfadfadfadfad"
		"adfadfadfeafeadfadfadfadfadfadfafdfadfadfdfdfdfdafadfad"
		"adfadfadfadfadfadfadfadfafadfadfadfadfadfadfadfadfadfad"
		"adfadfadfeafeadfadfadfadfadfadfafdfadfadfdfdfdfdafadfad"
		"adfadfadfadfadfadfadfadfafadfadfadfadfadfadfadfadfadfad"
		"adfadfadfeafeadfadfadfadfadfadfafdfadfadfdfdfdfdafadfad"
		"adfadfadfadfadfadfadfadfafadfadfadfadfadfadfadfadfadfad"
		"adfadfadfeafeadfadfadfadfadfadfafdfadfadfdfdfdfdafadfad"
		"adfaefafdfffefefaffefewfadfgfdfafefadfafaeafdfadfadfadf");
}

int main(int argc, char *argv[]) {
	fun("adfd");
	return 0;
}
