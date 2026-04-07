package com.example.audiocutterdemo2.core.amplituda.exceptions.processing;


import static com.example.audiocutterdemo2.core.amplituda.ErrorCode.PACKET_SUBMITTING_PROC_CODE;

public final class PacketSubmittingException extends AmplitudaProcessingException {
    public PacketSubmittingException() {
        super("Error submitting a packet for decoding!", PACKET_SUBMITTING_PROC_CODE);
    }
}
