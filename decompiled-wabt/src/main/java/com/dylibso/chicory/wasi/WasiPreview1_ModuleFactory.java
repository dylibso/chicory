package com.dylibso.chicory.wasi;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("com.dylibso.chicory.annotations.processor.HostModuleProcessor")
public final class WasiPreview1_ModuleFactory {

    private WasiPreview1_ModuleFactory() {
    }

    public static HostFunction[] toHostFunctions(WasiPreview1 functions) {
        return toHostFunctions(functions,
                               "wasi_snapshot_preview1");
    }

    public static HostFunction[] toHostFunctions(WasiPreview1 functions, String moduleName) {
        return new HostFunction[] { //
        new HostFunction(moduleName,
                         "adapter_close_badfd",
                         FunctionType.of(List.of(ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.adapterCloseBadfd((int) args[0]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "adapter_open_badfd",
                         FunctionType.of(List.of(ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.adapterOpenBadfd((int) args[0]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "args_get",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.argsGet(instance.memory(),
                                                            (int) args[0],
                                                            (int) args[1]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "args_sizes_get",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.argsSizesGet(instance.memory(),
                                                                 (int) args[0],
                                                                 (int) args[1]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "clock_res_get",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.clockResGet(instance.memory(),
                                                                (int) args[0],
                                                                (int) args[1]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "clock_time_get",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I64,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.clockTimeGet(instance.memory(),
                                                                 (int) args[0],
                                                                 args[1],
                                                                 (int) args[2]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "environ_get",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.environGet(instance.memory(),
                                                               (int) args[0],
                                                               (int) args[1]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "environ_sizes_get",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.environSizesGet(instance.memory(),
                                                                    (int) args[0],
                                                                    (int) args[1]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "fd_advise",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I64,
                                                 ValType.I64,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.fdAdvise((int) args[0],
                                                             args[1],
                                                             args[2],
                                                             (int) args[3]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "fd_allocate",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I64,
                                                 ValType.I64),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.fdAllocate((int) args[0],
                                                               args[1],
                                                               args[2]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "fd_close",
                         FunctionType.of(List.of(ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.fdClose((int) args[0]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "fd_datasync",
                         FunctionType.of(List.of(ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.fdDatasync((int) args[0]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "fd_fdstat_get",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.fdFdstatGet(instance.memory(),
                                                                (int) args[0],
                                                                (int) args[1]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "fd_fdstat_set_flags",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.fdFdstatSetFlags((int) args[0],
                                                                     (int) args[1]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "fd_fdstat_set_rights",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I64,
                                                 ValType.I64),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.fdFdstatSetRights((int) args[0],
                                                                      args[1],
                                                                      args[2]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "fd_filestat_get",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.fdFilestatGet(instance.memory(),
                                                                  (int) args[0],
                                                                  (int) args[1]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "fd_filestat_set_size",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I64),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.fdFilestatSetSize((int) args[0],
                                                                      args[1]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "fd_filestat_set_times",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I64,
                                                 ValType.I64,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.fdFilestatSetTimes((int) args[0],
                                                                       args[1],
                                                                       args[2],
                                                                       (int) args[3]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "fd_pread",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I64,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.fdPread(instance.memory(),
                                                            (int) args[0],
                                                            (int) args[1],
                                                            (int) args[2],
                                                            args[3],
                                                            (int) args[4]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "fd_prestat_dir_name",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.fdPrestatDirName(instance.memory(),
                                                                     (int) args[0],
                                                                     (int) args[1],
                                                                     (int) args[2]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "fd_prestat_get",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.fdPrestatGet(instance.memory(),
                                                                 (int) args[0],
                                                                 (int) args[1]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "fd_pwrite",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I64,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.fdPwrite(instance.memory(),
                                                             (int) args[0],
                                                             (int) args[1],
                                                             (int) args[2],
                                                             args[3],
                                                             (int) args[4]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "fd_read",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.fdRead(instance.memory(),
                                                           (int) args[0],
                                                           (int) args[1],
                                                           (int) args[2],
                                                           (int) args[3]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "fd_readdir",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I64,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.fdReaddir(instance.memory(),
                                                              (int) args[0],
                                                              (int) args[1],
                                                              (int) args[2],
                                                              args[3],
                                                              (int) args[4]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "fd_renumber",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.fdRenumber((int) args[0],
                                                               (int) args[1]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "fd_seek",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I64,
                                                 ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.fdSeek(instance.memory(),
                                                           (int) args[0],
                                                           args[1],
                                                           (int) args[2],
                                                           (int) args[3]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "fd_sync",
                         FunctionType.of(List.of(ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.fdSync((int) args[0]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "fd_tell",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.fdTell(instance.memory(),
                                                           (int) args[0],
                                                           (int) args[1]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "fd_write",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.fdWrite(instance.memory(),
                                                            (int) args[0],
                                                            (int) args[1],
                                                            (int) args[2],
                                                            (int) args[3]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "path_create_directory",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.pathCreateDirectory((int) args[0],
                                                                        instance.memory().readString((int) args[1],
                                                                                                     (int) args[2]));
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "path_filestat_get",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.pathFilestatGet(instance.memory(),
                                                                    (int) args[0],
                                                                    (int) args[1],
                                                                    instance.memory().readString((int) args[2],
                                                                                                 (int) args[3]),
                                                                    (int) args[4]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "path_filestat_set_times",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I64,
                                                 ValType.I64,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.pathFilestatSetTimes((int) args[0],
                                                                         (int) args[1],
                                                                         instance.memory().readString((int) args[2],
                                                                                                      (int) args[3]),
                                                                         args[4],
                                                                         args[5],
                                                                         (int) args[6]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "path_link",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.pathLink((int) args[0],
                                                             (int) args[1],
                                                             instance.memory().readString((int) args[2],
                                                                                          (int) args[3]),
                                                             (int) args[4],
                                                             instance.memory().readString((int) args[5],
                                                                                          (int) args[6]));
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "path_open",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I64,
                                                 ValType.I64,
                                                 ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.pathOpen(instance.memory(),
                                                             (int) args[0],
                                                             (int) args[1],
                                                             instance.memory().readString((int) args[2],
                                                                                          (int) args[3]),
                                                             (int) args[4],
                                                             args[5],
                                                             args[6],
                                                             (int) args[7],
                                                             (int) args[8]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "path_readlink",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.pathReadlink(instance.memory(),
                                                                 (int) args[0],
                                                                 instance.memory().readString((int) args[1],
                                                                                              (int) args[2]),
                                                                 (int) args[3],
                                                                 (int) args[4],
                                                                 (int) args[5]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "path_remove_directory",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.pathRemoveDirectory((int) args[0],
                                                                        instance.memory().readString((int) args[1],
                                                                                                     (int) args[2]));
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "path_rename",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.pathRename((int) args[0],
                                                               instance.memory().readString((int) args[1],
                                                                                            (int) args[2]),
                                                               (int) args[3],
                                                               instance.memory().readString((int) args[4],
                                                                                            (int) args[5]));
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "path_symlink",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.pathSymlink(instance.memory().readString((int) args[0],
                                                                                             (int) args[1]),
                                                                (int) args[2],
                                                                instance.memory().readString((int) args[3],
                                                                                             (int) args[4]));
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "path_unlink_file",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.pathUnlinkFile((int) args[0],
                                                                   instance.memory().readString((int) args[1],
                                                                                                (int) args[2]));
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "poll_oneoff",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.pollOneoff(instance.memory(),
                                                               (int) args[0],
                                                               (int) args[1],
                                                               (int) args[2],
                                                               (int) args[3]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "proc_exit",
                         FunctionType.of(List.of(ValType.I32),
                                         List.of()),
                         (Instance instance, long... args) -> {
                             functions.procExit((int) args[0]);
                             return null;
                         }), //
        new HostFunction(moduleName,
                         "proc_raise",
                         FunctionType.of(List.of(ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.procRaise((int) args[0]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "random_get",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.randomGet(instance.memory(),
                                                              (int) args[0],
                                                              (int) args[1]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "sched_yield",
                         FunctionType.of(List.of(),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.schedYield();
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "sock_accept",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.sockAccept((int) args[0],
                                                               (int) args[1],
                                                               (int) args[2]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "sock_recv",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.sockRecv((int) args[0],
                                                             (int) args[1],
                                                             (int) args[2],
                                                             (int) args[3],
                                                             (int) args[4],
                                                             (int) args[5]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "sock_send",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.sockSend((int) args[0],
                                                             (int) args[1],
                                                             (int) args[2],
                                                             (int) args[3],
                                                             (int) args[4]);
                             return new long[] { (long) result };
                         }), //
        new HostFunction(moduleName,
                         "sock_shutdown",
                         FunctionType.of(List.of(ValType.I32,
                                                 ValType.I32),
                                         List.of(ValType.I32)),
                         (Instance instance, long... args) -> {
                             int result = functions.sockShutdown((int) args[0],
                                                                 (int) args[1]);
                             return new long[] { (long) result };
                         }) };
    }
}
