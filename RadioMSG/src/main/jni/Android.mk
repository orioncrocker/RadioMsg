LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE    := RadioMSG_Modem_Interface
LOCAL_CPP_EXTENSION := .cxx .cpp .cc
LOCAL_C_INCLUDES := $(LOCAL_PATH)/fldigi/include $(LOCAL_PATH)/oboe/src $(LOCAL_PATH)/oboe/include $(LOCAL_PATH)/oboe/include/oboe
LOCAL_CPPFLAGS += -std=c++17
LOCAL_SRC_FILES := AndFlmsg_Fldigi_Interface.cpp fldigi/modem.cxx \
			fldigi/psk/psk.cxx fldigi/psk/pskcoeff.cxx fldigi/psk/pskvaricode.cxx \
			fldigi/filters/filters.cxx fldigi/filters/viterbi.cxx fldigi/filters/fftfilt.cxx \
			fldigi/mfsk/interleave.cxx fldigi/mfsk/mfskvaricode.cxx fldigi/mfsk/mfsk.cxx \
			fldigi/misc/misc.cxx fldigi/misc/util.cxx fldigi/misc/configuration.cxx \
			fldigi/mt63/dsp.cxx fldigi/mt63/mt63base.cxx fldigi/mt63/mt63.cxx \
			fldigi/rsid/rsid.cxx \
			fldigi/fft/fft.cxx \
			fldigi/dominoex/dominoex.cxx fldigi/dominoex/dominovar.cxx \
			fldigi/thor/thor.cxx fldigi/thor/thorvaricode.cxx \
			fldigi/olivia/olivia.cxx \
			fldigi/libsamplerate/samplerate.c fldigi/libsamplerate/src_linear.c \
			fldigi/libsamplerate/src_sinc.c fldigi/libsamplerate/src_zoh.c \
			fldigi/kissfft/kiss_fft.c fldigi/kissfft/kiss_fftr.c \
			    oboe/src/aaudio/AAudioLoader.cpp \
                oboe/src/aaudio/AudioStreamAAudio.cpp \
                oboe/src/common/AdpfWrapper.cpp \
                oboe/src/common/AudioSourceCaller.cpp \
                oboe/src/common/AudioStream.cpp \
                oboe/src/common/AudioStreamBuilder.cpp \
                oboe/src/common/DataConversionFlowGraph.cpp \
                oboe/src/common/FilterAudioStream.cpp \
                oboe/src/common/FixedBlockAdapter.cpp \
                oboe/src/common/FixedBlockReader.cpp \
                oboe/src/common/FixedBlockWriter.cpp \
                oboe/src/common/LatencyTuner.cpp \
                oboe/src/common/OboeExtensions.cpp \
                oboe/src/common/SourceFloatCaller.cpp \
                oboe/src/common/SourceI16Caller.cpp \
                oboe/src/common/SourceI24Caller.cpp \
                oboe/src/common/SourceI32Caller.cpp \
                oboe/src/common/Utilities.cpp \
                oboe/src/common/QuirksManager.cpp \
                oboe/src/fifo/FifoBuffer.cpp \
                oboe/src/fifo/FifoController.cpp \
                oboe/src/fifo/FifoControllerBase.cpp \
                oboe/src/fifo/FifoControllerIndirect.cpp \
                oboe/src/flowgraph/FlowGraphNode.cpp \
                oboe/src/flowgraph/ChannelCountConverter.cpp \
                oboe/src/flowgraph/ClipToRange.cpp \
                oboe/src/flowgraph/Limiter.cpp \
                oboe/src/flowgraph/ManyToMultiConverter.cpp \
                oboe/src/flowgraph/MonoBlend.cpp \
                oboe/src/flowgraph/MonoToMultiConverter.cpp \
                oboe/src/flowgraph/MultiToManyConverter.cpp \
                oboe/src/flowgraph/MultiToMonoConverter.cpp \
                oboe/src/flowgraph/RampLinear.cpp \
                oboe/src/flowgraph/SampleRateConverter.cpp \
                oboe/src/flowgraph/SinkFloat.cpp \
                oboe/src/flowgraph/SinkI16.cpp \
                oboe/src/flowgraph/SinkI24.cpp \
                oboe/src/flowgraph/SinkI32.cpp \
                oboe/src/flowgraph/SinkI8_24.cpp \
                oboe/src/flowgraph/SourceFloat.cpp \
                oboe/src/flowgraph/SourceI16.cpp \
                oboe/src/flowgraph/SourceI24.cpp \
                oboe/src/flowgraph/SourceI32.cpp \
                oboe/src/flowgraph/SourceI8_24.cpp \
                oboe/src/flowgraph/resampler/IntegerRatio.cpp \
                oboe/src/flowgraph/resampler/LinearResampler.cpp \
                oboe/src/flowgraph/resampler/MultiChannelResampler.cpp \
                oboe/src/flowgraph/resampler/PolyphaseResampler.cpp \
                oboe/src/flowgraph/resampler/PolyphaseResamplerMono.cpp \
                oboe/src/flowgraph/resampler/PolyphaseResamplerStereo.cpp \
                oboe/src/flowgraph/resampler/SincResampler.cpp \
                oboe/src/flowgraph/resampler/SincResamplerStereo.cpp \
                oboe/src/opensles/AudioInputStreamOpenSLES.cpp \
                oboe/src/opensles/AudioOutputStreamOpenSLES.cpp \
                oboe/src/opensles/AudioStreamBuffered.cpp \
                oboe/src/opensles/AudioStreamOpenSLES.cpp \
                oboe/src/opensles/EngineOpenSLES.cpp \
                oboe/src/opensles/OpenSLESUtilities.cpp \
                oboe/src/opensles/OutputMixerOpenSLES.cpp \
                oboe/src/common/StabilizedCallback.cpp \
                oboe/src/common/Trace.cpp \
                oboe/src/common/Version.cpp
LOCAL_LDLIBS := -llog -landroid -lOpenSLES
include $(BUILD_SHARED_LIBRARY)

