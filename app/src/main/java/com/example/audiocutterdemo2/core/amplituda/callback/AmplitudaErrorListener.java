package com.example.audiocutterdemo2.core.amplituda.callback;


import com.example.audiocutterdemo2.core.amplituda.exceptions.AmplitudaException;

/**
 * Callback interface for error events
 */
public interface AmplitudaErrorListener {
    void onError(final AmplitudaException exception);
}
