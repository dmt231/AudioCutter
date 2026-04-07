package com.example.audiocutterdemo2.core.amplituda.exceptions.processing;


import static com.example.audiocutterdemo2.core.amplituda.ErrorCode.UNSUPPORTED_SAMPLE_FMT_PROC_CODE;

public final class UnsupportedSampleFormatException extends AmplitudaProcessingException {
    public UnsupportedSampleFormatException() {
        super("Sample format is not supported!", UNSUPPORTED_SAMPLE_FMT_PROC_CODE);
    }
}
