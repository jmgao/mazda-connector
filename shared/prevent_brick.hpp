#pragma once

#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

inline void prevent_brick(const char *checkfile)
{
    int checkfd = ::open(checkfile, O_RDWR, 0755);

    if (checkfd < 0) {
        printf("failed to open checkfile %s\n", checkfile);
        for (;;);
    }

    char check = '\0';
    ssize_t count = ::pread(checkfd, &check, 1, 0);
    if (count != 1 || (check != '1' && check != '2')) {
        printf("%s is disabled\n", checkfile);
        printf("count = %d, check = %c\n", count, check);
        for (;;);
    }

    if (check != '2') {
        ::pwrite(checkfd, "0", 1, 0);
    }

    ::close(checkfd);
}
