package com.example.audiocutterdemo2.core.amplituda.exceptions.allocation;


import com.example.audiocutterdemo2.core.amplituda.exceptions.AmplitudaException;

public class AmplitudaAllocationException extends AmplitudaException {
    public AmplitudaAllocationException(String message, final int code) {
        super(message, code);
    }
}
