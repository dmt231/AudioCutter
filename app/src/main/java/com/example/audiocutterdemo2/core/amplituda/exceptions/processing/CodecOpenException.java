package com.example.audiocutterdemo2.core.amplituda.exceptions.processing;


import static com.example.audiocutterdemo2.core.amplituda.ErrorCode.CODEC_OPEN_PROC_CODE;

public final class CodecOpenException extends AmplitudaProcessingException {
    public CodecOpenException() {
        super("Failed to open codec!", CODEC_OPEN_PROC_CODE);
    }
}
