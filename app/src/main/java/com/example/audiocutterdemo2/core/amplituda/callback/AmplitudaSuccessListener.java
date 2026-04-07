package com.example.audiocutterdemo2.core.amplituda.callback;


import com.example.audiocutterdemo2.core.amplituda.AmplitudaResult;

/**
 * Callback interface for success processing event
 */
public interface AmplitudaSuccessListener<T> {
    void onSuccess(final AmplitudaResult<T> result);
}
