//
// Created by YXU21 on 1/12/2023.
//

// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("eol");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("eol")
//      }
//    }

#include <jni.h>
#include <string>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <linux/i2c.h>
#include <linux/i2c-dev.h>
#include <android/log.h>

#define TAG "EOL_JNI_Native"
#define LOGE(...)    __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__);
#define LOGI(...)    __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__);
#define LOGD(...)    __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__);

// MFi Auth Device Path
#define MFI_AUTH_DEVICE_PATH       "/dev/i2c-1"
#define MFI_AUTH_DEVICE_I2C_ADDR   (0x10)

// Function declare
uint64_t UpTicks(void);

uint64_t SecondsToUpTicks(uint64_t x);

#if 0
/* Test JNI */
extern "C" JNIEXPORT jstring JNICALL
Java_vendor_yfvet_eol_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++.";
    return env->NewStringUTF(hello.c_str());
}
#endif

extern "C" JNIEXPORT jbyte JNICALL
Java_vendor_yfvet_eol_EolService_byteFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    ssize_t n;
    size_t readBuffLen;
    uint8_t buf[1] = {0};
    uint8_t registerAddr;

    uint64_t timeOutDuration;
    int tries = 0;
    jbyte mfiChipRet = 0; /* set return value as negative value as default */

    // 1. open mfi auth chip
    jint fd = open(MFI_AUTH_DEVICE_PATH, O_RDWR);

    if (fd < 0) {
        LOGE("== ERROR 1 == Open MFi Auth Chip file(/dev/i2c-1) failed, errno:%s ",
             strerror(errno));
        goto EXIT;
    }

    LOGI("#1 Open MFi Auth Chip file(/dev/i2c-1) succeed.");

    // 2. send i2c write address to MFi Auth
    if ((-1) == ioctl(fd, I2C_SLAVE, MFI_AUTH_DEVICE_I2C_ADDR)) {
        LOGE("Use ioctl function to access MFi device failed. ");
        close(fd);
        goto EXIT;
    }

    LOGI("#2 Use ioctl function to access MFi device on I2C bus succeed. ")

    // 3. send need to read register address to MFi Auth
    registerAddr = 0x40; // set MFi auth self test register address
    timeOutDuration = UpTicks() + SecondsToUpTicks(2);
    for (tries = 1;; ++tries) {
        n = write(fd, &registerAddr, 1);
        if (1 == n) {
            break;
        }
        usleep(500); // according Apple MFi Auth spec to set
        if (UpTicks() > timeOutDuration) {
            LOGE("Write self test register address:%#x failed. retry time:%d ", registerAddr,
                 tries);
            goto EXIT;
        }
    }

    LOGI("#3 Write self test register address:%#x succeed.", registerAddr);

    for (tries = 1;; ++tries) {
        n = read(fd, buf, 1);
        if (1 == n) {
            break;
        }
        usleep(500);
        if (UpTicks() > timeOutDuration) {
            LOGE("MFi Auth self test register status is Not OK. buf[0]:%#x ", buf[0]);
            goto EXIT;
        }
    }
    LOGI("#4 Read from MFi Auth self test register succeed. ");

    if (0xC0 == buf[0]) {
        LOGI("#5 MFi Auth self test register status is OK.")
        mfiChipRet = 1;
    }
    close(fd);

    EXIT:
    return mfiChipRet;
}

uint64_t UpTicks(void) {
    uint64_t nanos;
    struct timespec ts;

    ts.tv_sec = 0;
    ts.tv_nsec = 0;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    nanos = ts.tv_sec;
    nanos *= 1000000000;
    nanos += ts.tv_nsec;

    return (nanos);
}

uint64_t SecondsToUpTicks(uint64_t x) {
    return (x * 1000000000);
}