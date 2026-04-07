package com.example.audiocutterdemo2.core.amplituda.exceptions.allocation;


import static com.example.audiocutterdemo2.core.amplituda.ErrorCode.CODEC_CONTEXT_ALLOC_CODE;

public final class CodecContextAllocationException extends AmplitudaAllocationException {
    public CodecContextAllocationException() {
        super("Failed to allocate the codec context!", CODEC_CONTEXT_ALLOC_CODE);
    }
}
