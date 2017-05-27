LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

#LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/res
src_dirs := java

LOCAL_SRC_FILES := $(call all-java-files-under, $(src_dirs))

LOCAL_STATIC_JAVA_LIBRARIES := \
  android-support-v4 \
   android-support-v13 \
    android-support-v7-recyclerview \
	 android-support-v7-preference \
	  android-support-v7-appcompat \
	   android-support-v14-preference \

#LOCAL_MODULE := smlogger
LOCAL_PACKAGE_NAME := smlogger

LOCAL_CERTIFICATE := platform

LOCAL_PROGUARD_ENABLED := disabled
#LOCAL_MODULE_CLASS := APPS

#LOCAL_DEX_PREOPT := false

#LOCAL_CERTIFICATE := PRESIGNED

include $(BUILD_PACKAGE)
