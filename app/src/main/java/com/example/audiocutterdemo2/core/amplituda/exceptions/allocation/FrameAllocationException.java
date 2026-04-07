package com.example.audiocutterdemo2.core.amplituda.exceptions.allocation;


import static com.example.audiocutterdemo2.core.amplituda.ErrorCode.FRAME_ALLOC_CODE;

public final class FrameAllocationException extends AmplitudaAllocationException {
    public FrameAllocationException() {
        super("Could not allocate frame!", FRAME_ALLOC_CODE);
    }
}
