#pragma once

#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

inline void prevent_brick(const char *checkfile, const char *logfile)
{
    int checkfd = ::open(checkfile, O_RDWR | O_CREAT, 0755);
    int logfd = ::open(logfile, O_RDWR | O_CREAT | O_TRUNC, 0755);

    if (checkfd < 0) {
        printf("failed to open checkfile\n");
        const char msg[] = "failed to open checkfile";
        ::write(logfd, msg, sizeof msg - 1);
        ::close(logfd);
        for (;;);
    }

    char check = '\0';
    ssize_t count = ::pread(checkfd, &check, 1, 0);
    if (count != 1 || (check != '1' && check != '2')) {
        printf("enable_input_filter is disabled\n");
        printf("count = %d, check = %c\n", count, check);
        const char msg[] = "enable_input_filter is disabled";
        ::write(logfd, msg, sizeof msg - 1);
        ::close(logfd);
        for (;;);
    }

    if (check != '2') {
        check = '0';
        ::pwrite(checkfd, &check, 1, 0);
    }

    ::close(logfd);
    ::close(checkfd);
}
