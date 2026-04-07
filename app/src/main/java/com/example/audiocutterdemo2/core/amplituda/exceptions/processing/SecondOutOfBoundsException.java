package com.example.audiocutterdemo2.core.amplituda.exceptions.processing;

import static com.example.audiocutterdemo2.core.amplituda.ErrorCode.SECOND_OUT_OF_BOUNDS_PROC_CODE;

import java.util.Locale;


public final class SecondOutOfBoundsException extends AmplitudaProcessingException {

    public SecondOutOfBoundsException(int second, int duration) {
        super(String.format(
                Locale.getDefault(),
                "Cannot extract amplitudes for second %d when input audio duration = %d", second, duration),
                SECOND_OUT_OF_BOUNDS_PROC_CODE
        );
    }

    public SecondOutOfBoundsException() {
        this(0, 0);
    }

}
