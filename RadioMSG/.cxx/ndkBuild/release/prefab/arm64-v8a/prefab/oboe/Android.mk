LOCAL_PATH := $(call my-dir)

ifeq ($(TARGET_ARCH_ABI),arm64-v8a)

include $(CLEAR_VARS)
LOCAL_MODULE := oboe
LOCAL_SRC_FILES := /home/jdouyere/.gradle/caches/transforms-3/5425c02f202a0d0ed9325006fa93d4a5/transformed/jetified-oboe-1.9.0/prefab/modules/oboe/libs/android.arm64-v8a/liboboe.so
LOCAL_EXPORT_C_INCLUDES := /home/jdouyere/.gradle/caches/transforms-3/5425c02f202a0d0ed9325006fa93d4a5/transformed/jetified-oboe-1.9.0/prefab/modules/oboe/include
LOCAL_EXPORT_SHARED_LIBRARIES :=
LOCAL_EXPORT_STATIC_LIBRARIES :=
LOCAL_EXPORT_LDLIBS :=
include $(PREBUILT_SHARED_LIBRARY)

endif  # arm64-v8a

