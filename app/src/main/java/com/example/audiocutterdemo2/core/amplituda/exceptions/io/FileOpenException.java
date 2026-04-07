package com.example.audiocutterdemo2.core.amplituda.exceptions.io;


import static com.example.audiocutterdemo2.core.amplituda.ErrorCode.FILE_OPEN_IO_CODE;

public final class FileOpenException extends AmplitudaIOException {
    public FileOpenException() {
        super("Could not open input file!", FILE_OPEN_IO_CODE);
    }
}
