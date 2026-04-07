package com.example.audiocutterdemo2.core.amplituda.exceptions.io;


import com.example.audiocutterdemo2.core.amplituda.exceptions.AmplitudaException;

public class AmplitudaIOException extends AmplitudaException {
    public AmplitudaIOException(String message, final int code) {
        super(message, code);
    }
}
