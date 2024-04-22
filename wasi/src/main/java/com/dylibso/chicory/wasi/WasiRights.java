package com.dylibso.chicory.wasi;

final class WasiRights {
    private WasiRights() {}

    public static final int FD_DATASYNC = bit(0);
    public static final int FD_READ = bit(1);
    public static final int FD_SEEK = bit(2);
    public static final int FD_FDSTAT_SET_FLAGS = bit(3);
    public static final int FD_SYNC = bit(4);
    public static final int FD_TELL = bit(5);
    public static final int FD_WRITE = bit(6);
    public static final int FD_ADVISE = bit(7);
    public static final int FD_ALLOCATE = bit(8);
    public static final int PATH_CREATE_DIRECTORY = bit(9);
    public static final int PATH_CREATE_FILE = bit(10);
    public static final int PATH_LINK_SOURCE = bit(11);
    public static final int PATH_LINK_TARGET = bit(12);
    public static final int PATH_OPEN = bit(13);
    public static final int FD_READDIR = bit(14);
    public static final int PATH_READLINK = bit(15);
    public static final int PATH_RENAME_SOURCE = bit(16);
    public static final int PATH_RENAME_TARGET = bit(17);
    public static final int PATH_FILESTAT_GET = bit(18);
    public static final int PATH_FILESTAT_SET_SIZE = bit(19);
    public static final int PATH_FILESTAT_SET_TIMES = bit(20);
    public static final int FD_FILESTAT_GET = bit(21);
    public static final int FD_FILESTAT_SET_SIZE = bit(22);
    public static final int FD_FILESTAT_SET_TIMES = bit(23);
    public static final int PATH_SYMLINK = bit(24);
    public static final int PATH_REMOVE_DIRECTORY = bit(25);
    public static final int PATH_UNLINK_FILE = bit(26);
    public static final int POLL_FD_READWRITE = bit(27);
    public static final int SOCK_SHUTDOWN = bit(28);
    public static final int SOCK_ACCEPT = bit(29);

    public static final int FILE_RIGHTS_BASE =
            FD_DATASYNC
                    | FD_READ
                    | FD_SEEK
                    | FD_FDSTAT_SET_FLAGS
                    | FD_SYNC
                    | FD_TELL
                    | FD_WRITE
                    | FD_ADVISE
                    | FD_ALLOCATE
                    | FD_FILESTAT_GET
                    | FD_FILESTAT_SET_SIZE
                    | FD_FILESTAT_SET_TIMES
                    | POLL_FD_READWRITE;

    public static final int DIRECTORY_RIGHTS_BASE =
            FD_DATASYNC
                    | FD_FDSTAT_SET_FLAGS
                    | FD_SYNC
                    | PATH_CREATE_DIRECTORY
                    | PATH_CREATE_FILE
                    | PATH_LINK_SOURCE
                    | PATH_LINK_TARGET
                    | PATH_OPEN
                    | FD_READDIR
                    | PATH_READLINK
                    | PATH_RENAME_SOURCE
                    | PATH_RENAME_TARGET
                    | PATH_FILESTAT_GET
                    | PATH_FILESTAT_SET_SIZE
                    | PATH_FILESTAT_SET_TIMES
                    | FD_FILESTAT_GET
                    | FD_FILESTAT_SET_TIMES
                    | PATH_SYMLINK
                    | PATH_REMOVE_DIRECTORY
                    | PATH_UNLINK_FILE;

    private static int bit(int n) {
        return 1 << n;
    }
}
