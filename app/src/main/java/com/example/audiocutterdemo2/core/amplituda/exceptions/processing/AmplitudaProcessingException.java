package com.example.audiocutterdemo2.core.amplituda.exceptions.processing;


import com.example.audiocutterdemo2.core.amplituda.exceptions.AmplitudaException;

public class AmplitudaProcessingException extends AmplitudaException {
    public AmplitudaProcessingException(String message, final int code) {
        super(message, code);
    }
}
